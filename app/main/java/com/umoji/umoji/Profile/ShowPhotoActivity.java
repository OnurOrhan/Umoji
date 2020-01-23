package com.umoji.umoji.Profile;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.umoji.umoji.Models.User;
import com.umoji.umoji.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
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
import java.util.Objects;

public class ShowPhotoActivity extends AppCompatActivity {
    private Context mContext;

    private StorageReference mStorageRef;
    private DatabaseReference mRef;
    private RoundedImageView profilePhoto;
    private TextView topUsernameTxt;

    private RelativeLayout rel1, rel2, rel3;
    private TextView usernameTxt, nameTxt, descriptionTxt;
    private ImageView usernameRef, nameRef, descriptionRef;
    private Button saveBtn;

    private boolean isSelf;
    private String user_id;
    private String username, name, description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_photo);
        mContext = ShowPhotoActivity.this;

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mRef = FirebaseDatabase.getInstance().getReference();

        Intent intent = getIntent();
        user_id = intent.getStringExtra("user_id");

        isSelf = user_id.equals(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());

        final TextView topUsernameTxt = (TextView) findViewById(R.id.show_photo_top_username);

        final ImageView changeBtn = (ImageView) findViewById(R.id.shown_photo_change_icon);
        profilePhoto = (RoundedImageView) findViewById(R.id.shown_photo);

        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels;

        profilePhoto.getLayoutParams().width = (int) dpWidth*4/5;
        profilePhoto.getLayoutParams().height = (int) dpWidth*4/5;

        if(isSelf){
            rel1 = (RelativeLayout) findViewById(R.id.show_photo_username_layout);
            rel2 = (RelativeLayout) findViewById(R.id.show_photo_name_layout);
            rel3 = (RelativeLayout) findViewById(R.id.show_photo_description_layout);

            usernameTxt = (TextView) findViewById(R.id.show_photo_username);
            nameTxt = (TextView) findViewById(R.id.show_photo_name);
            descriptionTxt = (TextView) findViewById(R.id.show_photo_description);

            usernameRef = (ImageView) findViewById(R.id.show_photo_username_refresh);
            nameRef = (ImageView) findViewById(R.id.show_photo_name_refresh);
            descriptionRef = (ImageView) findViewById(R.id.show_photo_description_refresh);

            mRef.child("users").orderByChild("user_id").equalTo(user_id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot singleSnapshot: dataSnapshot.getChildren()){
                        User temp = singleSnapshot.getValue(User.class);

                        description = Objects.requireNonNull(temp).getDescription();
                        name = temp.getName();
                        username = temp.getUsername();
                        topUsernameTxt.setText(username);

                        // ------------------------
                        rel1.setVisibility(View.VISIBLE);
                        rel2.setVisibility(View.VISIBLE);
                        rel3.setVisibility(View.VISIBLE);
                        changeBtn.setVisibility(View.VISIBLE);

                        saveBtn = (Button) findViewById(R.id.show_photo_save_btn);
                        saveBtn.setVisibility(View.VISIBLE);

                        setUsername();
                        setName();
                        setDescription();

                        usernameRef.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                setUsername();
                            }
                        });

                        nameRef.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                setName();
                            }
                        });

                        descriptionRef.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                setDescription();
                            }
                        });

                        saveBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                buttonFunc();
                            }
                        });

                        // ------------------------
                        View.OnClickListener listener = new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent i = new Intent(mContext, GalleryActivity.class);
                                startActivity(i);
                            }
                        };

                        changeBtn.setOnClickListener(listener);
                        profilePhoto.setOnClickListener(listener);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        setPhoto();

        ImageView backarrow = (ImageView) findViewById(R.id.shown_photo_backarrow);
        backarrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        setPhoto();
    }

    private void buttonFunc() {
        hideSoftKeyboard();

        final String un = usernameTxt.getText().toString().trim();
        final String n = nameTxt.getText().toString().trim();
        final String d = descriptionTxt.getText().toString().trim();

        if(un.length() < 6){
            Toast.makeText(mContext, "Minimum 6 character username needed!",
                Toast.LENGTH_SHORT).show();
            return;
        }

        if(!TextUtils.isEmpty(un)){
            mRef.child("users").orderByChild("username").equalTo(un).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.getChildrenCount() != 0 && !un.equals(username)){ // If the username already exists...
                        Toast.makeText(mContext, "Username already exists!",
                                Toast.LENGTH_SHORT).show();

                    } else { // If the username is unique...
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(un).build();
                        Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                mRef.child("users").child(user_id).child("username").setValue(un);

                                if(!TextUtils.isEmpty(n)) mRef.child("users").child(user_id).child("name").setValue(n);
                                else mRef.child("users").child(user_id).child("name").setValue(un);

                                mRef.child("users").child(user_id).child("description").setValue(d);

                                Intent i = new Intent(mContext, ProfileActivity.class);
                                i.putExtra("user_id", user_id);
                                startActivity(i);
                                finish();
                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        } else {
            Toast.makeText(mContext, "Please enter your username!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void setUsername(){
        usernameTxt.setText(username);
    }

    private void setName(){
        nameTxt.setText(name);
    }

    private void setDescription(){
        descriptionTxt.setText(description);
    }

    private void setPhoto() {
        try {
            //this.progressBar.setVisibility(View.VISIBLE);

            final File localFile = File.createTempFile("images", "jpg");
            mStorageRef.child("profile_photos/" + user_id + ".jpg")
                    .getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    //progressBar.setVisibility(View.GONE);
                    Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    profilePhoto.setImageBitmap(myBitmap);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    //progressBar.setVisibility(View.GONE);
                }
            });

        } catch (IOException e) {
            //progressBar.setVisibility(View.GONE);
            e.printStackTrace();
        }
    }

    private void hideSoftKeyboard(){
        if(getCurrentFocus() != null){
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        }
    }

}
