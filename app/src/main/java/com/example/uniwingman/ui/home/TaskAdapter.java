package com.example.uniwingman.ui.home;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.uniwingman.R;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {
    private final List<TaskItem> tasks;
    public TaskAdapter(List<TaskItem> tasks) { this.tasks = tasks; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        TaskItem t = tasks.get(pos);
        h.tvCourse.setText(t.course);
        h.tvTitle.setText(t.title);
        h.tvDeadline.setText("Προθεσμία: " + t.deadline);
        h.tvDaysLeft.setText(t.daysLeft + " μέρες");

        if (t.daysLeft < 7) {
            // Επείγον — κόκκινο
            h.itemView.setBackgroundColor(0xFFFEECEC);
            h.viewBar.setBackgroundColor(0xFFE24B4A);
            h.tvDaysLeft.setBackgroundTintList(ColorStateList.valueOf(0xFFE24B4A));
        } else {
            // Μη επείγον — μωβ/μπλε
            h.itemView.setBackgroundColor(0xFFEEEEFF);
            h.viewBar.setBackgroundColor(0xFF7C6FCD);
            h.tvDaysLeft.setBackgroundTintList(ColorStateList.valueOf(0xFF7C6FCD));
        }
    }

    @Override public int getItemCount() { return tasks.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvCourse, tvTitle, tvDeadline, tvDaysLeft;
        View viewBar;
        VH(View v) {
            super(v);
            tvCourse   = v.findViewById(R.id.tvTaskCourse);
            tvTitle    = v.findViewById(R.id.tvTaskTitle);
            tvDeadline = v.findViewById(R.id.tvTaskDeadline);
            tvDaysLeft = v.findViewById(R.id.tvDaysLeft);
            viewBar    = v.findViewById(R.id.viewTaskBar);
        }
    }
}