package com.umoji.umoji.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.umoji.umoji.Models.Notification;
import com.umoji.umoji.Models.User;
import com.umoji.umoji.Profile.ProfileActivity;
import com.umoji.umoji.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendViewHolder> {
    private static final String TAG = "FriendListAdapter";

    private List<User> mUserList;
    private Context mContext;

    private FirebaseAuth mAuth;
    private String currentUser;
    private DatabaseReference mRef;
    private StorageReference mStorageRef;

    public FriendListAdapter(List<User> mUserList, Context context){
        this.mUserList = mUserList;
        this.mContext = context;
        this.mAuth = FirebaseAuth.getInstance();
        this.currentUser = Objects.requireNonNull(this.mAuth.getCurrentUser()).getUid();
        this.mRef = FirebaseDatabase.getInstance().getReference();
        this.mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    @NonNull
    @Override
    public FriendListAdapter.FriendViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        mContext = viewGroup.getContext();

        View v = LayoutInflater.from(mContext)
                .inflate(R.layout.layout_friend_listitem, viewGroup, false);

        return new FriendViewHolder(v);
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout relativeLayout;
        private CircleImageView mProfileImage;
        private TextView username, name;

        private ImageView followBtn, unfollowBtn;

        private User friend;
        private String user_id;
        //private ProgressBar progressBar;

        FriendViewHolder(View view){
            super(view);

            relativeLayout = (RelativeLayout) view.findViewById(R.id.friend_item_layout);

            mProfileImage = (CircleImageView) view.findViewById(R.id.friend_profile_photo);
            username = (TextView) view.findViewById(R.id.friend_username);
            name = (TextView) view.findViewById(R.id.friend_name);

            followBtn = (ImageView) view.findViewById(R.id.item_follow_button);
            unfollowBtn = (ImageView) view.findViewById(R.id.item_unfollow_button);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final FriendListAdapter.FriendViewHolder viewHolder, int i) {
        viewHolder.friend = mUserList.get(i);
        viewHolder.user_id = viewHolder.friend.getUser_id();
        viewHolder.relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, ProfileActivity.class);
                i.putExtra("user_id", viewHolder.user_id);
                mContext.startActivity(i);
            }
        });

        viewHolder.name.setText(viewHolder.friend.getName());
        viewHolder.username.setText(viewHolder.friend.getUsername());

        if(currentUser.equals(viewHolder.user_id)){
            viewHolder.followBtn.setVisibility(View.GONE);
            viewHolder.unfollowBtn.setVisibility(View.GONE);
        } else {
            mRef.child("follows").child(currentUser).child(viewHolder.user_id)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (!dataSnapshot.exists()){
                                viewHolder.followBtn.setVisibility(View.VISIBLE);
                                viewHolder.unfollowBtn.setVisibility(View.GONE);
                            } else {
                                viewHolder.followBtn.setVisibility(View.GONE);
                                viewHolder.unfollowBtn.setVisibility(View.VISIBLE);
                            }

                            setupFollowBtn(viewHolder);
                            setupUnfollowBtn(viewHolder);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

        }

        // Show friend tags (removed feature)
        //
        /* final ArrayList<String> tags = new ArrayList<>();
        Query tagsQ = mRef.child("users").child(viewHolder.user_id).child("tags");
        tagsQ.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot: dataSnapshot.getChildren()){
                    if(!TextUtils.isEmpty(singleSnapshot.getValue(String.class))){
                        tags.add(singleSnapshot.getValue(String.class));
                    }
                }

                if(tags.size() == 0 && !viewHolder.friend.getDescription().equals("Description")){
                    viewHolder.description.setText(viewHolder.friend.getDescription());
                } else if(tags.size() > 0) {
                    StringBuilder temp = new StringBuilder();
                    for (int i = 0; i < tags.size(); i++){
                        temp.append(tags.get(i).substring(0, 1).toUpperCase()).append(tags.get(i).substring(1));
                        if(i < tags.size()-1) temp.append(", ");
                    } viewHolder.description.setText(temp.toString());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        }); */

        viewHolder.mProfileImage.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_profile));

        // Set the profile image
        try {
            //viewHolder.progressBar.setVisibility(View.VISIBLE);
            final File localFile = File.createTempFile("images", "jpg");
            mStorageRef.child("profile_photos/" + viewHolder.user_id + ".jpg")
                    .getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    viewHolder.mProfileImage.setImageBitmap(myBitmap);
                    //viewHolder.progressBar.setVisibility(View.GONE);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    //viewHolder.progressBar.setVisibility(View.GONE);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupFollowBtn(final FriendListAdapter.FriendViewHolder viewHolder) {
        viewHolder.followBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewHolder.followBtn.setEnabled(false);
                mRef.child("follows").child(currentUser).child(viewHolder.user_id).setValue(viewHolder.user_id);
                mRef.child("followed").child(viewHolder.user_id).child(currentUser).setValue(currentUser);

                String notifKey = mRef.child("notifications").child(viewHolder.user_id).push().getKey();
                Notification notif = new Notification(5, notifKey, currentUser, viewHolder.user_id);
                notif.setKey1("none");
                notif.setKey2("none");
                notif.setDate_created(Calendar.getInstance().getTimeInMillis());

                mRef.child("notifications").child(viewHolder.user_id).child(Objects.requireNonNull(notifKey)).setValue(notif);

                viewHolder.followBtn.setVisibility(View.GONE);
                viewHolder.unfollowBtn.setVisibility(View.VISIBLE);
                viewHolder.unfollowBtn.setEnabled(true);
            }
        });
    }

    private void setupUnfollowBtn(final FriendListAdapter.FriendViewHolder viewHolder) {
        viewHolder.unfollowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewHolder.unfollowBtn.setEnabled(false);
                mRef.child("follows").child(currentUser).child(viewHolder.user_id).removeValue();
                mRef.child("followed").child(viewHolder.user_id).child(currentUser).removeValue();

                viewHolder.unfollowBtn.setVisibility(View.GONE);
                viewHolder.followBtn.setVisibility(View.VISIBLE);
                viewHolder.followBtn.setEnabled(true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mUserList.size();
    }
}


