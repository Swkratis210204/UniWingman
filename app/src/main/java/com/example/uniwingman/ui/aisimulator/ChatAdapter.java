package com.example.uniwingman.ui.aisimulator;

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

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;
    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        // Χρησιμοποιούμε ArrayList για να μπορούμε να τροποποιήσουμε τη λίστα αν χρειαστεί
        this.messages = new ArrayList<>(messages);
    }

    // Η ΝΕΑ ΜΕΘΟΔΟΣ ΠΟΥ ΛΥΝΕΙ ΤΟ SCROLL UP
    public void setMessages(List<ChatMessage> newMessages) {
        // Υπολογισμός των διαφορών ανάμεσα στην παλιά και τη νέα λίστα
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ChatMessagesDiffCallback(this.messages, newMessages));

        // Ενημέρωση της εσωτερικής λίστας
        this.messages.clear();
        this.messages.addAll(newMessages);

        // Ενημέρωση του RecyclerView μόνο για τις αλλαγές
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = (viewType == VIEW_TYPE_USER) ? R.layout.item_chat_user : R.layout.item_chat_ai;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.textView.setText(messages.get(position).getText());
    }

    @Override
    public int getItemCount() { return messages != null ? messages.size() : 0; }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ChatViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tvMessage);
        }
    }

    // ΕΣΩΤΕΡΙΚΗ ΚΛΑΣΗ ΓΙΑ ΤΟ DIFFUTIL
    private static class ChatMessagesDiffCallback extends DiffUtil.Callback {
        private final List<ChatMessage> oldList;
        private final List<ChatMessage> newList;

        public ChatMessagesDiffCallback(List<ChatMessage> oldList, List<ChatMessage> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList != null ? oldList.size() : 0; }

        @Override
        public int getNewListSize() { return newList != null ? newList.size() : 0; }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // Ελέγχουμε αν είναι το ίδιο αντικείμενο (συνήθως μέσω ID, εδώ μέσω περιεχομένου)
            return oldList.get(oldItemPosition).getText().equals(newList.get(newItemPosition).getText());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            // Ελέγχουμε αν το περιεχόμενο είναι το ίδιο
            ChatMessage oldMsg = oldList.get(oldItemPosition);
            ChatMessage newMsg = newList.get(newItemPosition);
            return oldMsg.getText().equals(newMsg.getText()) && oldMsg.isUser() == newMsg.isUser();
        }
    }
}