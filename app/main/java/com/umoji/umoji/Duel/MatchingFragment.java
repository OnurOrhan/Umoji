package com.umoji.umoji.Duel;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.umoji.umoji.Models.User;
import com.umoji.umoji.Profile.ProfileActivity;
import com.umoji.umoji.R;
import com.umoji.umoji.Utils.MatchListAdapter;
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

public class MatchingFragment extends Fragment {
    private static final String TAG = "MatchingFragment";

    private FirebaseAuth mAuth;
    private DatabaseReference myRef;
    private String currentUser;

    private ArrayList<String> mMatches;
    private ArrayList<String> interests;
    private ArrayList<String> friends;

    private ListView mListView;
    private MatchListAdapter mAdapter;
    private Boolean foundInterests;
    private final int limit = 15;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference();

        View view = inflater.inflate(R.layout.fragment_matching, container, false);
        mListView = (ListView) view.findViewById(R.id.duel_matching_list);

        mMatches = new ArrayList<>();
        interests = new ArrayList<>();
        friends = new ArrayList<>();
        foundInterests = false;
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null){
            currentUser = user.getUid();

            if(!foundInterests){
                Query query = myRef.child("users").child(currentUser).child("tags");
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                            interests.add(singleSnapshot.getValue(String.class));
                        }

                        Query query = myRef.child("friends").child(currentUser);
                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                                    friends.add(singleSnapshot.getValue(String.class));
                                }

                                foundInterests = true;
                                mMatches.clear();
                                getMatches(); // getFriends() --> getMatches() --> displayMatches()
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
            } else {
                mMatches.clear();
                getMatches(); // getFriends() --> getMatches() --> displayMatches()
            }
        }
    }

    private void getMatches(){
        if(interests.size() == 0) getRandomMatches();

        final ArrayList<String> results = new ArrayList<>();
        for(int j = 0; j < interests.size(); j++){
            final int finalJ = j;

            Query q = myRef.child("tag_users").child(interests.get(j));
            q.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    int count = 0;

                    for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                        String temp = singleSnapshot.getKey();
                        if(temp.equals(currentUser) || friends.contains(temp)) continue;

                        count++;
                        results.add(temp);
                        if(count == limit) break;
                    } if(finalJ == interests.size()-1) displayMatches(results);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }
    }

    private void getRandomMatches() {
        final ArrayList<String> results = new ArrayList<>();

        Query q = myRef.child("users");
        q.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long limitCount = 0;

                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    String temp = singleSnapshot.getValue(User.class).getUser_id();
                    if(temp.equals(currentUser) || friends.contains(temp)) continue;

                    if(!results.contains(temp)){
                        limitCount++;
                        results.add(temp);
                    }

                    if(limitCount == limit) break;
                } displayMatches(results);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void displayMatches(ArrayList<String> results){
        if(results.size() == 0) getRandomMatches();

        Collections.shuffle(results);
        for (int i = 0; i < results.size(); i++){
            if(i == limit) break;
            mMatches.add(results.get(i));
        }

        if(mMatches != null){
            mAdapter = new MatchListAdapter(getActivity(), R.layout.layout_match_listitem, mMatches);
            mListView.setAdapter(mAdapter);
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent i = new Intent(getContext(), ProfileActivity.class);
                    i.putExtra("user_id", mMatches.get(position));
                    startActivity(i);
                }
            });
        }
    }
}
