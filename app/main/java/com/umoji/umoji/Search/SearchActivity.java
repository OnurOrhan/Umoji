package com.umoji.umoji.Search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.umoji.umoji.Home.HomeActivity;
import com.umoji.umoji.Models.Chain;
import com.umoji.umoji.Models.User;
import com.umoji.umoji.Profile.ProfileActivity;
import com.umoji.umoji.R;
import com.umoji.umoji.Share.CustomCameraActivity;
import com.umoji.umoji.Utils.FriendListAdapter;
import com.umoji.umoji.Utils.MainfeedListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.umoji.umoji.Utils.SectionsPagerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = "SearchActivity";
    private Context mContext;
    private ViewPager mViewPager;

    private FirebaseAuth mAuth;
    private DatabaseReference myRef;
    private String currentUser;

    private SearchUsersFragment searchUsersFragment;
    private SearchTagsFragment searchTagsFragment;

    private EditText searchTxt;

    private ImageView clearBtn;
    private ImageView cancelBtn;

    public Boolean isSearching;
    private Boolean jobExists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Log.d(TAG, "onCreate: created...");
        mContext = SearchActivity.this;

        mAuth = FirebaseAuth.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference();

        searchTxt = (EditText) findViewById(R.id.search_text);
        searchTxt.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    switch (keyCode)
                    {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            if(!isSearching){
                                isSearching = true;
                                searchUsersFragment.getUsers(searchTxt.getText().toString().trim().toLowerCase());
                                searchTagsFragment.getChains(searchTxt.getText().toString().trim().toLowerCase());

                            } else {
                                jobExists = true;
                            }

                            hideSoftKeyboard();
                            searchTxt.clearFocus();
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        });

        clearBtn = (ImageView) findViewById(R.id.search_clear);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jobExists = false;
                searchTxt.setText("");
                hideSoftKeyboard();
                searchTxt.clearFocus();
                searchUsersFragment.clearUsers();
                searchTagsFragment.clearChains();
            }
        });

        cancelBtn = (ImageView) findViewById(R.id.search_cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mViewPager = (ViewPager) findViewById(R.id.search_viewpager_container);

        setupBottomNavigationView();
        setupViewPager();
        initTextListener();
    }

    private void setupViewPager() {
        searchUsersFragment = new SearchUsersFragment();
        searchTagsFragment = new SearchTagsFragment();

        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(searchUsersFragment); //index 0
        adapter.addFragment(searchTagsFragment); //index 1
        mViewPager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.search_tabs);
        tabLayout.setupWithViewPager(mViewPager);

        tabLayout.getTabAt(0).setText("USERS");
        tabLayout.getTabAt(1).setText("TAGS");
    }

    @Override
    protected void onStart() {
        super.onStart();
        isSearching = false;
        jobExists = false;

        currentUser = mAuth.getCurrentUser().getUid();

        searchTxt.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        Objects.requireNonNull(imm).showSoftInput(searchTxt, InputMethodManager.SHOW_IMPLICIT);
    }

    public void lookForJob(){
        if(jobExists){
            isSearching = true;
            jobExists = false;

            searchTagsFragment.getChains(searchTxt.getText().toString().trim().toLowerCase());
            searchUsersFragment.getUsers(searchTxt.getText().toString().trim().toLowerCase());
        }
    }

    private void initTextListener(){
        Log.d(TAG, "initTextListener: initializing");

        searchTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String text = searchTxt.getText().toString().toLowerCase();
                if(text.length() == 0) return;

                if(!isSearching){
                    isSearching = true;
                    searchUsersFragment.getUsers(text);
                    searchTagsFragment.getChains(text);

                } else {
                    jobExists = true;
                }
            }
        });
    }

    private void hideSoftKeyboard(){
        if(getCurrentFocus() != null){
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
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

        homeBtn.setVisibility(View.GONE);
        homeBlueBtn.setVisibility(View.VISIBLE);

        homeBlueBtn.setOnClickListener(new View.OnClickListener() {
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
