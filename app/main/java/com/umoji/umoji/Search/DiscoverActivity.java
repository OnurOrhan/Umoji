package com.umoji.umoji.Search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.umoji.umoji.Home.HomeActivity;
import com.umoji.umoji.Models.Chain;
import com.umoji.umoji.Profile.ProfileActivity;
import com.umoji.umoji.R;
import com.umoji.umoji.Share.CustomCameraActivity;
import com.umoji.umoji.Utils.GridChainAdapter;
import com.umoji.umoji.Utils.TagListAdapter;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.one.EmojiOneProvider;

import java.util.ArrayList;
import java.util.Objects;

public class DiscoverActivity extends AppCompatActivity {
    private static final String TAG = "DiscoverActivity";
    private Context mContext = DiscoverActivity.this;
    private static final int TAG_LIMIT = 20;

    private RelativeLayout relativeLayout;
    private ImageView searchImg;

    private FirebaseAuth mAuth;
    private DatabaseReference mRef;
    private String currentUser;

    private LinearLayoutManager storyTagManager;
    private ArrayList<String> mStoryTags;
    private TagListAdapter storyTagListAdapter;
    private RecyclerView storyTagRecyclerView;

    private ArrayList<Chain> mChains;
    private ArrayList<ArrayList<String>> mChainTags;
    private GridChainAdapter chainAdapter;
    private GridView chainGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover);
        Log.d(TAG, "onCreate: creating...");

        EmojiManager.install(new EmojiOneProvider());

        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseDatabase.getInstance().getReference();
        currentUser = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        mStoryTags = new ArrayList<>();
        storyTagRecyclerView = (RecyclerView) findViewById(R.id.discover_tags_view);

        mChains = new ArrayList<>();
        mChainTags = new ArrayList<>();
        chainGridView = (GridView) findViewById(R.id.discover_grid_view);

        setStoryTags();
        getChains();

        setTopView();
        setupBottomNavigationView();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: started...");

    }

    private void setStoryTags() {
        mStoryTags.clear();

        storyTagManager = new LinearLayoutManager(mContext);
        storyTagManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        storyTagRecyclerView.setHasFixedSize(true);
        storyTagRecyclerView.setLayoutManager(storyTagManager);

        getStoryTags();
    }

    private void getStoryTags() {
        mRef.child("story_tags").limitToLast(TAG_LIMIT).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for(DataSnapshot d : dataSnapshot.getChildren()) {
                        mStoryTags.add(Objects.requireNonNull(d.getValue()).toString());
                    }
                }

                storyTagListAdapter = new TagListAdapter(mStoryTags, mContext);
                storyTagRecyclerView.setAdapter(storyTagListAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getChains() {
        mRef.child("chains").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot: dataSnapshot.getChildren()){
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
                    for(DataSnapshot tagSnap : singleSnapshot.child("tags").getChildren()){
                        tagList.add(tagSnap.getValue(String.class));
                    }

                    mChains.add(0, chain);
                    mChainTags.add(0, tagList);
                }

                chainAdapter = new GridChainAdapter(mContext, R.layout.layout_grid_chainview, mChains, mChainTags);
                chainGridView.setAdapter(chainAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void setTopView() {
        ImageView backBtn = (ImageView) findViewById(R.id.discover_back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        relativeLayout = (RelativeLayout) findViewById(R.id.discover_search_bar);
        searchImg = (ImageView) findViewById(R.id.discover_search_icon);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, SearchActivity.class);
                startActivity(i);
            }
        };

        relativeLayout.setOnClickListener(listener);
        searchImg.setOnClickListener(listener);
    }

    private void setupBottomNavigationView(){
        ImageView homeBtn, homeBlueBtn, cameraBtn, cameraBlueBtn, discoverBtn, discoverBlueBtn;

        homeBtn = (ImageView) findViewById(R.id.bottom_home);
        homeBlueBtn = (ImageView) findViewById(R.id.bottom_home_blue);
        cameraBtn = (ImageView) findViewById(R.id.bottom_camera);
        cameraBlueBtn = (ImageView) findViewById(R.id.bottom_camera_blue);
        discoverBtn = (ImageView) findViewById(R.id.bottom_discover);
        discoverBlueBtn = (ImageView) findViewById(R.id.bottom_discover_blue);

        discoverBtn.setVisibility(View.GONE);
        discoverBlueBtn.setVisibility(View.VISIBLE);

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

        discoverBlueBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, DiscoverActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
        });
    }
}
