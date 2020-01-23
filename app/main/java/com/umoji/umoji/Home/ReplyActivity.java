package com.umoji.umoji.Home;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.umoji.umoji.Models.Chain;
import com.umoji.umoji.Models.Notification;
import com.umoji.umoji.Models.Video;
import com.umoji.umoji.R;
import com.umoji.umoji.Share.CustomCameraActivity;
import com.umoji.umoji.Utils.FilePaths;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.iceteck.silicompressorr.SiliCompressor;
import com.umoji.umoji.Utils.MainfeedListAdapter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URISyntaxException;
import java.util.Objects;

public class ReplyActivity extends AppCompatActivity {
    private static final String TAG = "ReplyActivity";
    private Context mContext;

    private FilePaths filePaths;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private String user_id;
    private DatabaseReference myRef;
    private StorageReference storageRef;

    private String videoPath;
    private Uri videoUri;

    private FrameLayout mFrameLayout;
    private VideoView mVideoView;
    private ProgressBar progressBar;
    private TextView percentProgress;
    private String chain_id, creator_id;

    private TextView retryText;
    private ImageView backButton;

    private Button cancelBtn, replyBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reply);
        Log.d(TAG, "onCreate: created...");

        mContext = ReplyActivity.this;
        filePaths = new FilePaths();

        Intent intent = getIntent();
        chain_id = intent.getStringExtra("chain_id");
        creator_id = intent.getStringExtra("creator_id");

        retryText = (TextView) findViewById(R.id.reply_retry_text);
        backButton = (ImageView) findViewById(R.id.reply_back_arrow);

        View.OnClickListener retryListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, CustomCameraActivity.class);
                i.putExtra("task", "reply");
                i.putExtra("chain_id", chain_id);
                i.putExtra("creator_id", creator_id);
                startActivity(i);
                finish();
            }
        };

        retryText.setOnClickListener(retryListener);
        backButton.setOnClickListener(retryListener);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        user_id = user.getUid();
        myRef= FirebaseDatabase.getInstance().getReference();
        storageRef = FirebaseStorage.getInstance().getReference();

        mVideoView = (VideoView) findViewById(R.id.reply_video_view);
        mFrameLayout = (FrameLayout) findViewById(R.id.reply_frame);

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });

        // setting the video
        videoPath = intent.getStringExtra("video_path");
        videoUri = Uri.fromFile(new File(videoPath));
        mVideoView.setVideoPath(videoPath);
        mVideoView.setVideoURI(videoUri);
        mVideoView.requestFocus();
        mVideoView.start();

        /* MediaController mediaController = new MediaController(mContext);
        mVideoView.setMediaController(mediaController);
        mediaController.setAnchorView(mVideoView); */

        cancelBtn = (Button) findViewById(R.id.reply_cancel_button);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, HomeActivity.class);
                startActivity(i);
                finish();
            }
        });

        replyBtn = (Button) findViewById(R.id.reply_share_button);
        setReplyBtn();

        progressBar = (ProgressBar) findViewById(R.id.reply_progressbar);
        progressBar.setVisibility(View.GONE);
        progressBar.getIndeterminateDrawable().setColorFilter(mContext.getResources().getColor(R.color.progressBarColor), PorterDuff.Mode.SRC_IN);
        percentProgress = (TextView) findViewById(R.id.reply_percent);
        percentProgress.setText("");
    }

    private void setReplyBtn(){
        replyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAllEnabled(false);

                progressBar.setVisibility(View.VISIBLE);

                final String newKey = myRef.child("videos").push().getKey();

                try {
                    Toast.makeText(mContext, "Please wait while your video is being compressed...",
                            Toast.LENGTH_LONG).show();

                    compressVideo(newKey);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, "Failed to compress video!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void compressVideo(final String newKey) {
        try {
            final String newPath = filePaths.CAMERA;
            checkForPermission();

            CompressAsyncTask comp = new CompressAsyncTask(newKey);
            comp.execute(newPath);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private void uploadThumbnail(final String vKey) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(mContext, videoUri);
        Bitmap thumb = retriever.getFrameAtTime();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        thumb.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = storageRef.child("thumbnails/" + user.getUid() + "/" + vKey + ".jpg").putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Intent i = new Intent(mContext, WatchActivity.class);
                i.putExtra("chain_id", chain_id);
                i.putExtra("video_id", vKey);
                startActivity(i);
                finish();
            }
        });
    }

    private void updateProgress(UploadTask.TaskSnapshot taskSnapshot){
        long filesize = taskSnapshot.getTotalByteCount();
        long uploadBytes = taskSnapshot.getBytesTransferred();
        long progress = (100 * uploadBytes) / filesize;

        String str = "%" + Long.toString(progress);
        percentProgress.setText(str);
        progressBar.setProgress((int) progress);
    }

    private void uploadVideo(final String newKey){
        final StorageReference videoRef = storageRef.child("videos/" + user_id + "/" + newKey + ".mp4");

        Toast.makeText(mContext, "Your reply is being uploaded...",
                Toast.LENGTH_LONG).show();

        videoRef.putFile(videoUri)
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        updateProgress(taskSnapshot);
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(mContext, "Video successfully uploaded!",
                                Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        percentProgress.setText("");

                        addVideoData(newKey, videoRef);
                        uploadThumbnail(newKey);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(mContext, "Error uploading the video!",
                                Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        percentProgress.setText("");
                        setAllEnabled(true);
                    }
                });
    }

    private void addVideoData(final String newKey, final StorageReference videoRef){
        videoRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                final long cTime = storageMetadata.getCreationTimeMillis();

                final Video video = new Video(newKey, user_id, chain_id);
                video.setDate_created(cTime);
                video.setVideo_format(storageMetadata.getContentType());

                videoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        video.setVideo_uri(uri.toString());

                        myRef.child("videos").child(newKey).setValue(video);
                        myRef.child("chain_videos").child(chain_id).child(newKey).setValue(video);

                        String notifKey = myRef.child("notifications").child(creator_id).push().getKey();
                        Notification notif = new Notification(1, notifKey, user_id, creator_id);
                        notif.setKey1(newKey);
                        notif.setKey2(chain_id);
                        notif.setDate_created(cTime);

                        myRef.child("notifications").child(creator_id).child(Objects.requireNonNull(notifKey)).setValue(notif);

                        incrementResponses(newKey);
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Uh-oh, an error occurred!
            }
        });
    }

    private void incrementResponses(String newKey) {
        myRef.child("chains").orderByChild("chain_id").equalTo(chain_id)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                        Chain temp = Objects.requireNonNull(singleSnapshot.getValue(Chain.class));
                        int responses = temp.getResponses() + 1;
                        String chain_id = temp.getChain_id();

                        myRef.child("chains").child(chain_id).child("responses").setValue(responses);
                        myRef.child("user_chains").child(creator_id).child(chain_id).child("responses").setValue(responses);

                        // Tags couldn't be found (tag_chains may not contain correct "responses" value
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
    }

    private void setAllEnabled(boolean isEnabled) {
        replyBtn.setEnabled(isEnabled);
        cancelBtn.setEnabled(isEnabled);
        backButton.setEnabled(isEnabled);
        retryText.setEnabled(isEnabled);
    }

    private void checkForPermission() {
        //check for permission
        if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            //ask for permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }
        }

        if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            //ask for permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            }
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class CompressAsyncTask extends AsyncTask<String, Void, String> {
        private String nKey;

        CompressAsyncTask(String newKey) {
            this.nKey = newKey;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... paths) {
            String temp;
            try {
                temp = SiliCompressor.with(mContext).compressVideo(videoPath, paths[0], 1280, 720, 2306000);//, 720, 1280, 1024);
            } catch (URISyntaxException e) {
                e.printStackTrace();
                temp = "";
            } return temp;
        }

        @Override
        protected void onPostExecute(String s) {
            videoUri = Uri.fromFile(new File(s));
            uploadVideo(nKey);
        }
    }

}
