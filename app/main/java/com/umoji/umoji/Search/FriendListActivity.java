package com.umoji.umoji.Search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.umoji.umoji.Home.HomeActivity;
import com.umoji.umoji.Models.User;
import com.umoji.umoji.Profile.ProfileActivity;
import com.umoji.umoji.R;
import com.umoji.umoji.Share.CustomCameraActivity;
import com.umoji.umoji.Utils.FriendListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FriendListActivity extends AppCompatActivity {
    public static int TASK__SHOW_FOLLOWERS = 0;
    public static int TASK__SHOW_FOLLOWING = 1;

    private static final String TAG = "FriendListActivity";
    private Context mContext;

    private FirebaseAuth mAuth;
    private String user_id;
    private DatabaseReference mRef;

    private LinearLayoutManager mManager;
    private RecyclerView mRecyclerView;
    private FriendListAdapter mAdapter;
    private List<User> mFriends;
    private List<String> mUids;

    private String task_string;
    private int task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list);
        mContext = FriendListActivity.this;

        mFriends = new ArrayList<>();
        mUids = new ArrayList<>();

        TextView titleTxt = (TextView) findViewById(R.id.top_bar_title);
        titleTxt.setVisibility(View.VISIBLE);

        task_string = getIntent().getStringExtra("task");

        if (task_string.equals("followed")){
            task = TASK__SHOW_FOLLOWERS;
            titleTxt.setText("Followers");
        }
        else if (task_string.equals("follows")){
            task = TASK__SHOW_FOLLOWING;
            titleTxt.setText("Following");
        }

        mAuth = FirebaseAuth.getInstance();
        user_id = getIntent().getStringExtra("user_id");
        mRef = FirebaseDatabase.getInstance().getReference();

        mManager = new LinearLayoutManager(mContext);
        mRecyclerView = (RecyclerView) findViewById(R.id.friend_list);

        setFriends();
        setupTopView();
        setupBottomNavigationView();
    }

    private void setFriends() {
        mFriends.clear();
        mUids.clear();

        mManager = new LinearLayoutManager(mContext);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mManager);

        getList();
    }

    private void getList() {
        mRef.child(task_string).child(user_id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot: dataSnapshot.getChildren()){
                    mUids.add(singleSnapshot.getValue(String.class));
                }

                for(String s: mUids){
                    mRef.child("users").orderByChild("user_id").equalTo(s).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for(DataSnapshot singleSnapshot: dataSnapshot.getChildren()){
                                User user = new User();
                                User result = singleSnapshot.getValue(User.class);

                                if (result != null) {
                                    if (result.getUser_id() != null) user.setUser_id(result.getUser_id());
                                    if (result.getEmail() != null) user.setEmail(result.getEmail());
                                    if (result.getUsername() != null) user.setUsername(result.getUsername());
                                    if (result.getName() != null) user.setName(result.getName());
                                    if (result.getDescription() != null) user.setDescription(result.getDescription());
                                }

                                mFriends.add(user);
                            }

                            if(mFriends.size() == mUids.size()){
                                mAdapter = new FriendListAdapter(mFriends, mContext);
                                mRecyclerView.setAdapter(mAdapter);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void setupTopView() {
        ImageView backBtn = (ImageView) findViewById(R.id.top_bar_back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        findViewById(R.id.top_bar_profile_btn).setVisibility(View.GONE);
        findViewById(R.id.top_bar_messages_btn).setVisibility(View.GONE);
        findViewById(R.id.top_bar_notifications_btn).setVisibility(View.GONE);
    }

    private void setupBottomNavigationView(){
        ImageView homeBtn, homeBlueBtn, cameraBtn, cameraBlueBtn, discoverBtn, discoverBlueBtn;

        homeBtn = (ImageView) findViewById(R.id.bottom_home);
        homeBlueBtn = (ImageView) findViewById(R.id.bottom_home_blue);
        cameraBtn = (ImageView) findViewById(R.id.bottom_camera);
        cameraBlueBtn = (ImageView) findViewById(R.id.bottom_camera_blue);
        discoverBtn = (ImageView) findViewById(R.id.bottom_discover);
        discoverBlueBtn = (ImageView) findViewById(R.id.bottom_discover_blue);

        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, HomeActivity.class);
                startActivity(i);
            }
        });

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, CustomCameraActivity.class);
                i.putExtra("task", "share");
                i.putExtra("chain_id", "");
                i.putExtra("creator_id", "");
                startActivity(i);
            }
        });

        discoverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, DiscoverActivity.class);
                startActivity(intent);
            }
        });
    }

}
