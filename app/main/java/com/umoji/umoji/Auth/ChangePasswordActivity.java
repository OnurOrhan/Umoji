package com.umoji.umoji.Auth;

import com.umoji.umoji.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class ChangePasswordActivity extends AppCompatActivity {
    private Context mContext = ChangePasswordActivity.this;
    private FirebaseAuth mAuth;

    private Button changePassBtn;
    private EditText oldPass, newPass, repeatPass;
    private ImageView backArrow;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        mAuth = FirebaseAuth.getInstance();

        oldPass = (EditText) findViewById(R.id.oldPass);
        newPass = (EditText) findViewById(R.id.newPass);
        repeatPass = (EditText) findViewById(R.id.newPass2);
        changePassBtn = (Button) findViewById(R.id.changePassButton);
        changePassBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryUpdatePass();
            }
        });

        backArrow = (ImageView)findViewById(R.id.changePassBackArrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.changePassProgress);
        progressBar.getIndeterminateDrawable().setColorFilter(mContext.getResources().getColor(R.color.progressBarColor), PorterDuff.Mode.SRC_IN);
    }

    private void tryUpdatePass(){
        final String oldp = oldPass.getText().toString();
        final String newp = newPass.getText().toString();
        String repeat = repeatPass.getText().toString();

        if(TextUtils.isEmpty(oldp) || TextUtils.isEmpty(newp) || TextUtils.isEmpty(repeat)){
            Toast.makeText(mContext, "Please fill out all the fields.",
                    Toast.LENGTH_SHORT).show();
        } else if (newp.length() < 6){
            Toast.makeText(mContext, "Please select a password longer than 6 characters.",
                    Toast.LENGTH_SHORT).show();
        } else if (!newp.equals(repeat)){
            Toast.makeText(mContext, "Please enter the same password twice.",
                    Toast.LENGTH_SHORT).show();
        } else {
            progressBar.setVisibility(View.VISIBLE);

            AuthCredential credential = EmailAuthProvider
                .getCredential(mAuth.getCurrentUser().getEmail(), "password");
            mAuth.getCurrentUser().reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mAuth.getCurrentUser().updatePassword(newp)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    progressBar.setVisibility(View.GONE);
                                }
                            });

                        Toast.makeText(mContext, "Updated password.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
        }

    }
}
