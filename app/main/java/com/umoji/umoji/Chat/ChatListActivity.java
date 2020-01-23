package com.umoji.umoji.Chat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.umoji.umoji.Home.HomeActivity;
import com.umoji.umoji.Models.Chat;
import com.umoji.umoji.Models.User;
import com.umoji.umoji.Profile.NotificationsActivity;
import com.umoji.umoji.Profile.ProfileActivity;
import com.umoji.umoji.R;
import com.umoji.umoji.Search.DiscoverActivity;
import com.umoji.umoji.Share.CustomCameraActivity;
import com.umoji.umoji.Utils.ChatListAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class ChatListActivity extends AppCompatActivity {
    private static final String TAG = "ChatListActivity";
    private Context mContext;

    private FirebaseAuth mAuth;
    private DatabaseReference mRef;
    private String currentUser;

    private ArrayList<Chat> mChats;
    private ArrayList<User> mFriends;
    private ArrayList<Chat> mPeople;

    private EditText searchTxt;
    private ChatListAdapter mChatsAdapter;
    private ChatListAdapter mAdapter;
    private LinearLayoutManager mLinearLayout;
    private RecyclerView mListView;

    private ImageView clearBtn;

    private Boolean isSearching, jobExists;

    private ImageView profileBtn;
    private ImageView messagesBtn;
    private ImageView notifBtn;
    private ImageView bubbleImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        mContext = ChatListActivity.this;

        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseDatabase.getInstance().getReference();
        currentUser = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        mChats = new ArrayList<>();
        mFriends = new ArrayList<>();
        mPeople = new ArrayList<>();

        isSearching = false;
        jobExists = false;

        mLinearLayout = new LinearLayoutManager(mContext);
        mListView = (RecyclerView) findViewById(R.id.chat_list);
        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(mLinearLayout);

        getChats();
        //getFriends();
        getAllUsers();

        searchTxt = (EditText) findViewById(R.id.chat_search_text);
        searchTxt.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
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

        clearBtn = (ImageView) findViewById(R.id.chat_search_clear);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearBtnFunc();
            }
        });

        setupBottomNavigationView();
        initTextListener();

        ImageView backBtn = (ImageView) findViewById(R.id.top_bar_back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        profileBtn = (ImageView) findViewById(R.id.top_bar_profile_btn);
        messagesBtn = (ImageView) findViewById(R.id.top_bar_messages_btn);
        notifBtn = (ImageView) findViewById(R.id.top_bar_notifications_btn);
        bubbleImg = (ImageView) findViewById(R.id.top_bar_notifications_bubble);

        TextView titleTxt = (TextView) findViewById(R.id.top_bar_title);
        titleTxt.setVisibility(View.VISIBLE);
        titleTxt.setText("Chats");

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
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
        });

        notifBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, NotificationsActivity.class);
                startActivity(intent);
            }
        });

        mRef.child("notifications").child(currentUser).orderByChild("seen").equalTo(false)
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

    private void lookForJob(){
        if(jobExists){
            jobExists = false;
            isSearching = true;

            String temp = searchTxt.getText().toString().trim();
            if(TextUtils.isEmpty(temp)) clearPeople();
            else searchPeople(temp);
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
                //                System.currentTimeMillis();
                String temp = searchTxt.getText().toString().trim();
                if(temp.length() == 0) clearPeople();

                if(!isSearching){
                    isSearching = true;
                    searchPeople(searchTxt.getText().toString().trim());
                } else {
                    jobExists = true;
                }
            }
        });
    }

    private void searchChats() {
        mRef.child("user_chat_info").child(currentUser).orderByChild("last_date")
                .addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Chat chat = dataSnapshot.getValue(Chat.class);
                mChats.add(0, chat);
                mChatsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Chat chat = dataSnapshot.getValue(Chat.class);
                Chat c;

                for(int i = 0; i < mChats.size(); i++){
                    c = mChats.get(i);

                    assert chat != null;
                    if(c.getUser_id().equals(chat.getUser_id())){
                        c.setLast_date(chat.getLast_date());
                        c.setLast_message(chat.getLast_message());
                        c.setFrom_self(chat.getFrom_self());

                        int index = i;
                        for (int j = i; j > 0; j--){
                            if(c.getLast_date() < mChats.get(j-1).getLast_date()){
                                index = j;
                                break;
                            } else if (j == 1) index = 0;
                        }

                        mChats.remove(i);
                        mChats.add(index, c);

                        mChatsAdapter.notifyDataSetChanged();

                        return;
                    }
                }

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

    private void searchPeople(final String keyword) {
        mPeople.clear();
        if(TextUtils.isEmpty(keyword)) displayPeople();

        else {
            for (User u: mFriends){
                if (u.getName().toLowerCase().contains(keyword.toLowerCase())
                        || u.getUsername().toLowerCase().contains(keyword.toLowerCase())
                        || keyword.toLowerCase().contains(u.getName().toLowerCase())
                        || keyword.toLowerCase().contains(u.getUsername().toLowerCase())){
                    Chat c = new Chat();
                    String username = u.getUsername();
                    String userid = u.getUser_id();
                    c.setUsername(username);
                    c.setUser_id(userid);
                    mPeople.add(c);
                }
            } displayPeople();
        }
    }

    private void clearBtnFunc() {
        hideSoftKeyboard();
        searchTxt.clearFocus();
        searchTxt.getText().clear();
        clearPeople();
    }

    private void clearPeople(){
        mListView.setAdapter(mChatsAdapter);
        mPeople.clear();
        isSearching = false;
        jobExists = false;
    }

    private void displayChats() {
        if(mChats != null){
            Collections.sort(mChats, new Comparator<Chat>() {
                @Override
                public int compare(Chat o1, Chat o2) {
                    return (int)(o2.getLast_date() - o1.getLast_date());
                }
            });

            mChatsAdapter = new ChatListAdapter(mChats);
            mListView.setAdapter(mChatsAdapter);

            isSearching = false;
            lookForJob();
        }
    }

    private void displayPeople() {
        if(mPeople != null){
            Collections.sort(mPeople, new Comparator<Chat>() {
                @Override
                public int compare(Chat o1, Chat o2) {
                    return (int)(o2.getUsername().compareTo(o1.getUsername()));
                }
            });

            mAdapter = new ChatListAdapter(mPeople);
            mListView.setAdapter(mAdapter);

            isSearching = false;
            lookForJob();
        }
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

    private void getChats() {
        displayChats();
        searchChats();
    }

    private void getFriends(){
        Log.d(TAG, "getFriends: retrieving friends");

        mFriends.clear();

        Query query = mRef.child("friends").child(currentUser);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    Query userq = mRef.child("users").orderByChild("user_id").equalTo(singleSnapshot.getValue(String.class));
                    userq.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
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

    private void getAllUsers(){
        Log.d(TAG, "getAllUsers: retrieving users");

        mFriends.clear();

        Query query = mRef.child("users");
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    User user = new User();
                    User result = singleSnapshot.getValue(User.class);

                    if (result != null) {
                        if (result.getUser_id() != null) user.setUser_id(result.getUser_id());
                        if(user.getUser_id().equals(currentUser)) continue;
                        if (result.getEmail() != null) user.setEmail(result.getEmail());
                        if (result.getUsername() != null) user.setUsername(result.getUsername());
                        if (result.getName() != null) user.setName(result.getName());
                        if (result.getDescription() != null) user.setDescription(result.getDescription());
                    }

                    mFriends.add(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

}
