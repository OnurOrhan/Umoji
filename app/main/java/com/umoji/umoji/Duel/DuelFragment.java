package com.umoji.umoji.Duel;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.umoji.umoji.Models.User;
import com.umoji.umoji.R;
import com.umoji.umoji.Utils.FriendListAdapter;
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

public class DuelFragment extends Fragment {
    private static final String TAG = "DuelFragment";

    private Context mContext;

    private FirebaseAuth mAuth;
    private DatabaseReference myRef;
    private String currentUser;

    private ArrayList<User> mFriends;
    private LinearLayoutManager mLinearLayout;
    private RecyclerView mRecyclerView;
    private FriendListAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContext = getContext();
        mAuth = FirebaseAuth.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference();

        View view = inflater.inflate(R.layout.fragment_duel, container, false);

        mLinearLayout = new LinearLayoutManager(mContext);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.duel_friend_list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLinearLayout);

        mFriends = new ArrayList<>();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null){
            currentUser = user.getUid();

            mFriends.clear();
            getFriends();
        }
    }

    private void getFriends(){
        Log.d(TAG, "getFriends: getting friends");

        final DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference.child("friends").child(currentUser);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                final long size = dataSnapshot.getChildrenCount();

                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    Query userq = reference.child("users").orderByChild("user_id").equalTo(singleSnapshot.getValue(String.class));
                    userq.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                                User user = new User();
                                User result = singleSnapshot.getValue(User.class);

                                user.setUser_id(result.getUser_id());
                                user.setEmail(result.getEmail());
                                user.setUsername(result.getUsername());
                                user.setName(result.getName());
                                user.setDescription(result.getDescription());

                                mFriends.add(user);
                                if(mFriends.size() == size) displayFriends();
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

    private void displayFriends(){
        if(mFriends != null){
            Collections.sort(mFriends, new Comparator<User>() {
                @Override
                public int compare(User o1, User o2) {
                    return (int)(o2.getUsername().compareTo(o1.getUsername()));
                }
            });

            mAdapter = new FriendListAdapter(mFriends, getActivity());
            mRecyclerView.setAdapter(mAdapter);
        }
    }
}
