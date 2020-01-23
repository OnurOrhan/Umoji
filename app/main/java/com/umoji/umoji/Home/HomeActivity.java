package com.umoji.umoji.Home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.umoji.umoji.Chat.ChatListActivity;
import com.umoji.umoji.MainActivity;
import com.umoji.umoji.Models.Chain;
import com.umoji.umoji.Profile.NotificationsActivity;
import com.umoji.umoji.Profile.ProfileActivity;
import com.umoji.umoji.R;
import com.umoji.umoji.Search.DiscoverActivity;
import com.umoji.umoji.Share.CustomCameraActivity;
import com.umoji.umoji.Utils.MainfeedListAdapter;
import com.umoji.umoji.Utils.PersonCircleListAdapter;
import com.umoji.umoji.Utils.UniversalImageLoader;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.one.EmojiOneProvider;

import java.util.ArrayList;
import java.util.Objects;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private Context mContext;
    private static final int STORY_PERSON_LIMIT = 20;
    private static final int LOAD_RATE = 7;

    private boolean isLoading;
    private long firstVideoDate, lastVideoDate;

    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private DatabaseReference mRef;
    private String user_id;

    private LinearLayoutManager storyPersonManager;
    private ArrayList<String> mStoryPersons;
    private PersonCircleListAdapter storyPersonListAdapter;
    private RecyclerView storyPersonRecyclerView;

    private ArrayList<Chain> mChains;
    private ArrayList<ArrayList<String>> mTags;
    private ArrayList<Boolean> mWatched;
    private LinearLayoutManager mLinearLayout;
    private RecyclerView mRecyclerView;
    private MainfeedListAdapter mAdapter;

    private ImageView profileBtn;
    private ImageView messagesBtn;
    private ImageView notifBtn;
    private ImageView bubbleImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Log.d(TAG, "onCreate: creating...");
        mContext = HomeActivity.this;

        EmojiManager.install(new EmojiOneProvider());

        mAuth = FirebaseAuth.getInstance();
        initImageLoader();
        setupBottomNavigationView();

        mRef = FirebaseDatabase.getInstance().getReference();

        ImageView backBtn = (ImageView) findViewById(R.id.top_bar_back_btn);
        backBtn.setVisibility(View.GONE);

        mStoryPersons = new ArrayList<>();
        storyPersonRecyclerView = (RecyclerView) findViewById(R.id.home_stories_view);

        mLinearLayout = new LinearLayoutManager(mContext);
        mRecyclerView = (RecyclerView) findViewById(R.id.feedListView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLinearLayout);
        mRecyclerView.setItemViewCacheSize(LOAD_RATE);
        isLoading = false;

        mChains = new ArrayList<>();
        mTags = new ArrayList<>();
        mWatched = new ArrayList<>();
        //mFollowing = new ArrayList<>();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null){
            user = currentUser;
            user_id = currentUser.getUid();

            setStoryTags();

            //getFollowing();
            firstVideoDate = 0;
            lastVideoDate = 0;
            getChains();
            setRecyclerScroll();

        } else {
            Intent i = new Intent(mContext, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        }

        profileBtn = (ImageView) findViewById(R.id.top_bar_profile_btn);
        messagesBtn = (ImageView) findViewById(R.id.top_bar_messages_btn);
        notifBtn = (ImageView) findViewById(R.id.top_bar_notifications_btn);
        bubbleImg = (ImageView) findViewById(R.id.top_bar_notifications_bubble);

        profileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, ProfileActivity.class);
                i.putExtra("user_id", Objects.requireNonNull(mAuth.getCurrentUser()).getUid());
                startActivity(i);
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

        mRef.child("notifications").child(user_id).orderByChild("seen").equalTo(false)
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

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: started...");
        bubbleImg.setVisibility(View.GONE);
    }

    private void initImageLoader(){
        UniversalImageLoader universalImageLoader = new UniversalImageLoader(mContext);
        ImageLoader.getInstance().init(universalImageLoader.getConfig());
    }

    private void setStoryTags() {
        mStoryPersons.clear();

        storyPersonManager = new LinearLayoutManager(mContext);
        storyPersonManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        storyPersonRecyclerView.setHasFixedSize(true);
        storyPersonRecyclerView.setLayoutManager(storyPersonManager);

        getStoryTags();
    }

    private void getStoryTags() {
        mRef.child("user_stories").limitToLast(STORY_PERSON_LIMIT).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for(DataSnapshot d : dataSnapshot.getChildren()) {
                        mStoryPersons.add(d.getKey());
                    }
                }

                storyPersonListAdapter = new PersonCircleListAdapter(mStoryPersons, mContext);
                storyPersonRecyclerView.setAdapter(storyPersonListAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getChains(){
        Log.d(TAG, "getChains: getting videos");

        isLoading = true;

        mRef.child("chains").orderByChild("date_created").limitToLast(LOAD_RATE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long count = 0;

                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    Chain chain = new Chain();
                    Chain temp = singleSnapshot.getValue(Chain.class);

                    chain.setChain_id(Objects.requireNonNull(temp).getChain_id());
                    chain.setFirst_video(temp.getFirst_video());
                    chain.setFirst_user(temp.getFirst_user());
                    long temp_date = temp.getDate_created();
                    chain.setDate_created(temp_date);
                    chain.setVideo_uri(temp.getVideo_uri());
                    chain.setViews(temp.getViews());
                    chain.setLikes(temp.getLikes());
                    chain.setTitle(temp.getTitle());
                    chain.setResponses(temp.getResponses());

                    ArrayList<String> tagList = new ArrayList<>();
                    for(DataSnapshot tagSnap : singleSnapshot.child("tags").getChildren()){
                        tagList.add(tagSnap.getValue(String.class));
                    }

                    mChains.add(0, chain);
                    mTags.add(0, tagList);

                    count++;
                    if (count == 1){
                        lastVideoDate = temp_date;
                        firstVideoDate = temp_date;
                    } else if (temp_date < firstVideoDate) firstVideoDate = temp_date;
                    else if (temp_date > lastVideoDate) lastVideoDate = temp_date;
                }

                for(int i = 0; i < count; i++){
                    mWatched.add(0, Boolean.FALSE);
                }

                displayChains();
                isLoading = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                isLoading = false;
            }
        });
    }

    private void displayChains(){
        if(mChains != null){
            mAdapter = new MainfeedListAdapter(mChains, mTags, mWatched, mContext);
            mRecyclerView.setAdapter(mAdapter);
        }
    }

    private void setRecyclerScroll() {
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            int visibleItemCount, totalItemCount, firstVisibleItemPosition, lastVisibleItem;

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

                visibleItemCount = layoutManager.getChildCount();
                totalItemCount = layoutManager.getItemCount();
                int tempFirst = layoutManager.findFirstVisibleItemPosition();

                if (tempFirst < firstVisibleItemPosition){
                    mAdapter.reloadVideo((MainfeedListAdapter.MainfeedViewHolder) recyclerView.findViewHolderForLayoutPosition(tempFirst));
                } else if (tempFirst + visibleItemCount - 1 > lastVisibleItem){
                    mAdapter.reloadVideo((MainfeedListAdapter.MainfeedViewHolder) recyclerView.findViewHolderForLayoutPosition(tempFirst + visibleItemCount - 1));
                }

                firstVisibleItemPosition = tempFirst;
                lastVisibleItem = firstVisibleItemPosition + visibleItemCount - 1;

                if (!isLoading) {
                    if (lastVisibleItem == mChains.size() - 1) {
                        isLoading = true;

                        mRef.child("chains").orderByChild("date_created").endAt(firstVideoDate - 1)
                                .limitToLast(LOAD_RATE).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        if(dataSnapshot.getChildrenCount() > 0) {
                                            long count = 0;

                                            for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                                                Chain chain = new Chain();
                                                Chain temp = singleSnapshot.getValue(Chain.class);

                                                chain.setChain_id(Objects.requireNonNull(temp).getChain_id());
                                                chain.setFirst_video(temp.getFirst_video());
                                                chain.setFirst_user(temp.getFirst_user());
                                                long temp_date = temp.getDate_created();
                                                chain.setDate_created(temp_date);
                                                chain.setVideo_uri(temp.getVideo_uri());
                                                chain.setViews(temp.getViews());
                                                chain.setLikes(temp.getLikes());
                                                chain.setTitle(temp.getTitle());
                                                chain.setResponses(temp.getResponses());

                                                ArrayList<String> tagList = new ArrayList<>();
                                                for (DataSnapshot tagSnap : singleSnapshot.child("tags").getChildren()) {
                                                    tagList.add(tagSnap.getValue(String.class));
                                                }

                                                count++;
                                                if (temp_date < firstVideoDate)
                                                    firstVideoDate = temp_date;
                                                if (temp_date > lastVideoDate)
                                                    lastVideoDate = temp_date;

                                                mChains.add(chain);
                                                mTags.add(tagList);
                                            }

                                            for (int i = 0; i < count; i++) {
                                                mWatched.add(Boolean.FALSE);
                                            }

                                            mAdapter.notifyDataSetChanged();
                                            // mRecyclerView.scrollToPosition(mChains.size() - (int)count - 1);
                                        }

                                        isLoading = false;
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        isLoading = false;
                                    }
                                });
                    }
                }
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

        homeBtn.setVisibility(View.GONE);
        homeBlueBtn.setVisibility(View.VISIBLE);

        homeBlueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, HomeActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
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
