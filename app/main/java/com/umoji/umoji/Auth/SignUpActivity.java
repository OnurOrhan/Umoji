package com.umoji.umoji.Auth;

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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.umoji.umoji.Forms.FormActivity;
import com.umoji.umoji.Home.HomeActivity;
import com.umoji.umoji.Models.User;
import com.umoji.umoji.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

public class SignUpActivity extends AppCompatActivity {
    private Context mContext;
    private FirebaseAuth mAuth;
    private DatabaseReference myRef;

    private RelativeLayout signUpBtn;
    private EditText emailTxt, passTxt, repeatTxt;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        mContext = SignUpActivity.this;

        mAuth = FirebaseAuth.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference();

        emailTxt = (EditText)findViewById(R.id.editEmail);
        passTxt = (EditText)findViewById(R.id.editPass);
        repeatTxt = (EditText)findViewById(R.id.editRepeat);
        progressBar = (ProgressBar)findViewById(R.id.signUpProgress);
        progressBar.getIndeterminateDrawable().setColorFilter(mContext.getResources().getColor(R.color.progressBarColor), PorterDuff.Mode.SRC_IN);
        progressBar.setVisibility(View.GONE);

        signUpBtn = (RelativeLayout) findViewById(R.id.signup_btn_layout);
        signUpBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                createAccount();
            }
        });

        ImageView backButton = (ImageView) findViewById(R.id.signUpBackArrow);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onStart(){
        super.onStart();

        if (mAuth.getCurrentUser() != null){
            Intent i = new Intent(mContext, HomeActivity.class);
            startActivity(i);
        }
    }

    public void createAccount(){
        final String email = emailTxt.getText().toString();
        final String password = passTxt.getText().toString();
        String repeat = repeatTxt.getText().toString();

        if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(repeat)){
            Toast.makeText(mContext, "Please fill out all the fields.",
                    Toast.LENGTH_SHORT).show();
        } else if (password.length() < 6){
            Toast.makeText(mContext, "Please select a password longer than 6 characters.",
                    Toast.LENGTH_SHORT).show();
        } else if (!password.equals(repeat)){
            Toast.makeText(mContext, "Please enter the same password twice.",
                    Toast.LENGTH_SHORT).show();
        } else {
            myRef.child("users").orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                 @Override
                 public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.getChildrenCount() != 0){
                        Toast.makeText(mContext, "Email already registered!",
                                Toast.LENGTH_SHORT).show();

                    } else { // If email is unique
                        progressBar.setVisibility(View.VISIBLE);
                        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                    final String username = emailToUsername(email);
                                    final String userId = Objects.requireNonNull(user).getUid();

                                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder().setDisplayName(username).build();
                                    user.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            User dbUser = new User(userId, email);
                                            myRef.child("users").child(userId).setValue(dbUser);

                                            progressBar.setVisibility(View.GONE);

                                            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_white_profile);
                                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                            byte[] data = baos.toByteArray();

                                            final Intent i = new Intent(mContext, FormActivity.class);
                                            i.putExtra("username", username);

                                            FirebaseStorage.getInstance().getReference().child("profile_photos/" + userId + ".jpg").putBytes(data)
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception exception) {
                                                            startActivity(i);
                                                        }
                                                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                @Override
                                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                    startActivity(i);
                                                }
                                            });
                                        }
                                    });

                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(mContext, "Could not create user.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                 }

                 @Override
                 public void onCancelled(@NonNull DatabaseError databaseError) {

                 }
             });
        }
    }

    public String emailToUsername(String email){
        int index = email.indexOf('@');
        return email.substring(0,index);
    }
}
