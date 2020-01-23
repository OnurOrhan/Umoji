package com.umoji.umoji.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.umoji.umoji.Chat.ChatActivity;
import com.umoji.umoji.Models.Chat;
import com.umoji.umoji.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {
    private static final String TAG = "ChatListAdapter";
    private Context mContext;

    private List<Chat> mChatList;
    private StorageReference mStorageRef;
    private DatabaseReference mRef;

    public ChatListAdapter(List<Chat> mChatList) {
        this.mChatList = mChatList;
        this.mStorageRef = FirebaseStorage.getInstance().getReference();
        this.mRef = FirebaseDatabase.getInstance().getReference();
    }

    @NonNull
    @Override
    public ChatListAdapter.ChatViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        mContext = viewGroup.getContext();

        View v = LayoutInflater.from(mContext)
                .inflate(R.layout.layout_chat_item, viewGroup, false);

        return new ChatViewHolder(v);
    }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout mRelativeLayout;

        private CircleImageView mProfilePhoto;
        private TextView mUsername;
        private TextView mLastMessage;
        private TextView mLastDate;

        private String user_id;
        private String username;

        ChatViewHolder(View view){
            super(view);

            mRelativeLayout = (RelativeLayout) view.findViewById(R.id.chat_list_item);
            mProfilePhoto = (CircleImageView) view.findViewById(R.id.chat_list_photo);
            mUsername = (TextView) view.findViewById(R.id.chat_list_username);
            mLastMessage = (TextView) view.findViewById(R.id.chat_list_last_message);
            mLastDate = (TextView) view.findViewById(R.id.chat_list_date);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final ChatListAdapter.ChatViewHolder viewHolder, int i) {
        Chat c = mChatList.get(i);

        viewHolder.user_id = c.getUser_id();
        Boolean isSelf = c.getFrom_self();

        // Set the profile image
        try {
            final File localFile = File.createTempFile("images", "jpg");
            mStorageRef.child("profile_photos/" + viewHolder.user_id + ".jpg")
                .getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                        viewHolder.mProfilePhoto.setImageBitmap(myBitmap);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {

                    }
                });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Retrieve the username
        if(c.getUsername() != null) viewHolder.username = c.getUsername();
        else viewHolder.username = "";
        viewHolder.mUsername.setText(viewHolder.username);

        viewHolder.mRelativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, ChatActivity.class);
                i.putExtra("user_id", viewHolder.user_id);
                i.putExtra("username", viewHolder.username);
                mContext.startActivity(i);
            }
        });

        // Set the Last Message text field
        if(c.getLast_message() != null && !TextUtils.isEmpty(c.getLast_message())) {
            String temp = "You: ";
            if(isSelf) viewHolder.mLastMessage.setText(temp.concat(cropMessage(c.getLast_message())));
            else viewHolder.mLastMessage.setText(cropMessage(c.getLast_message()));
        } else {
            viewHolder.mLastMessage.setVisibility(View.GONE);
        }

        // Last message time information...
        // Calculating Delta time
        if(c.getLast_date() != 0) {
            long timestampDifference = getTimestampDifference(c.getLast_date());
            int factor = 1000*3600*24*7;
            String str = "";
            long temp = 0;

            if (timestampDifference > factor){
                temp = (timestampDifference/factor);
                if(temp==1) str += "last week";
                else str += temp + " weeks ago";
            } else {
                factor /= 7;

                if (timestampDifference > factor){
                    temp = (timestampDifference/factor);
                    if(temp==1) str += "yesterday";
                    else str += temp + " days ago";
                } else {
                    factor /= 24;

                    if (timestampDifference > factor){
                        temp = (timestampDifference/factor);
                        if(temp==1) str += "an hour ago";
                        else str += temp + " hours ago";
                    } else {
                        factor /= 60;

                        if (timestampDifference > factor){
                            temp = (timestampDifference/factor);
                            if(temp==1) str += "a minute ago";
                            else str += temp + " minutes ago";
                        } else {
                            str += "just now";
                        }}}} viewHolder.mLastDate.setText(str);
        } else {
            viewHolder.mLastDate.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mChatList.size();
    }

    private String cropMessage(String str){
        int n = 15;
        String cropped = str.substring(0, Math.min(str.length(), n+3));

        if(cropped.length() > n){
            cropped = cropped.concat("...");
        } return cropped;
    }

    private long getTimestampDifference(long milis){
        Log.d(TAG, "getTimestampDifference: getting timestamp difference.");
        return Calendar.getInstance().getTimeInMillis() - milis;
    }
}


