package com.example.uniwingman.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.uniwingman.R;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {
    private final List<NotificationItem> items;
    public NotificationAdapter(List<NotificationItem> items) { this.items = items; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        h.tvMessage.setText(items.get(pos).message);
        h.tvTime.setText(items.get(pos).time);
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        VH(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvNotifMessage);
            tvTime    = v.findViewById(R.id.tvNotifTime);
        }
    }
}
