package com.example.uniwingman.ui.profile;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.uniwingman.R;
import com.example.uniwingman.model.CourseItem;

import java.util.List;

public class CourseCardAdapter extends RecyclerView.Adapter<CourseCardAdapter.ViewHolder> {

    public interface OnCourseClickListener {
        void onCourseClick(CourseItem item);
    }

    private final List<CourseItem>      courses;
    private final int                   userCurrentSemester;
    private final OnCourseClickListener listener;

    public CourseCardAdapter(List<CourseItem> courses, int userCurrentSemester,
                             OnCourseClickListener listener) {
        this.courses             = courses;
        this.userCurrentSemester = userCurrentSemester;
        this.listener            = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        CourseItem item = courses.get(position);

        h.tvCode.setText(item.code);
        h.tvTitle.setText(item.title);
        h.tvEcts.setText((int) item.ects + " ECTS");
        h.tvWhenTaken.setText(item.getWhenTaken());
        h.tvAvailability.setText(item.getAvailability(userCurrentSemester));

// Reset
        h.tvGrade.setText("—");
        h.viewBar.setBackgroundColor(Color.parseColor("#888888"));
        android.util.Log.d("Adapter", "status='" + item.status + "'");

        switch (item.status) {
            case "passed":
                h.viewBar.setBackgroundColor(Color.parseColor("#4CAF50"));
                h.tvGrade.setBackgroundResource(R.drawable.bg_grade_circle_green);
                if (item.grade != null) {
                    h.tvGrade.setText(formatGrade(item.grade));
                } else {
                    h.tvGrade.setText("✓");
                }
                break;

            case "in_progress":
                h.viewBar.setBackgroundColor(Color.parseColor("#3D5AFE"));
                h.tvGrade.setBackgroundResource(R.drawable.bg_grade_circle_blue);
                h.tvGrade.setText("…");
                break;

            case "failed":
                h.viewBar.setBackgroundColor(Color.parseColor("#F44336"));
                h.tvGrade.setBackgroundResource(R.drawable.bg_grade_circle_red);
                if (item.grade != null) {
                    h.tvGrade.setText(formatGrade(item.grade));
                } else {
                    h.tvGrade.setText("✗");
                }
                break;

            default:
                h.viewBar.setBackgroundColor(Color.parseColor("#888888"));
                h.tvGrade.setBackgroundResource(R.drawable.bg_grade_circle);
                h.tvGrade.setText("—");
                break;
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCourseClick(item);
        });
    }

    private String formatGrade(float grade) {
        if (grade == (int) grade) return String.valueOf((int) grade);
        return String.valueOf(grade);
    }

    @Override
    public int getItemCount() { return courses.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View     viewBar;
        TextView tvCode, tvTitle, tvEcts, tvWhenTaken, tvAvailability, tvGrade;

        ViewHolder(@NonNull View v) {
            super(v);
            viewBar        = v.findViewById(R.id.viewStatusBar);
            tvCode         = v.findViewById(R.id.tvCourseCode);
            tvTitle        = v.findViewById(R.id.tvCourseTitle);
            tvEcts         = v.findViewById(R.id.tvEcts);
            tvWhenTaken    = v.findViewById(R.id.tvWhenTaken);
            tvAvailability = v.findViewById(R.id.tvAvailability);
            tvGrade        = v.findViewById(R.id.tvGrade);
        }
    }
}