package com.umoji.umoji.Search;

import android.content.Context;
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
import android.widget.GridView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.umoji.umoji.Models.Chain;
import com.umoji.umoji.Models.User;
import com.umoji.umoji.R;
import com.umoji.umoji.Utils.FriendListAdapter;
import com.umoji.umoji.Utils.GridChainAdapter;

import java.util.ArrayList;
import java.util.Objects;

public class SearchTagsFragment extends Fragment {
    private static final String TAG = "SearchTagsFragment";
    private Context mContext;

    private DatabaseReference myRef;

    private ArrayList<Chain> mChains;
    private ArrayList<ArrayList<String>> mChainTags;
    private GridView mGridView;
    private GridChainAdapter mGridAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContext = getContext();
        myRef = FirebaseDatabase.getInstance().getReference();

        View view = inflater.inflate(R.layout.fragment_search_tag, container, false);

        mGridView = (GridView) view.findViewById(R.id.search_tag_list);

        mChains = new ArrayList<>();
        mChainTags = new ArrayList<>();
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mChains.clear();
        mChainTags.clear();
        displayChains();
    }

    public void getChains(String searchString){
        Log.d(TAG, "getChains: getting chains");

        mChains.clear();
        mChainTags.clear();

        if(TextUtils.isEmpty(searchString)) displayChains();
        else {
            myRef.child("tag_chains").child(searchString).addListenerForSingleValueEvent(new ValueEventListener() {
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

                    mGridAdapter = new GridChainAdapter(mContext, R.layout.layout_grid_chainview, mChains, mChainTags);
                    mGridView.setAdapter(mGridAdapter);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


        }

    }

    public void clearChains(){
        mChains.clear();
        mChainTags.clear();
        mGridView.setAdapter(null);
    }

    private void displayChains(){
    }
}
