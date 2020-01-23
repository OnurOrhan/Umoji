package com.umoji.umoji.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

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
import com.umoji.umoji.Models.Chain;
import com.umoji.umoji.Models.Like;
import com.umoji.umoji.Models.Notification;
import com.umoji.umoji.Models.User;
import com.umoji.umoji.Models.Video;
import com.umoji.umoji.Profile.ProfileActivity;
import com.umoji.umoji.R;
import com.umoji.umoji.Share.CustomCameraActivity;
import com.vanniktech.emoji.EmojiTextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainfeedListAdapter extends RecyclerView.Adapter<MainfeedListAdapter.MainfeedViewHolder> {
    private static final String TAG = "MainfeedListAdapter";

    private ArrayList<Chain> mMainfeedList;
    private ArrayList<ArrayList<String>> mTags;
    private ArrayList<Boolean> mWatchedList;

    private Context mContext;
    private FirebaseAuth mAuth;
    private String currentUser;
    private DatabaseReference mReference;
    private StorageReference storageReference;

    public MainfeedListAdapter(ArrayList<Chain> objects, ArrayList<ArrayList<String>> tags, ArrayList<Boolean> booleans, Context mContext) {
        this.mMainfeedList = objects;
        this.mTags = tags;
        this.mWatchedList = booleans;
        this.mContext = mContext;
        this.mReference = FirebaseDatabase.getInstance().getReference();
        this.storageReference = FirebaseStorage.getInstance().getReference();

        this.mAuth = FirebaseAuth.getInstance();
        this.currentUser = this.mAuth.getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public MainfeedListAdapter.MainfeedViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.layout_mainfeed_listitem, viewGroup, false);

        return new MainfeedViewHolder(v);
    }

    public class MainfeedViewHolder extends RecyclerView.ViewHolder {
        CircleImageView mProfileImage;
        TextView username, timeDelta, titleTxt, description, viewsTxt;
        EmojiTextView tagsTxt;
        RelativeLayout titleLayout;

        RelativeLayout mVideoFrame;
        Button mPlayBtn;
        VideoView mVideoView;
        ProgressBar progressBar;

        Chain chain;
        String user_id;

        LinearLayoutManager videoManager;
        ArrayList<Video> mVideos;
        ArrayList<String> chainTags;
        VideoListAdapter videoListAdapter;
        RecyclerView videoRecyclerView;

        Boolean repliesOpen, repliesLoaded;
        Boolean watched;

        ImageView likeBtn, dislikeBtn, repliesBtn;
        TextView likeTxt, repliesTxt;

        ImageView replyBtn;

        MainfeedViewHolder (View view){
            super(view);

            mProfileImage = (CircleImageView) view.findViewById(R.id.post_profile_photo);
            username = (TextView) view.findViewById(R.id.post_username);
            viewsTxt = (TextView) view.findViewById(R.id.post_views);

            mVideoView = (VideoView) view.findViewById(R.id.post_video);
            mVideoFrame = (RelativeLayout) view.findViewById(R.id.mainfeed_post_frame);
            mPlayBtn = (Button) view.findViewById(R.id.mainfeed_play_button);

            videoRecyclerView = (RecyclerView) view.findViewById(R.id.mainfeed_reply_list);
            repliesBtn = (ImageView) view.findViewById(R.id.mainfeed_replies_btn);
            repliesTxt = (TextView) view.findViewById(R.id.mainfeed_replies_count);

            likeBtn = (ImageView) view.findViewById(R.id.mainfeed_like_btn);
            dislikeBtn = (ImageView) view.findViewById(R.id.mainfeed_dislike_btn);
            likeTxt = (TextView) view.findViewById(R.id.mainfeed_like_count);

            replyBtn = (ImageView) view.findViewById(R.id.mainfeed_reply_btn);

            DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
            float dpWidth = displayMetrics.widthPixels;
            mVideoView.getLayoutParams().width = (int) dpWidth;
            mVideoView.getLayoutParams().height = (int) dpWidth*4/3;

            progressBar = (ProgressBar) view.findViewById(R.id.thumbnailProgressbar);

            titleLayout = (RelativeLayout) view.findViewById(R.id.mainfeed_bottom_title_layout);
            titleTxt = (TextView) view.findViewById(R.id.mainfeed_bottom_title_txt);
            tagsTxt = (EmojiTextView) view.findViewById(R.id.mainfeed_tags_txt);

            // holder.description = (TextView) convertView.findViewById(R.id.post_description);
            // holder.timeDelta = (TextView) convertView.findViewById(R.id.post_time);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final MainfeedListAdapter.MainfeedViewHolder viewHolder, int i) {
        viewHolder.chain = mMainfeedList.get(i);
        viewHolder.chainTags = mTags.get(i);
        viewHolder.watched = mWatchedList.get(i);
        viewHolder.user_id = viewHolder.chain.getFirst_user();

        viewHolder.progressBar.setVisibility(View.VISIBLE);

        viewHolder.likeBtn.setOnClickListener(null);
        viewHolder.dislikeBtn.setOnClickListener(null);

        setLike(viewHolder);
        setLikeTxt(viewHolder);
        setLikeBtn(viewHolder);
        setDislikeBtn(viewHolder);

        setRepliesTxt(viewHolder);
        setTitle(viewHolder);
        setTags(viewHolder, i);

        setReplyBtn(viewHolder);

        viewHolder.mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
                // mp.setVolume(0,0);
            }
        });

        viewHolder.mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return true;
            }
        });

        setVideo(viewHolder);

        viewHolder.videoManager = new LinearLayoutManager(mContext);
        viewHolder.videoManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        viewHolder.videoRecyclerView.setHasFixedSize(true);
        viewHolder.videoRecyclerView.setLayoutManager(viewHolder.videoManager);
        viewHolder.mVideos = new ArrayList<>();

        viewHolder.repliesOpen = false;
        viewHolder.repliesLoaded = false;

        viewHolder.repliesBtn.setOnClickListener(null);
        viewHolder.repliesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(viewHolder.repliesOpen) {
                    viewHolder.videoRecyclerView.setVisibility(View.GONE);
                    viewHolder.repliesOpen = false;

                } else if(viewHolder.repliesLoaded) {
                    viewHolder.videoRecyclerView.setVisibility(View.VISIBLE);
                    viewHolder.repliesOpen = true;

                } else {
                    mReference.child("chain_videos").child(viewHolder.chain.getChain_id())
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    for(DataSnapshot singleSnapshot: dataSnapshot.getChildren()){
                                        Video v = singleSnapshot.getValue(Video.class);

                                        assert v != null;
                                        if(!v.getVideo_id().equals(viewHolder.chain.getFirst_video())) {
                                            Video temp = new Video();
                                            temp.setChain_id(v.getChain_id());
                                            temp.setVideo_format(v.getVideo_format());
                                            temp.setDate_created(v.getDate_created());
                                            temp.setLikes(v.getLikes());
                                            temp.setViews(v.getViews());
                                            temp.setUser_id(v.getUser_id());
                                            temp.setVideo_id(v.getVideo_id());
                                            temp.setVideo_uri(v.getVideo_uri());
                                            temp.setIs_main(v.getIs_main());
                                            temp.setTitle(v.getTitle());
                                            viewHolder.mVideos.add(temp);
                                        }
                                    }

                                    if(viewHolder.mVideos.size() > 0) {
                                        viewHolder.videoListAdapter = new VideoListAdapter(viewHolder.mVideos, mContext);
                                        viewHolder.videoRecyclerView.setAdapter(viewHolder.videoListAdapter);
                                        viewHolder.repliesLoaded = true;
                                        viewHolder.repliesOpen = true;
                                        viewHolder.videoRecyclerView.setVisibility(View.VISIBLE);
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                }
            }
        });

        // Set the profile image
        viewHolder.mProfileImage.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_profile));
        try {
            final File localFile = File.createTempFile("images", "jpg");
            storageReference.child("profile_photos/" + viewHolder.user_id + ".jpg")
                    .getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    viewHolder.mProfileImage.setImageBitmap(myBitmap);
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

        //get the user object
        mReference.child("users").orderByChild("user_id").equalTo(viewHolder.user_id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    Log.d(TAG, "onDataChange: found user: " +
                            singleSnapshot.getValue(User.class). getUsername());

                    viewHolder.username.setText(singleSnapshot.getValue(User.class).getUsername());
                    setViewsTxt(viewHolder);

                    View.OnClickListener listener = new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent i = new Intent(mContext, ProfileActivity.class);
                            i.putExtra("user_id", viewHolder.user_id);
                            mContext.startActivity(i);
                        }
                    };

                    viewHolder.mProfileImage.setOnClickListener(listener);
                    viewHolder.username.setOnClickListener(listener);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        viewHolder.progressBar.setVisibility(View.GONE);
        viewHolder.progressBar.getIndeterminateDrawable().setColorFilter(mContext.getResources().getColor(R.color.progressBarColor), PorterDuff.Mode.SRC_IN);

        // Comments link
        //viewHolder.description.setText(viewHolder.chain.getDescription());
        //viewHolder.titleTxt.setText( /* emoji titleTxt */);

        /*// Calculating Delta time
        long timestampDifference = getTimestampDifference(viewHolder.chain.getDate_created());
        int factor = 1000*3600*24*7;
        String str = "";

        long temp;
        if (timestampDifference > factor){
            temp = (timestampDifference/factor);
            if (temp == 1) str += temp + " WEEK AGO";
            else str += temp + " WEEKS AGO";
        } else if (timestampDifference > factor/7){
            temp = (timestampDifference*7/factor);
            if (temp == 1) str += temp + " DAY AGO";
            else str += temp + " DAYS AGO";
        } else if (timestampDifference > factor/7/24){
            temp = (timestampDifference*7*24/factor);
            if (temp == 1) str += temp + " HOUR AGO";
            else str += temp + " HOURS AGO";
        } else if (timestampDifference > factor/7/24/60){
            temp = (timestampDifference*7*24*60/factor);
            if (temp == 1) str += temp + " MINUTE AGO";
            else str += temp + " MINUTES AGO";
        } else {
            str += "JUST NOW";
        } viewHolder.timeDelta.setText(str);*/
    }

    private void setReplyBtn(MainfeedViewHolder viewHolder) {
        viewHolder.replyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mReference.child("chain_videos").child(viewHolder.chain.getChain_id()).orderByChild("user_id").equalTo(currentUser)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.getChildrenCount() > 0){
                                    Toast.makeText(mContext,"You already have a video in this chain!", Toast.LENGTH_SHORT).show();

                                } else {
                                    Intent i = new Intent(mContext, CustomCameraActivity.class);
                                    i.putExtra("task", "reply");
                                    i.putExtra("chain_id", viewHolder.chain.getChain_id());
                                    i.putExtra("creator_id", viewHolder.chain.getFirst_user());
                                    mContext.startActivity(i);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Toast.makeText(mContext,"Database error!", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    private void setRepliesTxt(MainfeedViewHolder viewHolder) {
        viewHolder.repliesTxt.setText(String.valueOf(viewHolder.chain.getResponses()));
    }

    private void setTags(MainfeedViewHolder viewHolder, int i) {
        ArrayList<String> temp = mTags.get(i);

        if(temp.size() > 0){
            StringBuilder tagStr = new StringBuilder();

            for(String str: temp){
                tagStr.append(str);
            }

            viewHolder.tagsTxt.setVisibility(View.VISIBLE);
            viewHolder.tagsTxt.setText(tagStr);
        }
    }

    private void setTitle(MainfeedViewHolder viewHolder) {
        if(!TextUtils.isEmpty(viewHolder.chain.getTitle())){
            //viewHolder.titleLayout.setVisibility(View.VISIBLE);
            viewHolder.titleTxt.setVisibility(View.VISIBLE);
            viewHolder.titleTxt.setText(viewHolder.chain.getTitle());
        }
    }

    private void setThumbnailBackground(MainfeedListAdapter.MainfeedViewHolder viewHolder) {
        // Set thumbnail image
        try {
            final File localFile = File.createTempFile("images", "jpg");
            storageReference.child("thumbnails/" + viewHolder.chain.getFirst_user()
                    + "/"+ viewHolder.chain.getFirst_video() + ".jpg")
                    .getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    Drawable myDrawable = new BitmapDrawable(mContext.getResources(), myBitmap);
                    viewHolder.mVideoView.setBackground(myDrawable);
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

    public void reloadVideo(MainfeedViewHolder viewHolder){
        setVideo(viewHolder);
    }

    private void setVideo(MainfeedViewHolder viewHolder) {
        if(viewHolder.chain.getVideo_uri() != null){
            viewHolder.progressBar.setVisibility(View.GONE);
            viewHolder.mVideoView.setVideoURI(Uri.parse(viewHolder.chain.getVideo_uri()));
            viewHolder.mVideoView.requestFocus();
            viewHolder.mVideoView.start();

            /* MediaController mediaController = new MediaController(mContext);
            viewHolder.mVideoView.setMediaController(mediaController);
            mediaController.setAnchorView(viewHolder.mVideoView); */

            if(!viewHolder.watched) {
                viewHolder.watched = Boolean.TRUE;
                incrementViews(viewHolder);
            }

        } else {
            try {
                StorageReference vidRef = storageReference.child("videos/"
                        + viewHolder.user_id);
                if (viewHolder.chain.getVideo_format().contains("mp4"))
                    vidRef = vidRef.child(viewHolder.chain.getFirst_video() + ".mp4");
                else vidRef = vidRef.child(viewHolder.chain.getFirst_video() + ".3gp");

                vidRef.getDownloadUrl()
                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                viewHolder.progressBar.setVisibility(View.GONE);
                                viewHolder.mVideoView.setVideoURI(uri);
                                viewHolder.mVideoView.requestFocus();
                                viewHolder.mVideoView.start();

                                /* MediaController mediaController = new MediaController(mContext);
                                viewHolder.mVideoView.setMediaController(mediaController);
                                mediaController.setAnchorView(viewHolder.mVideoView); */

                                if (!viewHolder.watched) {
                                    viewHolder.watched = Boolean.TRUE;
                                    incrementViews(viewHolder);
                                }
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        viewHolder.progressBar.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                viewHolder.progressBar.setVisibility(View.GONE);
                e.printStackTrace();
            }
        }
    }

    private void setLike(@NonNull final MainfeedListAdapter.MainfeedViewHolder viewHolder) { // Whether the user has liked the current video
        mReference.child("video_likes").child(viewHolder.chain.getFirst_video()).orderByChild("user_id").equalTo(currentUser)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){ // HAS liked the video
                    activateDislikeBtn(viewHolder);
                } else { // HASN'T liked the video
                    activateLikeBtn(viewHolder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                activateLikeBtn(viewHolder);
            }
        });
    }

    private void setLikeTxt(@NonNull final MainfeedListAdapter.MainfeedViewHolder viewHolder) {
        viewHolder.likeTxt.setText(String.valueOf(viewHolder.chain.getLikes()));
    }

    private void setViewsTxt(@NonNull final MainfeedListAdapter.MainfeedViewHolder viewHolder) {
        int v = viewHolder.chain.getViews();

        String txt;
        if(v == 1) txt = "1 view";
        else txt = v + " views";
        viewHolder.viewsTxt.setText(txt);
    }

    private void setLikeBtn(@NonNull final MainfeedListAdapter.MainfeedViewHolder viewHolder) { // Like button click listener
        viewHolder.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewHolder.likeBtn.setEnabled(false);

                String likeKey = mReference.child("video_likes").child(viewHolder.chain.getFirst_video()).push().getKey();
                Like like = new Like(viewHolder.chain.getFirst_video(), viewHolder.user_id);
                mReference.child("video_likes").child(viewHolder.chain.getFirst_video()).child(likeKey).setValue(like);

                if(!currentUser.equals(viewHolder.chain.getFirst_user())) {
                    String notifKey = mReference.child("notifications").child(viewHolder.chain.getFirst_user()).push().getKey();
                    Notification notif = new Notification(2, notifKey, currentUser, viewHolder.chain.getFirst_user());
                    notif.setKey1(viewHolder.chain.getFirst_video());
                    notif.setKey2(viewHolder.chain.getChain_id());
                    notif.setDate_created(Calendar.getInstance().getTimeInMillis());

                    mReference.child("notifications").child(viewHolder.chain.getFirst_user()).child(notifKey).setValue(notif);
                }

                activateDislikeBtn(viewHolder);
                incrementLikes(true, viewHolder);
            }
        });
    }

    private void setDislikeBtn(@NonNull final MainfeedListAdapter.MainfeedViewHolder viewHolder) { // Dislike button click listener
        viewHolder.dislikeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Removing like data
                viewHolder.dislikeBtn.setEnabled(false);

                Query likeQ = mReference.child("video_likes").child(viewHolder.chain.getFirst_video()).orderByChild("user_id").equalTo(currentUser);
                likeQ.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                            final String deleteKey = singleSnapshot.getKey();

                            mReference.child("video_likes").child(viewHolder.chain.getFirst_video()).child(deleteKey).removeValue();
                            activateLikeBtn(viewHolder);
                            incrementLikes(false, viewHolder);
                            viewHolder.dislikeBtn.setClickable(true);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }
        });
    }

    private void activateLikeBtn(@NonNull final MainfeedListAdapter.MainfeedViewHolder viewHolder){
        viewHolder.likeBtn.setVisibility(View.VISIBLE);
        viewHolder.likeBtn.setEnabled(true);
        viewHolder.dislikeBtn.setVisibility(View.GONE);
        viewHolder.dislikeBtn.setEnabled(false);
    }

    private void activateDislikeBtn(@NonNull final MainfeedListAdapter.MainfeedViewHolder viewHolder){
        viewHolder.likeBtn.setVisibility(View.GONE);
        viewHolder.likeBtn.setEnabled(false);
        viewHolder.dislikeBtn.setVisibility(View.VISIBLE);
        viewHolder.dislikeBtn.setEnabled(true);
    }

    private void incrementLikes(boolean increment, @NonNull final MainfeedListAdapter.MainfeedViewHolder viewHolder) {
        final int add;
        if(increment) add = 1;
        else add = -1;

        mReference.child("videos").orderByChild("video_id").equalTo(viewHolder.chain.getFirst_video())
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    int likes = singleSnapshot.getValue(Video.class).getLikes() + add;
                    if(likes < 0) likes = 0;

                    mReference.child("videos").child(viewHolder.chain.getFirst_video()).child("likes").setValue(likes);
                    mReference.child("chain_videos").child(viewHolder.chain.getChain_id()).child(viewHolder.chain.getFirst_video()).child("likes").setValue(likes);

                    mReference.child("chains").child(viewHolder.chain.getChain_id()).child("likes").setValue(likes);
                    mReference.child("user_chains").child(viewHolder.chain.getFirst_user()).child(viewHolder.chain.getChain_id()).child("likes").setValue(likes);

                    for (int i = 0; i < viewHolder.chainTags.size(); i++) {
                        mReference.child("tag_chains").child(viewHolder.chainTags.get(i)).child(viewHolder.chain.getChain_id())
                                .child("likes").setValue(likes);
                    }

                    viewHolder.chain.setLikes(likes);
                    setLikeTxt(viewHolder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void incrementViews(@NonNull final MainfeedListAdapter.MainfeedViewHolder viewHolder) {
        mReference.child("chains").orderByChild("chain_id").equalTo(viewHolder.chain.getChain_id())
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    int views = Objects.requireNonNull(singleSnapshot.getValue(Chain.class)).getViews() + 1;
                    if(views < 0) views = 0;

                    mReference.child("videos").child(viewHolder.chain.getFirst_video()).child("views").setValue(views);
                    mReference.child("chain_videos").child(viewHolder.chain.getChain_id()).child(viewHolder.chain.getFirst_video()).child("views").setValue(views);

                    mReference.child("chains").child(viewHolder.chain.getChain_id()).child("views").setValue(views);
                    mReference.child("user_chains").child(viewHolder.chain.getFirst_user()).child(viewHolder.chain.getChain_id()).child("views").setValue(views);

                    for (int i = 0; i < viewHolder.chainTags.size(); i++) {
                        mReference.child("tag_chains").child(viewHolder.chainTags.get(i)).child(viewHolder.chain.getChain_id())
                                .child("views").setValue(views);
                    }

                    viewHolder.chain.setViews(views);
                    setViewsTxt(viewHolder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private long getTimestampDifference(long milis){
        Log.d(TAG, "getTimestampDifference: getting timestamp difference.");
        return Calendar.getInstance().getTimeInMillis() - milis;
    }

    @Override
    public int getItemCount() {
        return mMainfeedList.size();
    }
}


