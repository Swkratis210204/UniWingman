package com.example.uniwingman.ui.aisimulator;

import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.example.uniwingman.R;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;
    private static final int VIEW_TYPE_LOADING = 3;

    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = new ArrayList<>(messages);
    }

    public void setMessages(List<ChatMessage> newMessages) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ChatMessagesDiffCallback(this.messages, newMessages));
        this.messages.clear();
        this.messages.addAll(newMessages);
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage msg = messages.get(position);
        switch (msg.getType()) {
            case USER: return VIEW_TYPE_USER;
            case LOADING: return VIEW_TYPE_LOADING;
            default: return VIEW_TYPE_AI;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            View view = inflater.inflate(R.layout.item_chat_user, parent, false);
            return new ChatViewHolder(view);
        } else if (viewType == VIEW_TYPE_LOADING) {
            View view = inflater.inflate(R.layout.item_chat_loading, parent, false);
            return new LoadingViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_chat_ai, parent, false);
            return new AiViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);

        if (holder instanceof ChatViewHolder) {
            ((ChatViewHolder) holder).textView.setText(msg.getText());

        } else if (holder instanceof AiViewHolder) {
            AiViewHolder aiHolder = (AiViewHolder) holder;
            aiHolder.textView.setText(msg.getText());

            // Show response time for online mode
            if (msg.hasResponseTime()) {
                String timeStr = String.format("⚡ %.1fδ.", msg.getResponseTimeMs() / 1000.0);                aiHolder.tvTime.setText(timeStr);
                aiHolder.tvTime.setVisibility(View.VISIBLE);
            } else {
                aiHolder.tvTime.setVisibility(View.GONE);
            }

        } else if (holder instanceof LoadingViewHolder) {
            ((LoadingViewHolder) holder).startAnimation();
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof LoadingViewHolder) {
            ((LoadingViewHolder) holder).stopAnimation();
        }
    }

    @Override
    public int getItemCount() { return messages != null ? messages.size() : 0; }

    // ── ViewHolders ──

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ChatViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tvMessage);
        }
    }

    static class AiViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        TextView tvTime;
        AiViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvResponseTime);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        View dot1, dot2, dot3;
        ObjectAnimator anim1, anim2, anim3;

        LoadingViewHolder(View itemView) {
            super(itemView);
            dot1 = itemView.findViewById(R.id.tvDot1);
            dot2 = itemView.findViewById(R.id.tvDot2);
            dot3 = itemView.findViewById(R.id.tvDot3);
        }

        void startAnimation() {
            anim1 = ObjectAnimator.ofFloat(dot1, "alpha", 1f, 0.2f, 1f);
            anim1.setDuration(900);
            anim1.setRepeatCount(ObjectAnimator.INFINITE);
            anim1.setStartDelay(0);

            anim2 = ObjectAnimator.ofFloat(dot2, "alpha", 1f, 0.2f, 1f);
            anim2.setDuration(900);
            anim2.setRepeatCount(ObjectAnimator.INFINITE);
            anim2.setStartDelay(200);

            anim3 = ObjectAnimator.ofFloat(dot3, "alpha", 1f, 0.2f, 1f);
            anim3.setDuration(900);
            anim3.setRepeatCount(ObjectAnimator.INFINITE);
            anim3.setStartDelay(400);

            anim1.start();
            anim2.start();
            anim3.start();
        }

        void stopAnimation() {
            if (anim1 != null) anim1.cancel();
            if (anim2 != null) anim2.cancel();
            if (anim3 != null) anim3.cancel();
        }
    }

    // ── DiffUtil ──

    private static class ChatMessagesDiffCallback extends DiffUtil.Callback {
        private final List<ChatMessage> oldList;
        private final List<ChatMessage> newList;

        public ChatMessagesDiffCallback(List<ChatMessage> oldList, List<ChatMessage> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override public int getOldListSize() { return oldList != null ? oldList.size() : 0; }
        @Override public int getNewListSize() { return newList != null ? newList.size() : 0; }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getText().equals(newList.get(newPos).getText())
                    && oldList.get(oldPos).getType() == newList.get(newPos).getType();
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            ChatMessage o = oldList.get(oldPos);
            ChatMessage n = newList.get(newPos);
            return o.getText().equals(n.getText()) && o.isUser() == n.isUser() && o.getType() == n.getType();
        }
    }
}