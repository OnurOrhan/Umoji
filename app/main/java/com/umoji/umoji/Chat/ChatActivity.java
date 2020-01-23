package com.umoji.umoji.Chat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.makeramen.roundedimageview.RoundedImageView;
import com.umoji.umoji.Models.Chat;
import com.umoji.umoji.Models.Message;
import com.umoji.umoji.Profile.NotificationsActivity;
import com.umoji.umoji.Profile.ProfileActivity;
import com.umoji.umoji.R;
import com.umoji.umoji.Search.DiscoverActivity;
import com.umoji.umoji.Utils.MessageListAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private static final int LOAD_RATE = 15;
    private Context mContext;

    private FirebaseAuth mAuth;
    private DatabaseReference mRef;
    private StorageReference mStorageRef;

    private String currentUser;
    private String otherUser;
    private String currentUsername;
    private String otherUsername;

    private RoundedImageView mProfilePhoto;
    private TextView mUsername;

    private List<Message> mMessageList;
    private long firstMessageDate, lastMessageDate;

    private LinearLayoutManager mLinearLayout;
    private MessageListAdapter mAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private EditText mMessageTxt;
    private TextView mSendBtn;

    private ImageView profileBtn;
    private ImageView messagesBtn;
    private ImageView notifBtn;
    private ImageView bubbleImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Log.d(TAG, "onCreate: ChatActivity created.");
        mContext = ChatActivity.this;

        firstMessageDate = 0;
        lastMessageDate = 0;

        mMessageList = new ArrayList<>();

        mAuth = FirebaseAuth.getInstance();
        mRef = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        if(mAuth.getCurrentUser() != null){
            currentUser = mAuth.getCurrentUser().getUid();
        }

        Intent intent = getIntent();
        otherUser = intent.getStringExtra("user_id"); // User whom we chat with
        otherUsername = intent.getStringExtra("username");
        currentUsername = Objects.requireNonNull(mAuth.getCurrentUser()).getDisplayName();

        mAdapter = new MessageListAdapter(mContext, mMessageList);

        mRecyclerView = (RecyclerView) findViewById(R.id.chat_messages_list);
        mLinearLayout = new LinearLayoutManager(mContext);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLinearLayout);
        mRecyclerView.setAdapter(mAdapter);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.chat_swipe_layout);

        retrieveMessages();

        mProfilePhoto = (RoundedImageView) findViewById(R.id.chat_person_photo);
        mUsername = (TextView) findViewById(R.id.chat_person_username);
        setPhoto();

        mUsername.setText(otherUsername);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, ProfileActivity.class);
                i.putExtra("user_id", otherUser);
                mContext.startActivity(i);
            }
        };

        mProfilePhoto.setOnClickListener(listener);
        mUsername.setOnClickListener(listener);

        mMessageTxt = (EditText) findViewById(R.id.chat_message_txt);
        mSendBtn = (TextView) findViewById(R.id.chat_send_message_btn);
        mSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!TextUtils.isEmpty(mMessageTxt.getText())){
                    String newKey = mRef.child("user_chats").child(currentUser).child(otherUser).push().getKey();

                    String txt = mMessageTxt.getText().toString().trim();
                    final long t = Calendar.getInstance().getTimeInMillis();

                    mMessageTxt.getText().clear();

                    Message m = new Message(txt, t);
                    Chat c = new Chat(otherUser, txt, t);
                    c.setUsername(otherUsername);

                    mRef.child("user_chats").child(currentUser).child(otherUser).child(newKey).setValue(m);
                    mRef.child("user_chat_info").child(currentUser).child(otherUser).setValue(c);

                    m.setFrom_self(false);
                    mRef.child("user_chats").child(otherUser).child(currentUser).child(newKey).setValue(m);
                    c.setUsername(currentUsername);
                    c.setUser_id(currentUser);
                    c.setFrom_self(false);
                    mRef.child("user_chat_info").child(otherUser).child(currentUser).setValue(c);
                }
            }
        });

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

    private void retrieveMessages() {
        mRef.child("user_chats").child(currentUser).child(otherUser)
                .orderByChild("message_date").limitToLast(LOAD_RATE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        long count = 0;

                        for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                            Message message = singleSnapshot.getValue(Message.class);
                            Message temp = new Message();

                            assert message != null;
                            temp.setMessage_text(message.getMessage_text());
                            temp.setFrom_self(message.getFrom_self());

                            long temp_date = message.getMessage_date();
                            temp.setMessage_date(temp_date);
                            count++;
                            if (count == 1) firstMessageDate = temp_date;
                            if (temp_date < firstMessageDate) firstMessageDate = temp_date;
                            if (temp_date > lastMessageDate) lastMessageDate = temp_date;

                            mMessageList.add(message);
                        }

                        mAdapter.notifyDataSetChanged();
                        mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
                        setMessageListener();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void setMessageListener(){
        mRef.child("user_chats").child(currentUser).child(otherUser)
                .orderByChild("message_date").startAt(lastMessageDate + 1)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Message message = dataSnapshot.getValue(Message.class);
                        Message temp = new Message();

                        assert message != null;
                        temp.setMessage_text(message.getMessage_text());
                        temp.setFrom_self(message.getFrom_self());
                        long temp_date = message.getMessage_date();
                        temp.setMessage_date(temp_date);

                        if(temp_date > lastMessageDate) lastMessageDate = temp_date;
                        mMessageList.add(temp);
                        mAdapter.notifyDataSetChanged();
                        mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
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

        setSwipeListener();
    }

    private void setSwipeListener() {
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRef.child("user_chats").child(currentUser).child(otherUser)
                        .orderByChild("message_date").endAt(firstMessageDate - 1).limitToLast(LOAD_RATE)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                long count = 0;

                                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                                    Message message = singleSnapshot.getValue(Message.class);
                                    Message temp = new Message();

                                    assert message != null;
                                    temp.setMessage_text(message.getMessage_text());
                                    temp.setFrom_self(message.getFrom_self());

                                    long temp_date = message.getMessage_date();
                                    temp.setMessage_date(temp_date);
                                    if (temp_date < firstMessageDate) firstMessageDate = temp_date;

                                    mMessageList.add((int) count, message);
                                    count++;
                                }

                                swipeRefreshLayout.setRefreshing(false);
                                mAdapter.notifyDataSetChanged();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }
        });
    }

    private void setPhoto() {
        // Set the profile image
        try {
            final File localFile = File.createTempFile("images", "jpg");
            mStorageRef.child("profile_photos/" + otherUser + ".jpg")
                    .getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                    mProfilePhoto.setImageBitmap(myBitmap);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: ChatActivity started.");
        super.onStart();

        hideSoftKeyboard();
    }

    private void hideSoftKeyboard(){
        if(getCurrentFocus() != null){
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        }
    }
}
