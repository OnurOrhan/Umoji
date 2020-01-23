package com.umoji.umoji.Utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.umoji.umoji.Models.Message;
import com.umoji.umoji.R;

import java.util.List;

public class MessageListAdapter extends RecyclerView.Adapter<MessageListAdapter.MessageViewHolder> {
    private static final String TAG = "MessageListAdapter";

    private Context mContext;
    private List<Message> mMessageList;

    public MessageListAdapter(Context context, List<Message> mMessageList) {
        this.mContext = context;
        this.mMessageList = mMessageList;
    }

    @NonNull
    @Override
    public MessageListAdapter.MessageViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.layout_message_item, viewGroup, false);

        return new MessageViewHolder(v);
    }

    class MessageViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout selfLayout, otherLayout;
        TextView selfTxt, otherTxt;

        MessageViewHolder(View view){
            super(view);

            selfTxt = (TextView) view.findViewById(R.id.self_message_txt_item);
            otherTxt = (TextView) view.findViewById(R.id.other_message_txt_item);

            selfLayout = (RelativeLayout) view.findViewById(R.id.self_message_item_layout);
            otherLayout = (RelativeLayout) view.findViewById(R.id.other_message_item_layout);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageListAdapter.MessageViewHolder viewHolder, int i) {
        Message c = mMessageList.get(i);

        if(c.getFrom_self()){
            viewHolder.otherLayout.setVisibility(View.GONE);
            viewHolder.selfTxt.setText(c.getMessage_text());
            viewHolder.selfLayout.setVisibility(View.VISIBLE);
        } else {
            viewHolder.selfLayout.setVisibility(View.GONE);
            viewHolder.otherTxt.setText(c.getMessage_text());
            viewHolder.otherLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }
}
