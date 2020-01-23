package com.umoji.umoji.Home;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.umoji.umoji.Models.Chain;
import com.umoji.umoji.Models.Like;
import com.umoji.umoji.Models.Notification;
import com.umoji.umoji.Models.User;
import com.umoji.umoji.Models.Video;
import com.umoji.umoji.Profile.ProfileActivity;
import com.umoji.umoji.R;
import com.umoji.umoji.Share.CustomCameraActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

public class WatchActivity extends AppCompatActivity {
    private static final String TAG = "WatchActivity";
    private Context mContext;

    private StorageReference storageReference;
    private DatabaseReference mReference;

    // Parameters from HomeActivity
    private String chain_id, first_video_id, creator_id;

    // Video frame
    private VideoView mVideoView;
    private ProgressBar mProgressBar;
    private TextView mPercent;
    private ImageView mProfilePicture;
    private TextView mUsername;

    // Control buttons
    private Button previousBtn, nextBtn, closeBtn, replyBtn, deleteBtn;
    private Button likeBtn, dislikeBtn;
    private TextView likeTxt;

    // Video information
    private ArrayList<Video> chainVideos;
    private Video currentVideo;
    private int videoIndex;

    // The current user and if he has any videos in the chain
    private String currentUser;
    private Boolean userHasVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch);
        mContext = WatchActivity.this;

        mReference = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        Intent intent = getIntent();
        chain_id = intent.getStringExtra("chain_id");
        first_video_id = intent.getStringExtra("video_id");

        Query creatorQ = mReference.child("chains").orderByChild("chain_id").equalTo(chain_id);
        creatorQ.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot: dataSnapshot.getChildren()){
                    creator_id = singleSnapshot.getValue(Chain.class).getFirst_user();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        chainVideos = new ArrayList<>();

        currentUser = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userHasVideo = false;
        getVideoList();

        mVideoView = (VideoView) findViewById(R.id.watch_video);
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });

        previousBtn = (Button) findViewById(R.id.watch_previous_btn);
        nextBtn = (Button) findViewById(R.id.watch_next_btn);
        closeBtn = (Button) findViewById(R.id.watch_close_btn);
        deleteBtn = (Button) findViewById(R.id.watch_delete_btn);
        likeBtn = (Button) findViewById(R.id.watch_like_btn);
        dislikeBtn = (Button) findViewById(R.id.watch_dislike_btn);
        likeTxt = (TextView) findViewById(R.id.like_count);
        replyBtn = (Button) findViewById(R.id.watch_reply_btn);
        setPreviousBtn();
        setNextBtn();
        setCloseBtn();
        setLikeBtn();
        setDislikeBtn();
        setReplyBtn();
        setDeleteBtn();

        mProfilePicture = (ImageView) findViewById(R.id.watch_profile_photo);
        mUsername = (TextView) findViewById(R.id.watch_username);

        mProgressBar = (ProgressBar) findViewById(R.id.watch_progressbar);
        mProgressBar.getIndeterminateDrawable().setColorFilter(mContext.getResources().getColor(R.color.progressBarColor), PorterDuff.Mode.SRC_IN);
        mProgressBar.setVisibility(View.VISIBLE);
        mPercent = (TextView) findViewById(R.id.watch_percent);
    }

    private void getVideoList(){ // Get the videos belonging to the current chain
        mReference.child("chain_videos").child(chain_id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()) {
                            Boolean videoFound = false;
                            videoIndex = 0;

                            for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                                try {
                                    Video video = new Video();
                                    Video temp = singleSnapshot.getValue(Video.class);

                                    assert temp != null;
                                    video.setVideo_id(temp.getVideo_id());
                                    video.setChain_id(temp.getChain_id());
                                    video.setUser_id(temp.getUser_id());
                                    video.setDate_created(temp.getDate_created());
                                    video.setLikes(temp.getLikes());
                                    video.setViews(temp.getViews());
                                    video.setVideo_uri(temp.getVideo_uri());
                                    video.setVideo_format(temp.getVideo_format());
                                    video.setIs_main(temp.getIs_main());
                                    video.setTitle(temp.getTitle());

                                    if (!video.getVideo_id().equals(first_video_id)) {
                                        if (!videoFound) videoIndex++;
                                    } else {
                                        videoFound = true;
                                        currentVideo = video;
                                    }

                                    if (!userHasVideo) {
                                        if (video.getUser_id().equals(currentUser)) userHasVideo = true;
                                    }

                                    chainVideos.add(video);

                                } catch (NullPointerException e) {
                                    Log.e(TAG, "onDataChange: NullPointerException: " + e.getMessage());
                                }
                            } setEverything();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void setEverything(){ // When navigating to a new video, setting the screen elements
        setVideo();
        setUsernameAndPP();
        setDelete();
        setLike();
        setLikeTxt();
    }

    private void setVideo(){
        mProgressBar.setVisibility(View.VISIBLE);

        if(currentVideo.getVideo_uri() != null){
            mProgressBar.setVisibility(View.GONE);
            mVideoView.setVideoURI(Uri.parse(currentVideo.getVideo_uri()));
            mVideoView.requestFocus();
            mVideoView.start();
            mPercent.setText("");

        } else {
            try {
                StorageReference vidRef = storageReference.child("videos/"
                        + currentVideo.getUser_id());
                if(currentVideo.getVideo_format().contains("mp4")) vidRef = vidRef.child(currentVideo.getVideo_id() + ".mp4");
                else vidRef = vidRef.child(currentVideo.getVideo_id() + ".3gp");

                vidRef.getDownloadUrl()
                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                mProgressBar.setVisibility(View.GONE);
                                mVideoView.setVideoURI(uri);
                                mVideoView.requestFocus();
                                mVideoView.start();
                                mPercent.setText("");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mProgressBar.setVisibility(View.GONE);
                        mPercent.setText("");
                    }
                });
            } catch (Exception e) {
                mProgressBar.setVisibility(View.GONE);
                mPercent.setText("");
                e.printStackTrace();
            }
        }
    }

    private void setUsernameAndPP() {
        mProfilePicture.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_profile));

        Query query = mReference.child("users").orderByChild("user_id").equalTo(currentVideo.getUser_id());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot :  dataSnapshot.getChildren()){
                    mUsername.setText(Objects.requireNonNull(singleSnapshot.getValue(User.class)).getUsername());

                    try {
                        final File localFile = File.createTempFile("images", "jpg");
                        storageReference.child("profile_photos/" + currentVideo.getUser_id() + ".jpg")
                                .getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                                mProfilePicture.setImageBitmap(myBitmap);
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
                            Intent i = new Intent(mContext, ProfileActivity.class);
                            i.putExtra("user_id", currentVideo.getUser_id());
                            startActivity(i);
                        }
                    };

                    mProfilePicture.setOnClickListener(listener);
                    mUsername.setOnClickListener(listener);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void setLike() { // Whether the user has liked the current video
        Query likeQuery = mReference.child("video_likes").child(currentVideo.getVideo_id()).orderByChild("user_id").equalTo(currentUser);
        likeQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){ // HAS liked the video
                    activateDislikeBtn();
                } else { // HASN'T liked the video
                    activateLikeBtn();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void setLikeTxt() {
        likeTxt.setText(String.valueOf(currentVideo.getLikes()));
    }

    private void setDelete(){
        if(currentVideo.getUser_id().equals(currentUser)) deleteBtn.setVisibility(View.VISIBLE);
        else deleteBtn.setVisibility(View.GONE);
    }

    private void setLikeBtn() { // Like button click listener
        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                likeBtn.setClickable(false);

                String likeKey = mReference.child("video_likes").child(currentVideo.getVideo_id()).push().getKey();
                Like like = new Like(currentVideo.getVideo_id(), currentUser);
                mReference.child("video_likes").child(currentVideo.getVideo_id()).child(likeKey).setValue(like);

                if(!currentUser.equals(currentVideo.getUser_id())) {
                    String notifKey = mReference.child("notifications").child(currentVideo.getUser_id()).push().getKey();
                    Notification notif = new Notification(2, notifKey, currentUser, currentVideo.getUser_id());
                    notif.setKey1(currentVideo.getVideo_id());
                    notif.setKey2(currentVideo.getChain_id());
                    notif.setDate_created(Calendar.getInstance().getTimeInMillis());

                    mReference.child("notifications").child(currentVideo.getUser_id()).child(notifKey).setValue(notif);
                }

                activateDislikeBtn();
                incrementLikes(true);
                likeBtn.setClickable(true);
            }
        });
    }

    private void setDislikeBtn() { // Dislike button click listener
        dislikeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Removing like data
                dislikeBtn.setClickable(false);
                Query likeQ = mReference.child("video_likes").child(currentVideo.getVideo_id()).orderByChild("user_id").equalTo(currentUser);
                likeQ.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                            final String deleteKey = singleSnapshot.getKey();

                            mReference.child("video_likes").child(currentVideo.getVideo_id()).child(deleteKey).removeValue();
                            activateLikeBtn();
                            incrementLikes(false);
                            dislikeBtn.setClickable(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });
    }

    private void incrementLikes(boolean increment) {
        final int add;
        if(increment) add = 1;
        else add = -1;

        Query likeCount = mReference.child("videos").orderByChild("video_id").equalTo(currentVideo.getVideo_id());
        likeCount.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    Video temp = singleSnapshot.getValue(Video.class);
                    int likes = temp.getLikes() + add;
                    String creator = temp.getUser_id();
                    if(likes < 0) likes = 0;

                    mReference.child("videos").child(currentVideo.getVideo_id()).child("likes").setValue(likes);
                    mReference.child("chain_videos").child(chain_id).child(currentVideo.getVideo_id()).child("likes").setValue(likes);

                    mReference.child("chains").child(currentVideo.getChain_id()).child("likes").setValue(likes);
                    mReference.child("user_chains").child(creator).child(currentVideo.getChain_id()).child("likes").setValue(likes);

                    currentVideo.setLikes(likes);
                    setLikeTxt();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void activateLikeBtn(){
        likeBtn.setVisibility(View.VISIBLE);
        likeBtn.setEnabled(true);
        dislikeBtn.setVisibility(View.GONE);
        dislikeBtn.setEnabled(false);
    }

    private void activateDislikeBtn(){
        likeBtn.setVisibility(View.GONE);
        likeBtn.setEnabled(false);
        dislikeBtn.setVisibility(View.VISIBLE);
        dislikeBtn.setEnabled(true);
    }

    private void setPreviousBtn(){
        previousBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(chainVideos.size() > 1){
                    mVideoView.stopPlayback();
                    mVideoView.setVideoURI(null);
                    videoIndex = (videoIndex - 1) % chainVideos.size();
                    currentVideo = chainVideos.get(videoIndex);
                    setEverything();
                }
            }
        });
    }

    private void setNextBtn(){
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(chainVideos.size() > 1){
                    mVideoView.stopPlayback();
                    mVideoView.setVideoURI(null);
                    videoIndex = (videoIndex + 1) % chainVideos.size();
                    currentVideo = chainVideos.get(videoIndex);
                    setEverything();
                }
            }
        });
    }

    private void setReplyBtn() {
        replyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!userHasVideo){
                    Intent i = new Intent(mContext, CustomCameraActivity.class);
                    i.putExtra("task", "reply");
                    i.putExtra("chain_id", chain_id);
                    i.putExtra("creator_id", creator_id);
                    startActivity(i);
                } else {
                    Toast.makeText(mContext,"You already have a video in this chain!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setCloseBtn(){
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void deleteVideo(){
        likeBtn.setEnabled(false);
        dislikeBtn.setEnabled(false);
        previousBtn.setEnabled(false);
        nextBtn.setEnabled(false);
        deleteBtn.setEnabled(false);
        replyBtn.setEnabled(false);

        boolean exit = false;
        if(chainVideos.size() == 1){ // Delete the whole chain
            // Delete chain info from Database
            mReference.child("chains").child(chain_id).child("tags").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(DataSnapshot singleSnapshot :  dataSnapshot.getChildren()){
                        mReference.child("tag_chains").child(singleSnapshot.getValue().toString()).removeValue();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });

            mReference.child("chains").child(chain_id).removeValue();
            exit = true;

        } else if(chainVideos.size() > 1 && creator_id.equals(currentUser)){ // Do not delete the chain
            // Changing the chain creator (switching it to the next user)
            mReference.child("chains").child(chain_id).child("first_user")
                    .setValue(chainVideos.get(videoIndex + 1 % chainVideos.size()).getUser_id());
        } else exit = true;

        // Delete single video info from Database & Storage
        mVideoView.stopPlayback();
        mProgressBar.setVisibility(View.VISIBLE);
        final boolean finalExit = exit;
        storageReference.child("videos")
            .child(currentUser)
            .child(currentVideo.getVideo_id()+".mp4").delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    // Deleting the database entries for the video
                    mReference.child("chain_videos").child(chain_id).child(currentVideo.getVideo_id()).removeValue();
                    mReference.child("videos").child(currentVideo.getVideo_id()).removeValue();
                    mReference.child("video_likes").child(currentVideo.getVideo_id()).removeValue();

                    storageReference.child("thumbnails")
                        .child(currentUser)
                        .child(currentVideo.getVideo_id()+".jpg").delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                mProgressBar.setVisibility(View.GONE);
                                Toast.makeText(mContext, "Video successfully deleted!",
                                        Toast.LENGTH_SHORT).show();

                                if(finalExit){
                                    Intent i = new Intent(mContext, HomeActivity.class);
                                    startActivity(i);
                                    finish();
                                } else {
                                    // Swith to the next video
                                    if(videoIndex + 1 == chainVideos.size()){
                                        chainVideos.remove(videoIndex);
                                        videoIndex = 0;
                                    } else {
                                        chainVideos.remove(videoIndex);
                                    }
                                    currentVideo = chainVideos.get(videoIndex);
                                    likeBtn.setEnabled(true);
                                    dislikeBtn.setEnabled(true);
                                    previousBtn.setEnabled(true);
                                    nextBtn.setEnabled(true);
                                    deleteBtn.setEnabled(true);
                                    replyBtn.setEnabled(true);
                                    setEverything();
                                }
                            }
                        });
            }
        });
    }

    private void setDeleteBtn(){ // creator_id, currentVideo, currentUser
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(currentUser.equals(currentVideo.getUser_id())){ // The user's own video
                    AlertDialog.Builder altdial = new AlertDialog.Builder(mContext);
                    altdial
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteVideo();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                    AlertDialog alert = altdial.create();
                    alert.setTitle("Remove this video?");
                    alert.show();
                }
            }
        });
    }

    private void updateProgress(FileDownloadTask.TaskSnapshot taskSnapshot) {
        long filesize = taskSnapshot.getTotalByteCount();
        long uploadBytes = taskSnapshot.getBytesTransferred();
        long progress = (100 * uploadBytes) / filesize;

        String str = "%" + Long.toString(progress);
        mPercent.setText(str);
    }
}
