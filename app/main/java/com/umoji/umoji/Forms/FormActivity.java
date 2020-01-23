package com.umoji.umoji.Forms;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.umoji.umoji.Home.HomeActivity;
import com.umoji.umoji.MainActivity;
import com.umoji.umoji.Profile.GalleryActivity;
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
import java.util.ArrayList;
import java.util.Objects;

public class FormActivity extends AppCompatActivity {
    private static final String TAG = "FormActivity";
    private Context mContext;

    private Button startBtn;
    private EditText usernameTxt, nameTxt, descriptionTxt;//, tag1, tag2, tag3;

    private ProgressBar mProgressBar;
    private RoundedImageView mPhotoView;
    private ImageView mEditBtn;

    private DatabaseReference mRef;
    private StorageReference mStorageRef;
    private FirebaseAuth myAuth;
    private String currentUser;

    private NumberPicker yearPicker;
    private RadioGroup radioGroup;
    private RadioButton radioButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);
        mContext = FormActivity.this;

        mRef = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        myAuth = FirebaseAuth.getInstance();
        if (myAuth.getCurrentUser() == null){
            Intent i = new Intent(FormActivity.this, MainActivity.class);
            startActivity(i);
        }

        currentUser = Objects.requireNonNull(myAuth.getCurrentUser()).getUid();

        startBtn = (Button) findViewById(R.id.start_button);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonFunc();
            }
        });

        mProgressBar = (ProgressBar) findViewById(R.id.form__photo_progressbar);
        mProgressBar.getIndeterminateDrawable().setColorFilter(mContext.getResources().getColor(R.color.progressBarColor), PorterDuff.Mode.SRC_IN);
        mProgressBar.setVisibility(View.GONE);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, GalleryActivity.class);
                startActivity(i);
            }
        };

        mPhotoView = (RoundedImageView) findViewById(R.id.form_profile_photo);
        mEditBtn = (ImageView) findViewById(R.id.form_profile_photo_edit);
        mPhotoView.setOnClickListener(listener);
        mEditBtn.setOnClickListener(listener);

        usernameTxt = (EditText) findViewById(R.id.form_username);
        usernameTxt.setText(getIntent().getStringExtra("username"));
        nameTxt = (EditText) findViewById(R.id.form_name);
        descriptionTxt = (EditText) findViewById(R.id.form_description);

        /*tag1 = (EditText) findViewById(R.id.form_tag1);
        tag2 = (EditText) findViewById(R.id.form_tag2);
        tag3 = (EditText) findViewById(R.id.form_tag3);*/

        yearPicker = (NumberPicker) findViewById(R.id.form_year_picker);
        yearPicker.setMaxValue(2004);
        yearPicker.setMinValue(1897);
        yearPicker.setValue(1995);
        yearPicker.setWrapSelectorWheel(false);

        radioGroup = (RadioGroup) findViewById(R.id.sex_radio_group);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mProgressBar.setVisibility(View.VISIBLE);
        setPhoto();
    }

    private void buttonFunc() {
        hideSoftKeyboard();

        final String username = usernameTxt.getText().toString().trim();
        final String name = nameTxt.getText().toString().trim();

        if(username.length() < 6){
            Toast.makeText(mContext, "Minimum 6 character username needed!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        /*final ArrayList<String> tags = new ArrayList<>();
        addTag(tags, tag1.getText().toString().trim());
        addTag(tags, tag2.getText().toString().trim());
        addTag(tags, tag3.getText().toString().trim());

        for (int i = 0; i < tags.size(); i++){
            DatabaseReference tempRef = mRef.child("users").child(currentUser).child("tags").push();
            tempRef.setValue(tags.get(i));
            mRef.child("tag_users").child(tags.get(i)).child(currentUser).setValue(tags.get(i));
        }*/

        if(!TextUtils.isEmpty(username)){
            mRef.child("users").orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.getChildrenCount() != 0){ // If the username already exists...
                        Toast.makeText(mContext, "Username already exists!",
                                Toast.LENGTH_SHORT).show();

                    } else { // If the username is unique...
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(username).build();
                        Objects.requireNonNull(myAuth.getCurrentUser()).updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                mRef.child("users").child(currentUser).child("username").setValue(username);

                                if(!TextUtils.isEmpty(name)) mRef.child("users").child(currentUser).child("name").setValue(name);
                                else mRef.child("users").child(currentUser).child("name").setValue(username);

                                mRef.child("users").child(currentUser).child("birth_year").setValue(yearPicker.getValue());

                                if(radioGroup.getCheckedRadioButtonId() == R.id.female_button){
                                    mRef.child("users").child(currentUser).child("sex").setValue("female");
                                } else if(radioGroup.getCheckedRadioButtonId() == R.id.male_button){
                                    mRef.child("users").child(currentUser).child("sex").setValue("male");
                                } else {
                                    mRef.child("users").child(currentUser).child("sex").setValue("other");
                                }

                                mRef.child("users").child(currentUser).child("description")
                                        .setValue(descriptionTxt.getText().toString().trim());

                                Intent i = new Intent(FormActivity.this, HomeActivity.class);
                                startActivity(i);
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

    private void addTag(ArrayList<String> tags, String t){
        if(TextUtils.isEmpty(t)) return;

        String temp;
        int i = t.indexOf(' ');

        if(t.charAt(0) == '#'){
            if(i > 0) temp = t.substring(1, i);
            else temp = t.substring(1);
        } else {
            if(i > 0) temp = t.substring(0, i);
            else temp = t;
        }

        if(!TextUtils.isEmpty(temp)) tags.add(temp.toLowerCase());
    }

    private void setPhoto() {
        // Set the profile image
        try {
            final File localFile = File.createTempFile("images", "jpg");
            mStorageRef.child("profile_photos/" + currentUser + ".jpg")
                    .getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    mPhotoView.setImageBitmap(myBitmap);
                    mProgressBar.setVisibility(View.GONE);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    mProgressBar.setVisibility(View.GONE);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            mProgressBar.setVisibility(View.GONE);
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
