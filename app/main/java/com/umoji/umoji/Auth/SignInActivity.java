package com.umoji.umoji.Auth;

import android.content.Intent;
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
import android.widget.TextView;
import android.widget.Toast;

import com.umoji.umoji.Home.HomeActivity;
import com.umoji.umoji.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignInActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private RelativeLayout signInBtn;
    private EditText emailTxt, passTxt;
    private TextView forgotPass;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        mAuth = FirebaseAuth.getInstance();

        emailTxt = (EditText)findViewById(R.id.editEmail);
        passTxt = (EditText)findViewById(R.id.editPass);
        forgotPass = (TextView)findViewById(R.id.forgotPass);
        forgotPass.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent i = new Intent(SignInActivity.this, ResetPasswordActivity.class);
                startActivity(i);
            }
        });

        progressBar = (ProgressBar)findViewById(R.id.signInProgress);
        progressBar.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.progressBarColor), PorterDuff.Mode.SRC_IN);
        progressBar.setVisibility(View.GONE);

        signInBtn = (RelativeLayout) findViewById(R.id.signin_btn_layout);
        signInBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                signIn();
            }
        });

        ImageView backButton = (ImageView) findViewById(R.id.signInBackArrow);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    public void signIn(){
        String email = emailTxt.getText().toString();
        String password = passTxt.getText().toString();

        if(TextUtils.isEmpty(email) || TextUtils.isEmpty(password)){
            Toast.makeText(SignInActivity.this, "Please fill out all the fields.",
                    Toast.LENGTH_SHORT).show();
        } else {
            progressBar.setVisibility(View.VISIBLE);
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(SignInActivity.this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        Intent i = new Intent(SignInActivity.this, HomeActivity.class);
                        startActivity(i);
                    } else {
                        Toast.makeText(SignInActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    @Override
    public void onStart(){
        super.onStart();

        if (mAuth.getCurrentUser() != null){
            Intent i = new Intent(SignInActivity.this, HomeActivity.class);
            startActivity(i);
        }
    }

}
