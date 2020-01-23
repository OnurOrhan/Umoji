package com.umoji.umoji.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.umoji.umoji.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class GridImageAdapter extends ArrayAdapter<String>{

    private Context mContext;
    private LayoutInflater mInflater;
    private int layoutResource;
    private String mAppend;
    private ArrayList<String> imgURLs;
    private boolean isFirebase;

    public GridImageAdapter(Context context, int layoutResource, String append, ArrayList<String> imgURLs, boolean isFirebase) {
        super(context, layoutResource, imgURLs);
        this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mContext = context;
        this.layoutResource = layoutResource;
        this.mAppend = append;
        this.imgURLs = imgURLs;
        this.isFirebase = isFirebase;
    }

    private static class ViewHolder{
        RelativeLayout relLayout;
        SquareImageView image;
        ProgressBar mProgressBar;
        String imgURL;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final ViewHolder holder;
        convertView = mInflater.inflate(layoutResource, parent, false);
        holder = new ViewHolder();
        //holder.relLayout = (RelativeLayout) convertView.findViewById(R.id.grid_item_layout);
        holder.mProgressBar = (ProgressBar) convertView.findViewById(R.id.gridImageProgressbar);

        holder.image = (SquareImageView) convertView.findViewById(R.id.gridImageView);

        //holder.relLayout.getLayoutParams().height = (int) holder.relLayout.getLayoutParams().width*4/3;

        holder.imgURL = getItem(position);
        convertView.setTag(holder);

        //holder.image.setColorFilter(convertView.getResources().getColor(R.color.light_grey), PorterDuff.Mode.SRC_ATOP);

        if(holder.mProgressBar != null){
            holder.mProgressBar.setVisibility(View.VISIBLE);
            holder.mProgressBar.getIndeterminateDrawable().setColorFilter(mContext.getResources().getColor(R.color.progressBarColor), PorterDuff.Mode.SRC_IN);
        }

        if(isFirebase){
            try {
                final File localFile = File.createTempFile("images", "jpg");
                FirebaseStorage.getInstance().getReference().child(holder.imgURL)
                        .getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                        Bitmap myBitmap = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                        holder.image.setImageBitmap(myBitmap);
                        if(holder.mProgressBar != null){
                            holder.mProgressBar.setVisibility(View.GONE);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        if(holder.mProgressBar != null){
                            holder.mProgressBar.setVisibility(View.GONE);
                        }
                    }
                });
            } catch (IOException e) {
                if(holder.mProgressBar != null){
                    holder.mProgressBar.setVisibility(View.GONE);
                }
                e.printStackTrace();
            }
        } else {
            ImageLoader imageLoader = ImageLoader.getInstance();

            imageLoader.displayImage(this.mAppend + holder.imgURL, holder.image, new ImageLoadingListener() {
                @Override
                public void onLoadingStarted(String imageUri, View view) {
                    holder.mProgressBar.setVisibility(View.VISIBLE);
                }

                @Override
                public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                    holder.mProgressBar.setVisibility(View.GONE);
                }

                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                    holder.mProgressBar.setVisibility(View.GONE);
                }

                @Override
                public void onLoadingCancelled(String imageUri, View view) {
                    holder.mProgressBar.setVisibility(View.GONE);
                }
            });
        }

        return convertView;
    }
}

