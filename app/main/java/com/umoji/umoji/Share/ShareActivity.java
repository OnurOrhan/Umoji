package com.umoji.umoji.Share;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.umoji.umoji.Home.HomeActivity;
import com.umoji.umoji.Home.WatchActivity;
import com.umoji.umoji.Models.Chain;
import com.umoji.umoji.Models.Video;
import com.umoji.umoji.R;
import com.umoji.umoji.Utils.FilePaths;
import com.umoji.umoji.Utils.PageTransformer;
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
import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.one.EmojiOneProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Stack;

public class ShareActivity extends AppCompatActivity {
    private static final String TAG = "ShareActivity";

    // to execute "ffmpeg -version" command you just need to pass "-version"
    //private FFmpeg ffmpeg;
    private FilePaths filePaths;

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private String user_id;

    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private StorageReference storageRef;
    private Uri videoUri;
    private String videoPath;

    private FrameLayout mFrameLayout;
    private VideoView mVideoView;
    private ProgressBar progressBar;
    private TextView percentProgress;
    private Context mContext;

    private TextView retryText;
    private ImageView backButton;

    private EditText titleTxt;

    private ImageView tagClearBtn;
    private EmojiEditText tagsTxt;
    private ImageView emojiBtn;
    private RelativeLayout rootLayout;
    private EmojiPopup emojiPopup;

    private Stack<String> tags;
    private Stack<Integer> tagLengths;

    private boolean emojiKeyOpen, titleKeyOpen;
    private boolean emojiHasOpened, titleHasOpened;

    private Button cancelBtn, shareBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EmojiManager.install(new EmojiOneProvider());
        super.onCreate(savedInstanceState);
        mContext = ShareActivity.this;

        emojiKeyOpen = false;
        titleKeyOpen = false;
        emojiHasOpened = false;
        titleHasOpened = false;

        setContentView(R.layout.activity_share);
        Log.d(TAG, "onCreate: created...");

        tags = new Stack<>();
        tagLengths = new Stack<>();

        retryText = (TextView) findViewById(R.id.share_retry_text);
        backButton = (ImageView) findViewById(R.id.share_back_arrow);

        View.OnClickListener retryListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, CustomCameraActivity.class);
                i.putExtra("task", "share");
                i.putExtra("chain_id", "");
                i.putExtra("creator_id", "");
                startActivity(i);
                finish();
            }
        };

        retryText.setOnClickListener(retryListener);
        backButton.setOnClickListener(retryListener);

        //initFfmpeg();
        filePaths = new FilePaths();

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        user_id = user.getUid();

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

        storageRef = FirebaseStorage.getInstance().getReference();

        mVideoView = (VideoView) findViewById(R.id.share_video_view);
        mFrameLayout = (FrameLayout) findViewById(R.id.share_frame);

        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels;
        mVideoView.getLayoutParams().width = (int) dpWidth*5/6;
        mVideoView.getLayoutParams().height = mVideoView.getLayoutParams().width*4/3;

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });

        videoPath = getIntent().getStringExtra("video_path");
        videoUri = Uri.fromFile(new File(videoPath));
        mVideoView.setVideoPath(videoPath);
        mVideoView.setVideoURI(videoUri);
        mVideoView.requestFocus();
        mVideoView.start();

        /* MediaController mediaController = new MediaController(mContext);
        mVideoView.setMediaController(mediaController);
        mediaController.setAnchorView(mVideoView); */

        // Emoji section
        titleTxt = (EditText) findViewById(R.id.share_title);
        tagsTxt = (EmojiEditText) findViewById(R.id.share_tags_text);
        emojiBtn = (ImageView) findViewById(R.id.share_emoji_btn);
        rootLayout = (RelativeLayout) findViewById(R.id.share_tag_layout);

        tagClearBtn = (ImageView) findViewById(R.id.share_tag_clear);
        tagClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyboard();
                tagsTxt.clearFocus();
                tagsTxt.setText("");
                tags.clear();
                tagLengths.clear();

                emojiHasOpened = false;
                emojiKeyOpen = false;
                titleHasOpened = false;
                titleKeyOpen = false;
            }
        });

        setUpEmoji();

        cancelBtn = (Button) findViewById(R.id.share_cancel_button);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, HomeActivity.class);
                startActivity(i);
                finish();
            }
        });

        shareBtn = (Button) findViewById(R.id.share_share_button);
        setShareBtn();
        progressBar = (ProgressBar) findViewById(R.id.share_progressbar);
        progressBar.getIndeterminateDrawable().setColorFilter(mContext.getResources().getColor(R.color.progressBarColor), PorterDuff.Mode.SRC_IN);
        progressBar.setVisibility(View.GONE);
        percentProgress = (TextView) findViewById(R.id.share_percent);
        percentProgress.setText("");
    }

    private void setShareBtn(){
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAllEnabled(false);

                progressBar.setVisibility(View.VISIBLE);
                hideSoftKeyboard();
                tagsTxt.clearFocus();

                final String newKey = myRef.child("videos").push().getKey();

                try {
                    Toast.makeText(mContext, "Please wait while your video is being compressed...",
                            Toast.LENGTH_LONG).show();

                    //String fileName = videoPath.substring(videoPath.lastIndexOf('/') + 1, videoPath.lastIndexOf('.'));
                    compressVideo(newKey);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, "Failed to compress video!",
                            Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void uploadVideo(final String newKey){
        final String chainKey = myRef.child("chains").push().getKey();
        final StorageReference videoRef = storageRef.child("videos/" + user_id + "/" + newKey + ".mp4");

        Toast.makeText(mContext, "Your video is being uploaded...",
                Toast.LENGTH_LONG).show();
        progressBar.setVisibility(View.VISIBLE);
        videoRef.putFile(videoUri)
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        updateProgress(taskSnapshot);
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(mContext, "Video successfully uploaded!",
                        Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                percentProgress.setText("");

                addVideoData(newKey, chainKey, user_id, videoRef);
                uploadThumbnail(videoUri, newKey, chainKey);
            }
        }).addOnFailureListener(new OnFailureListener() {
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

    private void uploadThumbnail(Uri vUri, final String vKey, final String cKey) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(mContext, vUri);
        Bitmap thumb = retriever.getFrameAtTime();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        thumb.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = storageRef.child("thumbnails/" + user.getUid() + "/" + vKey + ".jpg").putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                setAllEnabled(true);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                /* Intent i = new Intent(mContext, HomeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish(); */

                Intent i = new Intent(mContext, HomeActivity.class);
                /* i.putExtra("chain_id", cKey);
                i.putExtra("video_id", vKey); */
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

    private void addVideoData(final String videoKey, final String chainKey, final String uid, final StorageReference videoRef){
        videoRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                final long cTime = storageMetadata.getCreationTimeMillis();

                final Video video = new Video(videoKey, uid, chainKey);
                video.setDate_created(cTime);
                video.setVideo_format(storageMetadata.getContentType());
                video.setIs_main(true);
                video.setTitle(titleTxt.getText().toString().trim());
                video.setViews(0);

                videoRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        video.setVideo_uri(uri.toString());

                        Chain chain = new Chain(chainKey, videoKey, uid);
                        chain.setDate_created(cTime);
                        chain.setVideo_uri(uri.toString());
                        chain.setTitle(video.getTitle());

                        // Writing the Video data to the database
                        myRef.child("videos").child(videoKey).setValue(video);
                        myRef.child("chain_videos").child(chainKey).child(videoKey).setValue(video);

                        // Writing the Chain data to the database
                        myRef.child("chains").child(chainKey).setValue(chain);
                        myRef.child("user_chains").child(user_id).child(chainKey).setValue(chain);

                        String temp;
                        ArrayList<String> tempTags = new ArrayList<>();

                        while (!tags.empty()){
                            temp = tags.pop();
                            tempTags.add(temp);

                            myRef.child("chains").child(chainKey).child("tags").push().setValue(temp);
                            myRef.child("user_chains").child(user_id).child(chainKey).child("tags").push().setValue(temp);
                            myRef.child("tag_chains").child(temp).child(chainKey).setValue(chain);
                        }

                        for (int z = 0; z < tempTags.size(); z++){
                            for (int j = 0; j < tempTags.size(); j++){
                                myRef.child("tag_chains").child(tempTags.get(z)).child(chainKey).child("tags").push().setValue(tempTags.get(j));
                            }
                        }

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

    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = { MediaStore.Video.Media.DATA };
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            res = cursor.getString(column_index);
        }
        assert cursor != null;
        cursor.close();
        return res;
    }

    private void setAllEnabled(boolean isEnabled) {
        shareBtn.setEnabled(isEnabled);
        cancelBtn.setEnabled(isEnabled);
        backButton.setEnabled(isEnabled);
        retryText.setEnabled(isEnabled);
        titleTxt.setEnabled(isEnabled);
        tagsTxt.setEnabled(isEnabled);
    }

    private void hideSoftKeyboard(){
        if(getCurrentFocus() != null){
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
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

    private void setUpEmoji() {
        emojiPopup = EmojiPopup.Builder.fromRootView(rootLayout)
                .setOnEmojiBackspaceClickListener(ignore -> Log.d(TAG, "Clicked on Backspace"))
                .setOnEmojiClickListener((ignore, ignore2) -> Log.d(TAG, "Clicked on emoji"))
                .setOnEmojiPopupShownListener(() -> emojiBtn.setImageResource(R.drawable.emoji_one_category_smileysandpeople))
                .setOnSoftKeyboardOpenListener(ignore -> Log.d(TAG, "Opened soft keyboard"))
                .setOnEmojiPopupDismissListener(() -> emojiBtn.setImageResource(R.drawable.emoji_one_category_smileysandpeople))
                .setOnSoftKeyboardCloseListener(() -> Log.d(TAG, "Closed soft keyboard"))
                .setKeyboardAnimationStyle(R.style.emoji_fade_animation_style)
                .setPageTransformer(new PageTransformer())
                .build(tagsTxt);

        // Clicking the emoji button
        emojiBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!emojiKeyOpen) {
                    if(titleKeyOpen){
                        titleTxt.clearFocus();
                        titleKeyOpen = false;
                    }

                    emojiKeyOpen = true;
                    emojiHasOpened = true;
                    titleHasOpened = false;
                    emojiPopup.toggle();
                } else {
                    emojiKeyOpen = false;
                    emojiHasOpened = false;
                    hideSoftKeyboard();
                    tagsTxt.clearFocus();
                }
            }
        });

        titleTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(titleTxt.hasFocus() && titleHasOpened){
                    hideSoftKeyboard();
                    titleTxt.clearFocus();

                    emojiHasOpened = false;
                    emojiKeyOpen = false;
                    titleHasOpened = false;
                    titleKeyOpen = false;
                }
            }
        });

        titleTxt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus){
                    if(emojiHasOpened){
                        hideSoftKeyboard();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        Objects.requireNonNull(imm).showSoftInput(titleTxt, InputMethodManager.SHOW_IMPLICIT);
                    }

                    titleKeyOpen = true;
                    titleHasOpened = true;
                    emojiHasOpened = false;
                } else {
                    titleKeyOpen = false;
                }
            }
        });

        tagsTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tagsTxt.hasFocus() && emojiHasOpened){
                    hideSoftKeyboard();
                    tagsTxt.clearFocus();

                    emojiHasOpened = false;
                    emojiKeyOpen = false;
                    titleHasOpened = false;
                    titleKeyOpen = false;
                }
            }
        });

        // Tag EditText focus change
        tagsTxt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) { // got focus
                    emojiPopup.toggle();

                    emojiHasOpened = true;
                    emojiKeyOpen = true;
                    titleHasOpened = false;
                } else {
                    emojiKeyOpen = false;
                }
            }
        });

        tagsTxt.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event){
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DEL:
                        tagsTxt.setText("");
                        tags.clear();
                        tagLengths.clear();
                        return true;
                    default:
                        break;

                } return false;
            }
        });

        tagsTxt.addTextChangedListener(new TextWatcher() {
            int first = 0;
            int last = 0;
            int difference = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                first = s.length();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                last = s.length();
                difference = last - first;

                if (difference > 0){
                    tags.push(s.subSequence(first, last).toString());
                    tagLengths.push(difference);

                } else if (difference < 0){
                    while (difference < 0){
                        int l = tagLengths.pop();
                        tags.pop();
                        difference += l;
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

}
