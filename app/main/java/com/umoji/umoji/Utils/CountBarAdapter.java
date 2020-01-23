package com.umoji.umoji.Utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.umoji.umoji.R;

public class CountBarAdapter extends RecyclerView.Adapter<CountBarAdapter.BarViewHolder> {
    private static final String TAG = "CountBarAdapter";

    private int count;
    private int itemWidth;
    private int whiteCode, greyCode;

    private Context mContext;

    public CountBarAdapter(int count, Context context){
        this.count = count;
        this.mContext = context;

        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels;
        itemWidth = (int) ((dpWidth - 4) / count) - 4;

        whiteCode = mContext.getResources().getColor(R.color.white);
        greyCode = mContext.getResources().getColor(R.color.dark_grey);
    }

    @NonNull
    @Override
    public CountBarAdapter.BarViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        mContext = viewGroup.getContext();
        View v = LayoutInflater.from(mContext)
                .inflate(R.layout.layout_story_index_item, viewGroup, false);

        return new BarViewHolder(v);
    }

    public class BarViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout relativeLayout;

        BarViewHolder(View view){
            super(view);

            relativeLayout = (RelativeLayout) view.findViewById(R.id.story_item_layout);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final CountBarAdapter.BarViewHolder viewHolder, int i) {
        viewHolder.relativeLayout.getLayoutParams().width = itemWidth;

        if (i == 0){
            setWatched(viewHolder);
        }
    }

    public void setWatched(BarViewHolder viewHolder){
        viewHolder.relativeLayout.setBackgroundColor(whiteCode);
    }

    public void setUnwatched(BarViewHolder viewHolder){
        viewHolder.relativeLayout.setBackgroundColor(greyCode);
    }

    @Override
    public int getItemCount() {
        return count;
    }
}


