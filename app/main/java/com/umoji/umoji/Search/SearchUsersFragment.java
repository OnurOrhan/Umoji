package com.umoji.umoji.Search;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.umoji.umoji.Models.User;
import com.umoji.umoji.R;
import com.umoji.umoji.Utils.FriendListAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;

public class SearchUsersFragment extends Fragment {
    private static final String TAG = "SearchUsersFragment";
    private Context mContext;

    private DatabaseReference myRef;

    private ArrayList<User> mUsers;
    private LinearLayoutManager mLinearLayout;
    private RecyclerView mRecyclerView;
    private FriendListAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContext = getContext();
        myRef = FirebaseDatabase.getInstance().getReference();

        View view = inflater.inflate(R.layout.fragment_search_user, container, false);

        mLinearLayout = new LinearLayoutManager(mContext);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.search_user_list);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLinearLayout);

        mUsers = new ArrayList<>();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mUsers.clear();
        displayUsers();
    }

    public void getUsers(String searchString){
        Log.d(TAG, "getUsers: getting friends");

        mUsers.clear();
        if(TextUtils.isEmpty(searchString)) displayUsers();
        else {
            myRef.child("users").orderByChild("username").startAt(searchString.toLowerCase()).endAt(searchString.toLowerCase() + "\uf8ff").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                        User user = new User();
                        User result = singleSnapshot.getValue(User.class);

                        user.setUser_id(result.getUser_id());
                        user.setEmail(result.getEmail());
                        user.setUsername(result.getUsername());
                        user.setName(result.getName());
                        user.setDescription(result.getDescription());

                        mUsers.add(user);
                    }

                    myRef.child("users").orderByChild("name").startAt(searchString).endAt(searchString + "\uf8ff").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                                User user = new User();
                                User result = singleSnapshot.getValue(User.class);

                                user.setUser_id(result.getUser_id());
                                user.setEmail(result.getEmail());
                                user.setUsername(result.getUsername());
                                user.setName(result.getName());
                                user.setDescription(result.getDescription());

                                mUsers.add(user);
                            }

                            displayUsers();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            mUsers.clear();
                            ((SearchActivity) Objects.requireNonNull(getActivity())).isSearching = false;
                            ((SearchActivity) Objects.requireNonNull(getActivity())).lookForJob();
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    ((SearchActivity) Objects.requireNonNull(getActivity())).isSearching = false;
                    ((SearchActivity) Objects.requireNonNull(getActivity())).lookForJob();
                }
            });
        }

    }

    public void clearUsers(){
        mUsers.clear();
        mRecyclerView.setAdapter(null);
    }

    private void displayUsers(){
        if(mUsers != null){
            Collections.sort(mUsers, new Comparator<User>() {
                @Override
                public int compare(User o1, User o2) {
                    return (int)(o2.getUsername().compareTo(o1.getUsername()));
                }
            });

            mAdapter = new FriendListAdapter(mUsers, getActivity());
            mRecyclerView.setAdapter(mAdapter);
        }

        ((SearchActivity) Objects.requireNonNull(getActivity())).isSearching = false;
        ((SearchActivity) Objects.requireNonNull(getActivity())).lookForJob();
    }
}
