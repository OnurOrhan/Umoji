package com.umoji.umoji.Profile;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.umoji.umoji.R;
import com.umoji.umoji.Utils.FilePaths;
import com.umoji.umoji.Utils.FileSearch;
import com.umoji.umoji.Utils.GridImageAdapter;
import com.umoji.umoji.Utils.UniversalImageLoader;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class GalleryActivity extends AppCompatActivity {
    private static final String TAG = "GalleryActivity";
    private static final int NUM_GRID_COLUMNS = 3;

    private Context mContext = GalleryActivity.this;
    private StorageReference mStorageRef;
    private FirebaseAuth mAuth;
    private String userID;

    //widgets
    private GridView gridView;
    private ImageView galleryImage;
    private ImageView closeBtn;
    private TextView saveBtn;
    private TextView percentage;
    private ProgressBar mProgressBar;
    private Spinner directorySpinner;

    //vars
    private ArrayList<String> directories;
    private String mAppend = "file:/";
    private String mSelectedImage;
    private int PERMISSION_READ_EXTERNAL_STORAGE = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        mAuth = FirebaseAuth.getInstance();
        if(mAuth.getCurrentUser() != null) userID = mAuth.getCurrentUser().getUid();
        else finish();

        mStorageRef = FirebaseStorage.getInstance().getReference();
        initImageLoader();
        checkForPermission();

        galleryImage = (ImageView) findViewById(R.id.galleryImageView);
        gridView = (GridView) findViewById(R.id.gridView);
        directorySpinner = (Spinner) findViewById(R.id.spinnerDirectory);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.getIndeterminateDrawable().setColorFilter(mContext.getResources().getColor(R.color.progressBarColor), PorterDuff.Mode.SRC_IN);
        mProgressBar.setVisibility(View.GONE);
        percentage = (TextView) findViewById(R.id.pp_percentage);
        percentage.setText("");
        directories = new ArrayList<>();

        closeBtn = (ImageView) findViewById(R.id.ivCloseShare);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: closing the gallery fragment.");
                finish();
            }
        });

        saveBtn = (TextView) findViewById(R.id.saveProfilePhoto);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSelectedImage != null && !TextUtils.isEmpty(mSelectedImage)) {
                    mProgressBar.setVisibility(View.VISIBLE);
                    enableWidgets(false);

                    Toast.makeText(mContext, "Your new profile photo is being uploaded...",
                            Toast.LENGTH_SHORT).show();

                    Uri file = Uri.fromFile(new File(mSelectedImage));
                    UploadTask uploadTask = mStorageRef.child("profile_photos").child(userID + ".jpg").putFile(file);
                    uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            updateProgress(taskSnapshot);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            enableWidgets(true);
                            Toast.makeText(mContext, "Error uploading the photo!",
                                    Toast.LENGTH_SHORT).show();
                            mProgressBar.setVisibility(View.GONE);
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Toast.makeText(mContext, "Your profile photo has been uploaded!",
                                    Toast.LENGTH_SHORT).show();
                            mProgressBar.setVisibility(View.GONE);
                            finish();
                        }
                    });
                }
            }
        });

        init();
    }

    private void enableWidgets(boolean enable){
        gridView.setEnabled(enable);
        saveBtn.setEnabled(enable);
        closeBtn.setEnabled(enable);
    }

    private void initImageLoader() {
        UniversalImageLoader universalImageLoader = new UniversalImageLoader(mContext);
        ImageLoader.getInstance().init(universalImageLoader.getConfig());
    }

    private void checkForPermission() {
        //check for permission
        if(ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            //ask for permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void init(){
        FilePaths filePaths = new FilePaths();

        directories.add(filePaths.CAMERA);
        //check for other folders inSide "/storage/emulated/0/pictures"
        FileSearch.addDirectoryPaths(directories, filePaths.PICTURES);

        ArrayList<String> directoryNames = new ArrayList<>();
        for (int i = 0; i < directories.size(); i++) {
            Log.d(TAG, "init: directory: " + directories.get(i));
            int index = directories.get(i).lastIndexOf("/");
            String string = directories.get(i).substring(index+1);
            directoryNames.add(string);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext, android.R.layout.simple_spinner_item, directoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        directorySpinner.setAdapter(adapter);

        directorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick: selected: " + directories.get(position));

                //setup our image grid for the directory chosen
                setupGridView(directories.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void setupGridView(String selectedDirectory){
        Log.d(TAG, "setupGridView: directory chosen: " + selectedDirectory);
        final ArrayList<String> imgURLs = FileSearch.getFilePaths(selectedDirectory);
        Collections.reverse(imgURLs);

        //set the grid column width
        int gridWidth = getResources().getDisplayMetrics().widthPixels;
        int imageWidth = gridWidth/NUM_GRID_COLUMNS;
        gridView.setColumnWidth(imageWidth);

        //use the grid adapter to adapter the images to gridview
        GridImageAdapter adapter = new GridImageAdapter(mContext, R.layout.layout_grid_imageview, mAppend, imgURLs, false);
        gridView.setAdapter(adapter);

        //set the first image to be displayed when the activity fragment view is inflated
        if(imgURLs.size() > 0){
            try{
                setImage(imgURLs.get(0), galleryImage, mAppend);
                mSelectedImage = imgURLs.get(0);
            }catch (ArrayIndexOutOfBoundsException e){
                Log.e(TAG, "setupGridView: ArrayIndexOutOfBoundsException: " +e.getMessage() );
            }
        }

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick: selected an image: " + imgURLs.get(position));

                setImage(imgURLs.get(position), galleryImage, mAppend);
                mSelectedImage = imgURLs.get(position);
            }
        });

    }

    private void setImage(String imgURL, ImageView image, String append){
        Log.d(TAG, "setImage: setting image");

        ImageLoader imageLoader = ImageLoader.getInstance();

        imageLoader.displayImage(append + imgURL, image, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
                mProgressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                mProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                mProgressBar.setVisibility(View.GONE);
            }
        });
    }

    private void updateProgress(UploadTask.TaskSnapshot taskSnapshot) {
        long filesize = taskSnapshot.getTotalByteCount();
        long uploadBytes = taskSnapshot.getBytesTransferred();
        long progress = (100 * uploadBytes) / filesize;

        String str = "%" + Long.toString(progress);
        percentage.setText(str);
    }
}
