package com.umoji.umoji.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.umoji.umoji.Models.User;
import com.umoji.umoji.Profile.ProfileActivity;
import com.umoji.umoji.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MatchListAdapter extends ArrayAdapter<String> {
    private static final String TAG = "MatchListAdapter";

    private LayoutInflater mInflater;
    private int mLayoutResource;
    private Context mContext;
    private DatabaseReference mReference;
    private StorageReference storageReference;

    public MatchListAdapter(@NonNull Context context, int resource, @NonNull List<String> objects) {
        super(context, resource, objects);
        this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mLayoutResource = resource;
        this.mContext = context;
        this.mReference = FirebaseDatabase.getInstance().getReference();
        this.storageReference = FirebaseStorage.getInstance().getReference();
    }

    static class ViewHolder{
        CircleImageView mProfileImage;
        TextView usernameTxt, nameTxt, descriptionTxt;

        String user_id, username, name, description;
        //ProgressBar progressBar;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        final ViewHolder holder;
        convertView = mInflater.inflate(mLayoutResource, parent, false);
        holder = new ViewHolder();

        holder.mProfileImage = (CircleImageView) convertView.findViewById(R.id.match_profile_photo);
        //holder.progressBar = (ProgressBar) convertView.findViewById(R.id.match_photo_progresbar);
        holder.usernameTxt = (TextView) convertView.findViewById(R.id.match_username);
        holder.nameTxt = (TextView) convertView.findViewById(R.id.match_name);
        holder.descriptionTxt = (TextView) convertView.findViewById(R.id.match_description);

        holder.user_id = getItem(position);

        convertView.setTag(holder);

        Query query = mReference.child("users").orderByChild("user_id").equalTo(holder.user_id);
        final View finalConvertView = convertView;
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot: dataSnapshot.getChildren()) {
                    holder.name = singleSnapshot.getValue(User.class).getName();
                    holder.nameTxt.setText(holder.name);

                    holder.username = singleSnapshot.getValue(User.class).getUsername();
                    holder.usernameTxt.setText(holder.username);

                    View.OnClickListener listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(mContext, ProfileActivity.class);
                            i.putExtra("user_id", holder.user_id);
                            mContext.startActivity(i);
                        }
                    };

                    holder.mProfileImage.setImageBitmap(BitmapFactory.decodeResource(finalConvertView.getResources(), R.drawable.ic_profile));
                    holder.mProfileImage.setOnClickListener(listener);
                    holder.usernameTxt.setOnClickListener(listener);
                    holder.nameTxt.setOnClickListener(listener);

                    holder.description = singleSnapshot.getValue(User.class).getDescription();
                    final ArrayList<String> tags = new ArrayList<>();
                    Query tagsQ = mReference.child("users").child(holder.user_id).child("tags");
                    tagsQ.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot singleSnapshot: dataSnapshot.getChildren()){
                                if(!TextUtils.isEmpty(singleSnapshot.getValue(String.class))){
                                    tags.add(singleSnapshot.getValue(String.class));
                                }
                            }

                            if(tags.size() == 0 && !holder.description.equals("Description")){
                                holder.descriptionTxt.setText(holder.description);
                            } else if(tags.size() > 0) {
                                StringBuilder temp = new StringBuilder();
                                for (int i = 0; i < tags.size(); i++){
                                    temp.append(tags.get(i).substring(0, 1).toUpperCase()).append(tags.get(i).substring(1));
                                    if(i < tags.size()-1) temp.append(", ");
                                } holder.descriptionTxt.setText(temp.toString());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        // Set the profile image
        try {
            //holder.progressBar.setVisibility(View.VISIBLE);
            final File localFile = File.createTempFile("images", "jpg");
            storageReference.child("profile_photos/" + holder.user_id + ".jpg")
                    .getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    holder.mProfileImage.setImageBitmap(myBitmap);
                    //holder.progressBar.setVisibility(View.GONE);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    //holder.progressBar.setVisibility(View.GONE);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return convertView;
    }
}


