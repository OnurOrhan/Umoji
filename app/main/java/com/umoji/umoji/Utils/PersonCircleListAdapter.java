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
import android.widget.RelativeLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.umoji.umoji.Home.WatchPersonStoryActivity;
import com.umoji.umoji.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class PersonCircleListAdapter extends RecyclerView.Adapter<PersonCircleListAdapter.TagpersonViewHolder> {
    private static final String TAG = "PersonCircleListAdapter";

    private ArrayList<String> mTagpersonList;
    private Context mContext;
    private StorageReference storageReference;

    public PersonCircleListAdapter(ArrayList<String> mTagpersonList, Context context){
        this.mTagpersonList = mTagpersonList;
        this.mContext = context;
        this.storageReference = FirebaseStorage.getInstance().getReference();
    }

    @NonNull
    @Override
    public PersonCircleListAdapter.TagpersonViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        mContext = viewGroup.getContext();
        View v = LayoutInflater.from(mContext)
                .inflate(R.layout.layout_storyperson_listitem, viewGroup, false);

        return new TagpersonViewHolder(v);
    }

    class TagpersonViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout relativeLayout;
        private CircleImageView circleImageView;
        private String tagPersonId;

        TagpersonViewHolder(View view){
            super(view);

            relativeLayout = (RelativeLayout) view.findViewById(R.id.tagperson_item_layout);
            circleImageView = (CircleImageView) view.findViewById(R.id.tagperson_image);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final PersonCircleListAdapter.TagpersonViewHolder viewHolder, int i) {
        viewHolder.tagPersonId = mTagpersonList.get(i);

        // Set the profile image
        viewHolder.circleImageView.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_profile));
        try {
            final File localFile = File.createTempFile("images", "jpg");
            storageReference.child("profile_photos/" + viewHolder.tagPersonId + ".jpg")
                    .getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    viewHolder.circleImageView.setImageBitmap(myBitmap);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, WatchPersonStoryActivity.class);
                intent.putStringArrayListExtra("mPeople", mTagpersonList);
                intent.putExtra("personIndex", i);
                mContext.startActivity(intent);
            }
        };

        viewHolder.relativeLayout.setOnClickListener(listener);

    }

    @Override
    public int getItemCount() {
        return mTagpersonList.size();
    }
}


