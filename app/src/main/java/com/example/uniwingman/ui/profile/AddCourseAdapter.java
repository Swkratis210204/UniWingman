package com.example.uniwingman.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uniwingman.R;

import java.util.List;

public class AddCourseAdapter extends RecyclerView.Adapter<AddCourseAdapter.ViewHolder> {

    public interface OnAddClickListener {
        void onAdd(AddCourseActivity.AddCourseItem item);
    }

    private final List<AddCourseActivity.AddCourseItem> items;
    private final OnAddClickListener listener;

    public AddCourseAdapter(List<AddCourseActivity.AddCourseItem> items, OnAddClickListener listener) {
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_add_course, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AddCourseActivity.AddCourseItem item = items.get(position);
        h.tvCode.setText(item.code);
        h.tvTitle.setText(item.title.trim());
        h.tvEcts.setText((int) item.ects + " ECTS");
        h.tvSemester.setText("Εξ. " + item.semester);
        h.tvType.setText(item.type);
        h.btnAdd.setOnClickListener(v -> {
            if (listener != null) listener.onAdd(item);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode, tvTitle, tvEcts, tvSemester, tvType;
        Button   btnAdd;

        ViewHolder(@NonNull View v) {
            super(v);
            tvCode     = v.findViewById(R.id.tvAddCode);
            tvTitle    = v.findViewById(R.id.tvAddTitle);
            tvEcts     = v.findViewById(R.id.tvAddEcts);
            tvSemester = v.findViewById(R.id.tvAddSemester);
            tvType     = v.findViewById(R.id.tvAddType);
            btnAdd     = v.findViewById(R.id.btnAdd);
        }
    }
}