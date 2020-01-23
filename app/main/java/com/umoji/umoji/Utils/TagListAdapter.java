package com.umoji.umoji.Utils;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.umoji.umoji.R;
import com.umoji.umoji.Search.WatchStoryActivity;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.EmojiTextView;
import com.vanniktech.emoji.one.EmojiOneProvider;

import java.util.ArrayList;
import java.util.List;

public class TagListAdapter extends RecyclerView.Adapter<TagListAdapter.TagViewHolder> {
    private static final String TAG = "TagListAdapter";

    private ArrayList<String> mTagsList;
    private Context mContext;

    public TagListAdapter(ArrayList<String> mTagsList, Context context){
        this.mTagsList = mTagsList;
        this.mContext = context;
        EmojiManager.install(new EmojiOneProvider());
    }

    @NonNull
    @Override
    public TagListAdapter.TagViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        mContext = viewGroup.getContext();
        View v = LayoutInflater.from(mContext)
                .inflate(R.layout.layout_tag_listitem, viewGroup, false);

        return new TagViewHolder(v);
    }

    class TagViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout relativeLayout;
        private EmojiTextView textView;
        private String tagString;

        TagViewHolder(View view){
            super(view);

            relativeLayout = (RelativeLayout) view.findViewById(R.id.tag_item_layout);
            textView = (EmojiTextView) view.findViewById(R.id.tag_name_txt);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final TagListAdapter.TagViewHolder viewHolder, int i) {
        viewHolder.tagString = mTagsList.get(i);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, WatchStoryActivity.class);
                intent.putStringArrayListExtra("mTags", mTagsList);
                intent.putExtra("tagIndex", i);
                mContext.startActivity(intent);
            }
        };

        viewHolder.relativeLayout.setOnClickListener(listener);
        viewHolder.textView.setOnClickListener(listener);

        viewHolder.textView.setText(viewHolder.tagString);
    }

    @Override
    public int getItemCount() {
        return mTagsList.size();
    }
}


