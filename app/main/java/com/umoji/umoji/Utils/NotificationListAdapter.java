package com.umoji.umoji.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.umoji.umoji.Home.WatchActivity;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class NotificationListAdapter extends ArrayAdapter<Notification> {
    private static final String TAG = "RequestListAdapter";

    private LayoutInflater mInflater;
    private int mLayoutResource;
    private Context mContext;
    private DatabaseReference mReference;
    private StorageReference storageReference;

    public NotificationListAdapter(@NonNull Context context, int resource, @NonNull List<Notification> objects) {
        super(context, resource, objects);
        this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mLayoutResource = resource;
        this.mContext = context;
        this.mReference = FirebaseDatabase.getInstance().getReference();
        this.storageReference = FirebaseStorage.getInstance().getReference();
    }

    static class ViewHolder{
        RelativeLayout relLayout;
        RoundedImageView mProfileImage;
        //ProgressBar progressBar;

        Notification notif;
        TextView deltaTime, username, description;

        ImageView thumbnail;
        Button acceptFriend, declineFriend;
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        final ViewHolder holder;
        convertView = mInflater.inflate(mLayoutResource, parent, false);
        holder = new ViewHolder();
        holder.notif = getItem(position);

        holder.relLayout = (RelativeLayout) convertView.findViewById(R.id.notification_listitem);
        holder.mProfileImage = (RoundedImageView) convertView.findViewById(R.id.notification_profile_photo);
        //holder.progressBar = (ProgressBar) convertView.findViewById(R.id.notification_photo_progresbar);
        holder.deltaTime = (TextView) convertView.findViewById(R.id.notification_time);
        holder.username = (TextView) convertView.findViewById(R.id.notification_username);
        holder.description = (TextView) convertView.findViewById(R.id.notification_description);

        holder.thumbnail = (ImageView) convertView.findViewById(R.id.notification_thumbnail);
        holder.acceptFriend = (Button) convertView.findViewById(R.id.notification_friend_accept);
        holder.declineFriend = (Button) convertView.findViewById(R.id.notification_friend_decline);
        convertView.setTag(holder);

        if(!holder.notif.getSeen()){
            holder.relLayout.setBackgroundColor(convertView.getResources().getColor(R.color.light_grey));
            holder.username.setTypeface(holder.username.getTypeface(), Typeface.BOLD);
            holder.description.setTypeface(holder.description.getTypeface(), Typeface.BOLD);
            holder.deltaTime.setTypeface(holder.deltaTime.getTypeface(), Typeface.BOLD);
            mReference.child("notifications").child(holder.notif.getUser2()).child(holder.notif.getNot_id()).child("seen").setValue(true);
        }

        Query u1 = mReference.child("users").orderByChild("user_id").equalTo(holder.notif.getUser1());
        u1.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot: dataSnapshot.getChildren()){
                    holder.username.setText(singleSnapshot.getValue(User.class).getUsername());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        setDescriptionTxt(holder.notif.getType(), holder.description);

        // Calculating Delta time
        long timestampDifference = getTimestampDifference(holder.notif.getDate_created());
        int factor = 1000*3600*24;
        String str = "";

        if (timestampDifference > factor){
            str += (timestampDifference/factor) + "d";
        } else if (timestampDifference > factor/24){
            str += (timestampDifference*24/factor) + "h";
        } else if (timestampDifference > factor/24/60){
            str += (timestampDifference*24*60/factor) + "m";
        } else {
            str += "now";
        } holder.deltaTime.setText(str);

        final String currentUser = FirebaseAuth.getInstance().getCurrentUser().getUid();

        if(holder.notif.getType() > 2) { // Friend / Follow notifications
            holder.relLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(mContext, ProfileActivity.class);
                    i.putExtra("user_id", holder.notif.getUser1());
                    mContext.startActivity(i);
                }
            });

            if ( holder.notif.getType() == 3 ){
                holder.acceptFriend.setVisibility(View.VISIBLE);
                holder.declineFriend.setVisibility(View.VISIBLE);

                holder.acceptFriend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mReference.child("friends").child(currentUser).child(holder.notif.getUser1()).setValue(holder.notif.getUser1());
                        mReference.child("friends").child(holder.notif.getUser1()).child(currentUser).setValue(currentUser);
                        mReference.child("requests").child(currentUser).child(holder.notif.getUser1()).removeValue();
                        remove(getItem(position));
                    }
                });

                holder.declineFriend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mReference.child("requests").child(currentUser).child(holder.notif.getUser1()).removeValue();
                        remove(getItem(position));
                    }
                });
            }

        } else { // Like or Reply notification
            holder.relLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(mContext, WatchActivity.class);
                    i.putExtra("video_id", holder.notif.getKey1());
                    i.putExtra("chain_id", holder.notif.getKey2());
                    mContext.startActivity(i);
                }
            });

            // set Video Thumbnail
            holder.thumbnail.setVisibility(View.VISIBLE);
            holder.thumbnail.setBackgroundColor(convertView.getResources().getColor(R.color.light_grey));
            try {
                final File localFile = File.createTempFile("images", "jpg");
                storageReference.child("thumbnails/" + currentUser + "/" + holder.notif.getKey1() + ".jpg")
                        .getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                        holder.thumbnail.setImageBitmap(myBitmap);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        holder.mProfileImage.setImageBitmap(BitmapFactory.decodeResource(convertView.getResources(), R.drawable.ic_profile));
        //holder.progressBar.setVisibility(View.VISIBLE);
        try {
            final File localFile = File.createTempFile("images", "jpg");
            storageReference.child("profile_photos/" + holder.notif.getUser1() + ".jpg")
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
            //holder.progressBar.setVisibility(View.GONE);
            e.printStackTrace();
        }

        return convertView;
    }

    private void setDescriptionTxt(int type, TextView descr){
        if (type == 1){
            descr.setText("replied to your challenge.");
        } else if (type == 2){
            descr.setText("liked your video.");
        } else if (type == 3){
            descr.setText("sent you a friend request.");
        } else if (type == 4){
            descr.setText("has accepted your friend request.");
        } else if (type == 5){
            descr.setText("started following you.");
        }
    }

    private long getTimestampDifference(long milis){
        Log.d(TAG, "getTimestampDifference: getting timestamp difference.");
        return Calendar.getInstance().getTimeInMillis() - milis;
    }

}


