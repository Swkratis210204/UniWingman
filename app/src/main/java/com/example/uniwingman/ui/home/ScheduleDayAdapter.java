package com.example.uniwingman.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.uniwingman.R;
import java.util.List;

public class ScheduleDayAdapter extends RecyclerView.Adapter<ScheduleDayAdapter.DayVH> {
    private final List<ScheduleDay> days;
    public ScheduleDayAdapter(List<ScheduleDay> days) { this.days = days; }

    @NonNull @Override
    public DayVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_day, parent, false);
        return new DayVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DayVH h, int pos) {
        ScheduleDay day = days.get(pos);
        h.tvDay.setText(day.day);
        h.slotsContainer.removeAllViews();
        for (CourseSlot slot : day.slots) {
            View slotView = LayoutInflater.from(h.itemView.getContext())
                    .inflate(R.layout.item_course_slot, h.slotsContainer, false);
            TextView tvName  = slotView.findViewById(R.id.tvCourseName);
            TextView tvTime  = slotView.findViewById(R.id.tvCourseTime);
            TextView tvType  = slotView.findViewById(R.id.tvCourseType);
            tvName.setText(slot.name);
            tvTime.setText(slot.time);
            tvType.setText(slot.getTypeLabel());
            slotView.setBackgroundColor(slot.getTypeColor());
            h.slotsContainer.addView(slotView);
        }
    }

    @Override public int getItemCount() { return days.size(); }

    static class DayVH extends RecyclerView.ViewHolder {
        TextView tvDay;
        LinearLayout slotsContainer;
        DayVH(View v) {
            super(v);
            tvDay = v.findViewById(R.id.tvDay);
            slotsContainer = v.findViewById(R.id.slotsContainer);
        }
    }
}
