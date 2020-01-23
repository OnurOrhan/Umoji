package com.umoji.umoji.Profile;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.makeramen.roundedimageview.RoundedImageView;
import com.umoji.umoji.Chat.ChatActivity;
import com.umoji.umoji.Chat.ChatListActivity;
import com.umoji.umoji.Home.HomeActivity;
import com.umoji.umoji.MainActivity;
import com.umoji.umoji.Models.Chain;
import com.umoji.umoji.Models.Notification;
import com.umoji.umoji.Models.User;
import com.umoji.umoji.R;
import com.umoji.umoji.Search.DiscoverActivity;
import com.umoji.umoji.Search.FriendListActivity;
import com.umoji.umoji.Share.CustomCameraActivity;
import com.umoji.umoji.Utils.GridChainAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {
    private static final String TAG = "ProfileActivity";
    private static final int NUM_GRID_COLUMNS = 3;

    private Context mContext = ProfileActivity.this;

    private TextView username, displayName, description;
    private TextView videoCountTxt, followerCountTxt, followingCountTxt;
    private long videoCount, followerCount, followingCount;

    private RoundedImageView profilePhoto;

    private String shownUser, currentUser;
    private User user;
    private Boolean isSelf;

    private DatabaseReference myRef;
    private StorageReference storageReference;

    private ImageView editProfileBtn;
    private ImageView followBtn, unfollowBtn;
    // private Button friendBtn, removeReqBtn, unfriendBtn;

    // private LinearLayout acceptDecline;
    // private Button acceptBtn, declineBtn;

    private ImageView profileBtn, messagesBtn, notifBtn, bubbleImg, startChat;

    private GridView mGridView;
    private GridChainAdapter mGridChainAdapter;
    private ArrayList<Chain> mChains;
    private ArrayList<ArrayList<String>> mChainTags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Log.d(TAG, "onCreate: created...");

        isSelf = false;
        myRef = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        ImageView backBtn = (ImageView) findViewById(R.id.top_bar_back_btn);
        backBtn.setVisibility(View.GONE);

        profileBtn = (ImageView) findViewById(R.id.top_bar_profile_btn);
        messagesBtn = (ImageView) findViewById(R.id.top_bar_messages_btn);
        notifBtn = (ImageView) findViewById(R.id.top_bar_notifications_btn);
        bubbleImg = (ImageView) findViewById(R.id.top_bar_notifications_bubble);

        username = (TextView) findViewById(R.id.top_bar_title);
        username.setVisibility(View.VISIBLE);
        username.setTextColor(getResources().getColor(R.color.secondary_variant));

        displayName = (TextView) findViewById(R.id.profileDisplayName);
        description = (TextView) findViewById(R.id.profileDescription);

        videoCountTxt = (TextView) findViewById(R.id.profile_num_posts);
        followerCountTxt = (TextView) findViewById(R.id.profile_num_followers);
        followingCountTxt = (TextView) findViewById(R.id.profile_num_following);

        LinearLayout followerLayout, followingLayout;
        followerLayout = (LinearLayout) findViewById(R.id.profile_followers_layout);
        followingLayout = (LinearLayout) findViewById(R.id.profile_following_layout);

        followerLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, FriendListActivity.class);
                i.putExtra("task", "followed");
                i.putExtra("user_id", shownUser);
                startActivity(i);
            }
        });

        followingLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, FriendListActivity.class);
                i.putExtra("task", "follows");
                i.putExtra("user_id", shownUser);
                startActivity(i);
            }
        });

        editProfileBtn = (ImageView) findViewById(R.id.edit_profile_button);
        startChat = (ImageView) findViewById(R.id.profileStartChat);

        followBtn = (ImageView) findViewById(R.id.follow_button);
        unfollowBtn = (ImageView) findViewById(R.id.unfollow_button);

        /* friendBtn = (Button) findViewById(R.id.add_friend_button);
        removeReqBtn = (Button) findViewById(R.id.remove_request_button);
        unfriendBtn = (Button) findViewById(R.id.remove_friend_button);

        acceptDecline = (LinearLayout) findViewById(R.id.friend_accept_decline);
        acceptBtn = (Button) findViewById(R.id.accept_request_button);
        declineBtn = (Button) findViewById(R.id.decline_request_button); */

        Intent intent = getIntent();
        shownUser = intent.getStringExtra("user_id");
        FirebaseUser temp = FirebaseAuth.getInstance().getCurrentUser();

        if(temp == null){
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            i.putExtra("EXIT", true);
            startActivity(i);
        } else {
            currentUser = temp.getUid();
            isSelf = shownUser.equals(temp.getUid());

            setupToolbar();
            setupBottomNavigationView();
            user = new User();

            String uid = "";
            if (isSelf) uid = currentUser;
            else uid = shownUser;

            myRef.child("users").orderByChild("user_id").equalTo(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for(DataSnapshot singleSnapshot :  dataSnapshot.getChildren()){
                        User result = singleSnapshot.getValue(User.class);

                        user.setUser_id(result.getUser_id());
                        user.setEmail(result.getEmail());
                        user.setUsername(result.getUsername());
                        user.setName(result.getName());
                        user.setDescription(result.getDescription());

                        setGrid();

                        setupActivityWidgets();
                        username.setText(user.getUsername());

                        if(TextUtils.isEmpty(user.getName()))
                            displayName.setText(user.getUsername());
                        else displayName.setText(user.getName());

                        if(TextUtils.isEmpty(user.getDescription()) || user.getDescription().equals("Description"))
                            description.setVisibility(View.GONE);
                        else description.setText(user.getDescription());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.d(TAG, "onCancelled: " + databaseError.toString());
                }
            });

            countFields();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupButtons();
        setProfilePhoto();
        bubbleImg.setVisibility(View.GONE);
    }

    private void countFields() {
        countFollowers();
        countFollowing();
    }

    private void countFollowers() {
        Query followerQ = myRef.child("followed").child(shownUser);
        followerQ.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                followerCount = dataSnapshot.getChildrenCount();
                followerCountTxt.setText(String.valueOf(followerCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void countFollowing() {
        Query followingQ = myRef.child("follows").child(shownUser);
        followingQ.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                followingCount = dataSnapshot.getChildrenCount();
                followingCountTxt.setText(String.valueOf(followingCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void setProfilePhoto() {
        try {
            final File localFile = File.createTempFile("images", "jpg");
            storageReference.child("profile_photos/" + user.getUser_id() + ".jpg")
                    .getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    profilePhoto.setImageBitmap(myBitmap);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupButtons() {
        if (isSelf){
            editProfileBtn.setVisibility(View.VISIBLE);
            editProfileBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(mContext, ShowPhotoActivity.class);
                    i.putExtra("user_id", shownUser);
                    i.putExtra("username", user.getUsername());
                    startActivity(i);
                }
            });
        }

        else { // Shown user is not the current user
            editProfileBtn.setVisibility(View.GONE);

            myRef.child("follows").child(currentUser).child(shownUser)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()){
                        followBtn.setVisibility(View.VISIBLE);
                        unfollowBtn.setVisibility(View.GONE);
                    } else {
                        followBtn.setVisibility(View.GONE);
                        unfollowBtn.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

            /* Query friendq = myRef.child("friends").child(currentUser).child(shownUser);
            friendq.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (!dataSnapshot.exists()){ // The shown user is not friend
                        Query rq = myRef.child("requests").child(currentUser).child(shownUser);
                        rq.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (!dataSnapshot.exists()){ // No request from shown user
                                    Query requestq = myRef.child("requests").child(shownUser).child(currentUser);
                                    requestq.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            acceptDecline.setVisibility(View.GONE);
                                            if (!dataSnapshot.exists()){ // No friend request sent
                                                friendBtn.setVisibility(View.VISIBLE);
                                                unfriendBtn.setVisibility(View.GONE);
                                                removeReqBtn.setVisibility(View.GONE);
                                            } else { // There is a friend request sent to the shown user
                                                friendBtn.setVisibility(View.GONE);
                                                unfriendBtn.setVisibility(View.GONE);
                                                removeReqBtn.setVisibility(View.VISIBLE);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                } else { // There is a friend request from the shown user
                                    acceptDecline.setVisibility(View.VISIBLE);
                                    friendBtn.setVisibility(View.GONE);
                                    unfriendBtn.setVisibility(View.GONE);
                                    removeReqBtn.setVisibility(View.GONE);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                    } else { // Is a friend
                        acceptDecline.setVisibility(View.GONE);
                        friendBtn.setVisibility(View.GONE);
                        unfriendBtn.setVisibility(View.VISIBLE);
                        removeReqBtn.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            }); */

            setupFollowBtn();
            setupUnfollowBtn();

            /* setupFriendBtn();
            setupRemoveReqBtn();
            setupUnfriendBtn();

            setupAcceptBtn();
            setupDeclineBtn(); */
        }
    }

    /* private void setupAcceptBtn() {
        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                acceptBtn.setEnabled(false);
                myRef.child("friends").child(currentUser).child(shownUser).setValue(shownUser);
                myRef.child("friends").child(shownUser).child(currentUser).setValue(currentUser);
                myRef.child("requests").child(currentUser).child(shownUser).removeValue();

                String notifKey = myRef.child("notifications").child(shownUser).push().getKey();
                Notification notif = new Notification(4, notifKey, currentUser, shownUser);
                notif.setKey1("none");
                notif.setKey2("none");
                notif.setDate_created(Calendar.getInstance().getTimeInMillis());

                myRef.child("notifications").child(shownUser).child(notifKey).setValue(notif);

                acceptDecline.setVisibility(View.GONE);
                unfriendBtn.setVisibility(View.VISIBLE);
                unfriendBtn.setEnabled(true);
            }
        });
    }

    private void setupDeclineBtn() {
        declineBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                declineBtn.setEnabled(false);
                myRef.child("requests").child(currentUser).child(shownUser).removeValue();
                acceptDecline.setVisibility(View.GONE);
                friendBtn.setVisibility(View.VISIBLE);
                friendBtn.setEnabled(true);
            }
        });
    }

    private void setupFriendBtn() {
        friendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                friendBtn.setEnabled(false);
                myRef.child("requests").child(shownUser).child(currentUser).setValue(currentUser);

                String notifKey = myRef.child("notifications").child(shownUser).push().getKey();
                Notification notif = new Notification(3, notifKey, currentUser, shownUser);
                notif.setKey1("none");
                notif.setKey2("none");
                notif.setDate_created(Calendar.getInstance().getTimeInMillis());

                myRef.child("notifications").child(shownUser).child(notifKey).setValue(notif);

                friendBtn.setVisibility(View.GONE);
                removeReqBtn.setVisibility(View.VISIBLE);
                removeReqBtn.setEnabled(true);
            }
        });
    }

    private void setupRemoveReqBtn() {
        removeReqBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeReqBtn.setEnabled(false);
                myRef.child("requests").child(shownUser).child(currentUser).removeValue();
                removeReqBtn.setVisibility(View.GONE);
                friendBtn.setVisibility(View.VISIBLE);
                friendBtn.setEnabled(true);
            }
        });
    }

    private void setupUnfriendBtn() {
        unfriendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unfriendBtn.setEnabled(false);
                myRef.child("friends").child(currentUser).child(shownUser).removeValue();
                myRef.child("friends").child(shownUser).child(currentUser).removeValue();
                unfriendBtn.setVisibility(View.GONE);
                friendBtn.setVisibility(View.VISIBLE);
                friendBtn.setEnabled(true);
            }
        });
    } */

    private void setupFollowBtn() {
        followBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                followBtn.setEnabled(false);
                myRef.child("follows").child(currentUser).child(shownUser).setValue(shownUser);
                myRef.child("followed").child(shownUser).child(currentUser).setValue(currentUser);

                countFollowers();

                String notifKey = myRef.child("notifications").child(shownUser).push().getKey();
                Notification notif = new Notification(5, notifKey, currentUser, shownUser);
                notif.setKey1("none");
                notif.setKey2("none");
                notif.setDate_created(Calendar.getInstance().getTimeInMillis());

                myRef.child("notifications").child(shownUser).child(Objects.requireNonNull(notifKey)).setValue(notif);

                followBtn.setVisibility(View.GONE);
                unfollowBtn.setVisibility(View.VISIBLE);
                unfollowBtn.setEnabled(true);
            }
        });
    }

    private void setupUnfollowBtn() {
        unfollowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unfollowBtn.setEnabled(false);
                myRef.child("follows").child(currentUser).child(shownUser).removeValue();
                myRef.child("followed").child(shownUser).child(currentUser).removeValue();

                countFollowers();

                unfollowBtn.setVisibility(View.GONE);
                followBtn.setVisibility(View.VISIBLE);
                followBtn.setEnabled(true);
            }
        });
    }

    private void setupToolbar(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.profileToolBar);
        setSupportActionBar(toolbar);

        ImageView profileMenu = (ImageView) findViewById(R.id.profileMenu);
        if(!isSelf){
            profileMenu.setEnabled(false);
            profileMenu.setVisibility(View.GONE);
        } else {
            profileMenu.setEnabled(true);
            profileMenu.setVisibility(View.VISIBLE);
            profileMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: navigating to account settings.");
                    Intent intent = new Intent(ProfileActivity.this, AccountSettingsActivity.class);
                    startActivity(intent);
                }
            });
        }

        if(isSelf) startChat.setVisibility(View.GONE);
        else startChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, ChatActivity.class);
                i.putExtra("user_id", shownUser);
                i.putExtra("username", user.getUsername());
                mContext.startActivity(i);
            }
        });
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
            public void onClick(View view) {
                Intent intent = new Intent(mContext, DiscoverActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setGrid(){
        mGridView = (GridView) findViewById(R.id.profile_video_list);
        mChains = new ArrayList<>();
        mChainTags = new ArrayList<>();

        myRef.child("user_chains").child(shownUser)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    videoCount = dataSnapshot.getChildrenCount();
                    videoCountTxt.setText(String.valueOf(videoCount));

                    for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                        Chain chain = new Chain();
                        Chain temp = singleSnapshot.getValue(Chain.class);

                        chain.setChain_id(Objects.requireNonNull(temp).getChain_id());
                        chain.setFirst_video(temp.getFirst_video());
                        chain.setFirst_user(temp.getFirst_user());
                        chain.setDate_created(temp.getDate_created());
                        chain.setVideo_uri(temp.getVideo_uri());
                        chain.setViews(temp.getViews());
                        chain.setLikes(temp.getLikes());
                        chain.setTitle(temp.getTitle());
                        chain.setResponses(temp.getResponses());

                        ArrayList<String> tagList = new ArrayList<>();
                        for (DataSnapshot tagSnap : singleSnapshot.child("tags").getChildren()) {
                            tagList.add(tagSnap.getValue(String.class));
                        }

                        mChains.add(0, chain);
                        mChainTags.add(0, tagList);
                    }

                    mGridChainAdapter = new GridChainAdapter(mContext, R.layout.layout_grid_chainview, mChains, mChainTags);
                    mGridView.setAdapter(mGridChainAdapter);

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    videoCountTxt.setText("0");
                }
            });
    }

    private void setupActivityWidgets(){
        //mProgressBar = (ProgressBar) findViewById(R.id.profileProgressBar);
        //mProgressBar.setVisibility(View.GONE);
        profilePhoto = (RoundedImageView) findViewById(R.id.profile_photo);
        profilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, CustomCameraActivity.class);
                i.putExtra("task", "story");
                i.putExtra("chain_id", "");
                i.putExtra("creator_id", "");
                startActivity(i);
            }
        });

        setProfilePhoto();

        profileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, ProfileActivity.class);
                i.putExtra("user_id", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
        });

        messagesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, ChatListActivity.class);
                startActivity(i);
            }
        });

        notifBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, NotificationsActivity.class);
                startActivity(intent);
            }
        });

        myRef.child("notifications").child(currentUser).orderByChild("seen").equalTo(false)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        bubbleImg.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

}
