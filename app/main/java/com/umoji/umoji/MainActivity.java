package com.umoji.umoji;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.umoji.umoji.Auth.SignInActivity;
import com.umoji.umoji.Auth.SignUpActivity;
import com.umoji.umoji.Home.HomeActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private Context mContext;
    private FirebaseAuth mAuth;

    private TextView signInBtn, signUpBtn;
    private NumberPicker numberPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = MainActivity.this;

        mAuth = FirebaseAuth.getInstance();

        signInBtn = (TextView)findViewById(R.id.signInButton);
        signUpBtn = (TextView)findViewById(R.id.signUpButton);

        signInBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent i = new Intent(mContext, SignInActivity.class);
                startActivity(i);
            }
        });

        signUpBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent i = new Intent(mContext, SignUpActivity.class);
                startActivity(i);
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
}
