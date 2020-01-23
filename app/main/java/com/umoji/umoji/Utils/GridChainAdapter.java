package com.umoji.umoji.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.makeramen.roundedimageview.RoundedImageView;
import com.umoji.umoji.Models.Chain;
import com.umoji.umoji.R;
import com.vanniktech.emoji.EmojiTextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class GridChainAdapter extends BaseAdapter {

    private Context mContext;
    private LayoutInflater mInflater;
    private int layoutResource;

    private ArrayList<Chain> mChains;
    private ArrayList<ArrayList<String>> mChainTags;

    private DatabaseReference mRef;
    private StorageReference mStorageRef;

    public GridChainAdapter(Context context, int layoutResource, ArrayList<Chain> mChains, ArrayList<ArrayList<String>> mChainTags) {
        super();
        this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mContext = context;
        this.layoutResource = layoutResource;

        this.mChains = mChains;
        this.mChainTags = mChainTags;

        this.mRef = FirebaseDatabase.getInstance().getReference();
        this.mStorageRef = FirebaseStorage.getInstance().getReference();
    }

    private static class ViewHolder {
        RelativeLayout relativeLayout;
        RoundedImageView roundedImageView;
        ProgressBar progressBar;
        TextView textView;
        EmojiTextView emojiTextView;

        Chain chain;
        ArrayList<String> chainTags;

        ViewHolder(){
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = this.mInflater.inflate(R.layout.layout_grid_chainview, parent, false);
            viewHolder = new ViewHolder();

            viewHolder.relativeLayout = (RelativeLayout) convertView.findViewById(R.id.grid_chain_layout);
            viewHolder.roundedImageView = (RoundedImageView) convertView.findViewById(R.id.grid_chain_thumbnail);
            viewHolder.progressBar = (ProgressBar) convertView.findViewById(R.id.grid_chain_progressbar);

            viewHolder.textView = (TextView) convertView.findViewById(R.id.grid_chain_title);
            viewHolder.emojiTextView = (EmojiTextView) convertView.findViewById(R.id.grid_chain_tags_txt);

            viewHolder.chain = mChains.get(position);
            viewHolder.chainTags = mChainTags.get(position);

            convertView.setTag(viewHolder);

            setAspect(viewHolder);
            setThumbnailImage(viewHolder);
            setTitle(viewHolder);
            setTags(viewHolder);

        }

        return (View) convertView;
    }

    private void setAspect(ViewHolder viewHolder) {
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels;
        viewHolder.roundedImageView.getLayoutParams().height = (int) dpWidth * 2/3;
    }

    private void setTitle(ViewHolder viewHolder) {
        String temp = viewHolder.chain.getTitle();

        if(!TextUtils.isEmpty(temp)){
            viewHolder.textView.setVisibility(View.VISIBLE);
            viewHolder.textView.setText(temp);

        } else viewHolder.textView.setVisibility(View.GONE);

    }

    private void setTags(ViewHolder viewHolder) {
        ArrayList<String> tags = viewHolder.chainTags;

        if(tags.size() > 0){
            viewHolder.emojiTextView.setVisibility(View.VISIBLE);
            StringBuilder stringBuilder = new StringBuilder();

            for(String tag: tags) stringBuilder.append(tag);

            viewHolder.emojiTextView.setText(stringBuilder);

        } else viewHolder.emojiTextView.setVisibility(View.GONE);

    }

    private void setThumbnailImage(ViewHolder viewHolder) {
        Chain chain = viewHolder.chain;

        viewHolder.roundedImageView.setBackgroundColor(mContext.getResources().getColor(R.color.light_grey));

        if (viewHolder.progressBar != null) {
            viewHolder.progressBar.setVisibility(View.VISIBLE);
            viewHolder.progressBar.getIndeterminateDrawable().setColorFilter(mContext.getResources().getColor(R.color.progressBarColor), PorterDuff.Mode.SRC_IN);
        }

        try { // Download the Chain thumbnail
            final File localFile = File.createTempFile("images", "jpg");
            mStorageRef.child("thumbnails/" + chain.getFirst_user()
                    + "/"+ chain.getFirst_video() + ".jpg")
                    .getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());

                    viewHolder.roundedImageView.setImageBitmap(myBitmap);
                    viewHolder.roundedImageView.setBackgroundColor(mContext.getResources().getColor(R.color.white));

                    if (viewHolder.progressBar != null) viewHolder.progressBar.setVisibility(View.GONE);

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    if (viewHolder.progressBar != null) viewHolder.progressBar.setVisibility(View.GONE);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            if (viewHolder.progressBar != null) viewHolder.progressBar.setVisibility(View.GONE);
        }

    }

    @Override
    public int getCount() {
        return mChains.size();
    }

    @Override
    public Object getItem(int position) {
        return mChains.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

}

