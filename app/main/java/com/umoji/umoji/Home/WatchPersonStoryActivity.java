package com.umoji.umoji.Home;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
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
import com.umoji.umoji.Models.User;
import com.umoji.umoji.Models.Video;
import com.umoji.umoji.Profile.ProfileActivity;
import com.umoji.umoji.R;
import com.umoji.umoji.Utils.CountBarAdapter;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.EmojiTextView;
import com.vanniktech.emoji.one.EmojiOneProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class WatchPersonStoryActivity extends AppCompatActivity {
    private static final String TAG = "WatchPersonStoryActivity";
    private static final int STORY_LIMIT = 25;
    private Context mContext;

    private StorageReference storageReference;
    private DatabaseReference mReference;

    // Video frame
    private VideoView mVideoView;
    private ProgressBar mProgressBar;
    private TextView mPercent;
    private ImageView mProfilePicture;
    private TextView mUsername;
    private EmojiTextView emojiTextView;

    private LinearLayoutManager countBarManager;
    private int storyCount;
    private CountBarAdapter countBarAdapter;
    private RecyclerView countBarView;

    // Control buttons
    private Button previousBtn, nextBtn, closeBtn, deleteBtn;

    // Emoji information
    private ArrayList<String> mPeople;
    private int personIndex;

    // Video information
    private ArrayList<Video> chainVideos;
    private Video currentVideo;
    private int videoIndex;

    // The current user and if he has any videos in the chain
    private String currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_person_story);

        mContext = WatchPersonStoryActivity.this;
        EmojiManager.install(new EmojiOneProvider());

        mReference = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        Intent intent = getIntent();
        mPeople = intent.getStringArrayListExtra("mPeople");
        personIndex = intent.getIntExtra("personIndex", 0);

        chainVideos = new ArrayList<>();

        currentUser = FirebaseAuth.getInstance().getCurrentUser().getUid();

        mProfilePicture = (ImageView) findViewById(R.id.person_story_profile_photo);
        mUsername = (TextView) findViewById(R.id.person_story_username);
        emojiTextView = (EmojiTextView) findViewById(R.id.person_story_emoji_txt);

        mVideoView = (VideoView) findViewById(R.id.person_story_video);
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                nextFunc();
            }
        });

        previousBtn = (Button) findViewById(R.id.person_story_previous_btn);
        nextBtn = (Button) findViewById(R.id.person_story_next_btn);
        closeBtn = (Button) findViewById(R.id.person_story_close_btn);
        // deleteBtn = (Button) findViewById(R.id.person_story_delete_btn);

        mProgressBar = (ProgressBar) findViewById(R.id.person_story_progressbar);
        mProgressBar.getIndeterminateDrawable().setColorFilter(mContext.getResources().getColor(R.color.progressBarColor), PorterDuff.Mode.SRC_IN);
        mProgressBar.setVisibility(View.VISIBLE);
        mPercent = (TextView) findViewById(R.id.person_story_percent);

        countBarManager = new LinearLayoutManager(mContext);
        countBarManager.setOrientation(LinearLayoutManager.HORIZONTAL);

        countBarView = (RecyclerView) findViewById(R.id.person_story_count_bars);
        countBarView.setHasFixedSize(true);
        countBarView.setLayoutManager(countBarManager);
        countBarView.setItemViewCacheSize(25);

        getVideoList();
        setPreviousBtn();
        setNextBtn();
        setCloseBtn();
        // setDeleteBtn();
    }

    private void setPreviousBtn(){
        previousBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                prevFunc();
            }
        });
    }

    private void prevFunc(){
        mVideoView.stopPlayback();
        // mVideoView.setVideoURI(null);

        if (videoIndex <= 0) previousPerson();
        else {
            videoIndex--;
            currentVideo = chainVideos.get(videoIndex);
            countBarAdapter.setUnwatched((CountBarAdapter.BarViewHolder) countBarView.findViewHolderForLayoutPosition(videoIndex+1));
            setChainItem();
        }
    }

    private void setNextBtn(){
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextFunc();
            }
        });
    }

    private void nextFunc(){
        mVideoView.stopPlayback();
        // mVideoView.setVideoURI(null);

        if(videoIndex + 1 >= chainVideos.size()) nextPerson();
        else {
            videoIndex++;
            currentVideo = chainVideos.get(videoIndex);
            countBarAdapter.setWatched((CountBarAdapter.BarViewHolder) countBarView.findViewHolderForLayoutPosition(videoIndex));
            setChainItem();
        }
    }

    private void previousPerson(){
        if(personIndex <= 0) finish();
        else {
            personIndex--;
            chainVideos = new ArrayList<>();
            getVideoList();
        }
    }

    private void nextPerson() {
        if(personIndex + 1 >= mPeople.size()) finish();
        else {
            personIndex++;
            chainVideos = new ArrayList<>();
            getVideoList();
        }
    }

    private void setCloseBtn(){
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void getVideoList() {
        setUsernameAndPP();

        mReference.child("user_stories").child(mPeople.get(personIndex)).orderByChild("date_created").limitToLast(STORY_LIMIT)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        long videoCount = 0;

                        for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
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
                            video.setStory_tag(temp.getStory_tag());

                            if(videoCount == 0){
                                currentVideo = video;
                                videoIndex = 0;
                            }

                            chainVideos.add(video);
                            videoCount++;

                        }

                        storyCount = (int) videoCount;
                        setCountBars();
                        setChainItem();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void setChainItem(){
        setVideo();
        setEmoji();
    }

    private void setDelete(){
        // setDelete();
    }

    public void setCountBars(){
        countBarAdapter = new CountBarAdapter(storyCount, mContext);
        countBarView.setAdapter(countBarAdapter);
    }

    public void setEmoji() {
        emojiTextView.setText(currentVideo.getStory_tag());
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

        String temp_user = mPeople.get(personIndex);

        Query query = mReference.child("users").orderByChild("user_id").equalTo(temp_user);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot :  dataSnapshot.getChildren()){
                    mUsername.setText(Objects.requireNonNull(singleSnapshot.getValue(User.class)).getUsername());

                    try {
                        final File localFile = File.createTempFile("images", "jpg");
                        storageReference.child("profile_photos/" + temp_user + ".jpg")
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
                            i.putExtra("user_id", temp_user);
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

}
