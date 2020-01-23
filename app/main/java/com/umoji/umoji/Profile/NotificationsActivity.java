package com.umoji.umoji.Profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.umoji.umoji.Chat.ChatListActivity;
import com.umoji.umoji.Home.HomeActivity;
import com.umoji.umoji.Models.Notification;
import com.umoji.umoji.R;
import com.umoji.umoji.Search.DiscoverActivity;
import com.umoji.umoji.Share.CustomCameraActivity;
import com.umoji.umoji.Utils.NotificationListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class NotificationsActivity extends AppCompatActivity {
    private static final String TAG = "NotificationsActivity";
    private FirebaseAuth mAuth;
    private String currentUser;

    private ArrayList<Notification> mNotifications;
    private ListView mListView;
    private NotificationListAdapter mAdapter;

    private ImageView backBtn, profileBtn, msgBtn, notifBtn;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        mContext = NotificationsActivity.this;

        mAuth = FirebaseAuth.getInstance();
        mNotifications = new ArrayList<>();
        mListView = (ListView) findViewById(R.id.notification_list);

        backBtn = (ImageView) findViewById(R.id.top_bar_back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        profileBtn = (ImageView) findViewById(R.id.top_bar_profile_btn);
        msgBtn = (ImageView) findViewById(R.id.top_bar_messages_btn);
        notifBtn = (ImageView) findViewById(R.id.top_bar_notifications_btn);

        profileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, ProfileActivity.class);
                i.putExtra("user_id", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
                startActivity(i);
            }
        });

        msgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, ChatListActivity.class);
                startActivity(i);
            }
        });

        notifBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, NotificationsActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
        });

        TextView titleTxt = (TextView) findViewById(R.id.top_bar_title);
        titleTxt.setVisibility(View.VISIBLE);
        titleTxt.setText("Notifications");

        setupBottomNavigationView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null){
            currentUser = user.getUid();

            mNotifications.clear();
            getNotifications();
        }
    }

    private void getNotifications() {
        Log.d(TAG, "getNotifications: getting notifications");

        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child("notifications").child(currentUser);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    Notification not = new Notification();
                    Notification temp = singleSnapshot.getValue(Notification.class);

                    not.setNot_id(temp.getNot_id());
                    not.setType(temp.getType());
                    not.setSeen(temp.getSeen());
                    not.setUser1(temp.getUser1());
                    not.setUser2(temp.getUser2());
                    not.setKey1(temp.getKey1());
                    not.setKey2(temp.getKey2());
                    not.setDate_created(temp.getDate_created());

                    mNotifications.add(not);
                } displayNotifications();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void displayNotifications() {
        if(mNotifications != null){
            Collections.sort(mNotifications, new Comparator<Notification>() {
                @Override
                public int compare(Notification o1, Notification o2) {
                    return (int)(o2.getDate_created() - o1.getDate_created());
                }
            });

            mAdapter = new NotificationListAdapter(NotificationsActivity.this, R.layout.layout_notification_listitem, mNotifications);
            mListView.setAdapter(mAdapter);
        }
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
