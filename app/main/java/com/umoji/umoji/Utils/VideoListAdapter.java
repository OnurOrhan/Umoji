package com.umoji.umoji.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.umoji.umoji.Home.WatchActivity;
import com.umoji.umoji.Models.Video;
import com.umoji.umoji.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class VideoListAdapter extends RecyclerView.Adapter<VideoListAdapter.VideoViewHolder> {
    private static final String TAG = "VideoListAdapter";

    private List<Video> mVideoList;
    private Context mContext;
    private DatabaseReference mReference;
    private StorageReference storageReference;

    public VideoListAdapter(List<Video> mVideoList, Context context){
        this.mVideoList = mVideoList;
        this.mContext = context;
        this.mReference = FirebaseDatabase.getInstance().getReference();
        this.storageReference = FirebaseStorage.getInstance().getReference();
    }

    @NonNull
    @Override
    public VideoListAdapter.VideoViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        mContext = viewGroup.getContext();

        View v = LayoutInflater.from(mContext)
                .inflate(R.layout.layout_profile_videoview, viewGroup, false);

        return new VideoViewHolder(v);
    }

    class VideoViewHolder extends RecyclerView.ViewHolder {
        private Video video;

        private RelativeLayout relativeLayout;
        private RelativeLayout textLayout;
        private TextView title;
        private RoundedImageView mThumbnailImage;
        private ProgressBar progressBar;

        VideoViewHolder(View view){
            super(view);

            relativeLayout = (RelativeLayout) view.findViewById(R.id.profile_video_layout);

            textLayout = (RelativeLayout) view.findViewById(R.id.profile_video_title_layout);
            title = (TextView) view.findViewById(R.id.profile_video_title);
            mThumbnailImage = (RoundedImageView) view.findViewById(R.id.profile_video_thumbnail);
            progressBar = (ProgressBar) view.findViewById(R.id.profile_video_progressbar);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final VideoListAdapter.VideoViewHolder viewHolder, int i) {
        viewHolder.video = mVideoList.get(i);

        viewHolder.textLayout.setVisibility(View.INVISIBLE);
        viewHolder.relativeLayout.setVisibility(View.INVISIBLE);

        viewHolder.mThumbnailImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, WatchActivity.class);
                i.putExtra("chain_id", viewHolder.video.getChain_id());
                i.putExtra("video_id", viewHolder.video.getVideo_id());
                mContext.startActivity(i);
            }
        });

        // Set thumbnail image
        viewHolder.progressBar.setVisibility(View.VISIBLE);
        try {
            final File localFile = File.createTempFile("images", "jpg");
            storageReference.child("thumbnails/" + viewHolder.video.getUser_id()
                    + "/"+ viewHolder.video.getVideo_id() + ".jpg")
                    .getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    viewHolder.mThumbnailImage.setImageBitmap(myBitmap);
                    viewHolder.relativeLayout.setVisibility(View.VISIBLE);

                    DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
                    float dpWidth = displayMetrics.widthPixels;
                    viewHolder.mThumbnailImage.getLayoutParams().width = (int) dpWidth *9/20;
                    viewHolder.mThumbnailImage.getLayoutParams().height = (int) dpWidth *12/20;

                    viewHolder.progressBar.setVisibility(View.GONE);

                    // Get video titleTxt
                    mReference.child("chains").orderByChild("chain_id").equalTo(viewHolder.video.getChain_id())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for (DataSnapshot singleSnapshot: dataSnapshot.getChildren()){
                                        viewHolder.textLayout.setVisibility(View.VISIBLE);
                                        //viewHolder.titleTxt.setText(Objects.requireNonNull(singleSnapshot.getValue(Chain.class)).getTitle());
                                        // video titleTxt -> emojis
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    viewHolder.relativeLayout.setVisibility(View.GONE);
                                }
                            });
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    viewHolder.progressBar.setVisibility(View.GONE);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            viewHolder.progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mVideoList.size();
    }
}
