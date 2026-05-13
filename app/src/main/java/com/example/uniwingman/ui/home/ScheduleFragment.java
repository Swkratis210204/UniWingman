package com.example.uniwingman.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.uniwingman.R;
import com.example.uniwingman.databinding.FragmentScheduleBinding;
import java.util.Calendar;
import java.util.List;

public class ScheduleFragment extends Fragment {

    private FragmentScheduleBinding binding;
    private ScheduleViewModel viewModel;

    private static final String[] DAYS = {"Δευτέρα","Τρίτη","Τετάρτη","Πέμπτη","Παρασκευή"};
    private static final String[] DAYS_SHORT = {"Δευ","Τρι","Τετ","Πεμ","Παρ"};
    private int selectedDayIndex = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentScheduleBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(ScheduleViewModel.class);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("UniWingmanPrefs", Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", null);

        int dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        if (dow == Calendar.TUESDAY)        selectedDayIndex = 1;
        else if (dow == Calendar.WEDNESDAY) selectedDayIndex = 2;
        else if (dow == Calendar.THURSDAY)  selectedDayIndex = 3;
        else if (dow == Calendar.FRIDAY)    selectedDayIndex = 4;
        else                                selectedDayIndex = 0;

        setupDayTabs();
        setupAddButton();
        observeViewModel();

        if (userId != null) viewModel.load(userId);

        return binding.getRoot();
    }

    private void setupDayTabs() {
        Button[] tabs = {
                binding.btnDayMon, binding.btnDayTue, binding.btnDayWed,
                binding.btnDayThu, binding.btnDayFri
        };
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            tabs[i].setText(DAYS_SHORT[i]);
            tabs[i].setOnClickListener(v -> {
                selectedDayIndex = idx;
                updateDaySelection(tabs, idx);
                viewModel.selectDay(DAYS[idx]);
            });
        }
        updateDaySelection(tabs, selectedDayIndex);
        viewModel.selectDay(DAYS[selectedDayIndex]);
    }

    private void updateDaySelection(Button[] tabs, int selected) {
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setBackgroundColor(i == selected ? 0xFF185FA5 : 0xFFEEEEEE);
            tabs[i].setTextColor(i == selected ? 0xFFFFFFFF : 0xFF444444);
        }
    }

    private void setupAddButton() {
        binding.btnAdd.setOnClickListener(v -> showAddDialog());
    }

    private void showAddDialog() {
        String[] options = {"Μάθημα", "Άλλο"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Προσθήκη")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showAddCourseDialog();
                    else showAddOtherDialog();
                }).show();
    }

    private void showAddCourseDialog() {
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_course, null);

        Spinner spinnerCourse = v.findViewById(R.id.spinnerCourse);
        Spinner spinnerDay    = v.findViewById(R.id.spinnerDay);
        Spinner spinnerStart  = v.findViewById(R.id.spinnerStart);
        Spinner spinnerEnd    = v.findViewById(R.id.spinnerEnd);
        Spinner spinnerType   = v.findViewById(R.id.spinnerType);
        EditText etRoom       = v.findViewById(R.id.etRoom);

        viewModel.getAllCourses().observe(getViewLifecycleOwner(), courses -> {
            if (courses == null) return;
            String[] names = courses.stream()
                    .map(c -> c.title).toArray(String[]::new);
            spinnerCourse.setAdapter(new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_dropdown_item, names));
        });

        spinnerDay.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, DAYS));
        spinnerDay.setSelection(selectedDayIndex);

        String[] hours = {"9","11","1","3","5","7"};
        spinnerStart.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, hours));
        spinnerEnd.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, hours));

        String[] types = {"Διάλεξη","Εργαστήριο","Φροντιστήριο"};
        spinnerType.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, types));

        new AlertDialog.Builder(requireContext())
                .setTitle("Προσθήκη Μαθήματος")
                .setView(v)
                .setPositiveButton("Προσθήκη", (d, w) -> {
                    List<CourseItem> courses = viewModel.getAllCourses().getValue();
                    if (courses == null || courses.isEmpty()) return;

                    CourseItem selected = courses.get(spinnerCourse.getSelectedItemPosition());
                    String day  = DAYS[spinnerDay.getSelectedItemPosition()];
                    int start   = Integer.parseInt(hours[spinnerStart.getSelectedItemPosition()]);
                    int end     = Integer.parseInt(hours[spinnerEnd.getSelectedItemPosition()]);
                    String type = types[spinnerType.getSelectedItemPosition()];
                    String room = etRoom.getText().toString().trim();

                    viewModel.addUserSlot(new UserSlot(
                            selected.title, day, start, end, type, room, false));
                })
                .setNegativeButton("Άκυρο", null)
                .show();
    }

    private void showAddOtherDialog() {
        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_other, null);

        EditText etTitle     = v.findViewById(R.id.etTitle);
        EditText etComment   = v.findViewById(R.id.etComment);
        Spinner spinnerDay   = v.findViewById(R.id.spinnerDay);
        Spinner spinnerStart = v.findViewById(R.id.spinnerStart);
        Spinner spinnerEnd   = v.findViewById(R.id.spinnerEnd);

        spinnerDay.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, DAYS));
        spinnerDay.setSelection(selectedDayIndex);

        String[] hours = {"9","11","1","3","5","7"};
        spinnerStart.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, hours));
        spinnerEnd.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, hours));

        new AlertDialog.Builder(requireContext())
                .setTitle("Προσθήκη Άλλου")
                .setView(v)
                .setPositiveButton("Προσθήκη", (d, w) -> {
                    String title   = etTitle.getText().toString().trim();
                    String comment = etComment.getText().toString().trim();
                    String day     = DAYS[spinnerDay.getSelectedItemPosition()];
                    int start      = Integer.parseInt(hours[spinnerStart.getSelectedItemPosition()]);
                    int end        = Integer.parseInt(hours[spinnerEnd.getSelectedItemPosition()]);

                    if (title.isEmpty()) return;
                    viewModel.addUserSlot(new UserSlot(
                            title, day, start, end, "Άλλο", comment, true));
                })
                .setNegativeButton("Άκυρο", null)
                .show();
    }

    private void observeViewModel() {
        viewModel.getCurrentDaySlots().observe(getViewLifecycleOwner(), rows -> {
            if (rows == null) return;
            binding.scheduleContainer.removeAllViews();
            for (SlotRow row : rows) {
                View rowView = buildSlotRow(row);
                binding.scheduleContainer.addView(rowView);
            }
        });
    }

    private View buildSlotRow(SlotRow row) {
        LinearLayout rowLayout = new LinearLayout(requireContext());
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        rowLayout.setPadding(0, 4, 0, 4);

        TextView tvTime = new TextView(requireContext());
        tvTime.setText(formatTime(row.startHour) + "\n" + formatTime(row.endHour));
        tvTime.setTextSize(11);
        tvTime.setTextColor(0xFF888888);
        tvTime.setGravity(android.view.Gravity.CENTER);
        tvTime.setLayoutParams(new LinearLayout.LayoutParams(56, ViewGroup.LayoutParams.MATCH_PARENT));
        rowLayout.addView(tvTime);

        View line = new View(requireContext());
        line.setBackgroundColor(0xFFDDDDDD);
        line.setLayoutParams(new LinearLayout.LayoutParams(1, ViewGroup.LayoutParams.MATCH_PARENT));
        rowLayout.addView(line);

        LinearLayout slotsLayout = new LinearLayout(requireContext());
        slotsLayout.setOrientation(LinearLayout.HORIZONTAL);
        slotsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        for (SlotCard card : row.cards) {
            View cardView = buildCard(card);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            cardParams.setMargins(4, 0, 4, 0);
            cardView.setLayoutParams(cardParams);
            slotsLayout.addView(cardView);
        }
        rowLayout.addView(slotsLayout);

        return rowLayout;
    }

    private View buildCard(SlotCard card) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(12, 10, 12, 10);
        layout.setBackgroundColor(card.color);

        TextView tvName = new TextView(requireContext());
        tvName.setText(card.name);
        tvName.setTextColor(0xFFFFFFFF);
        tvName.setTextSize(12);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvName);

        if (card.room != null && !card.room.isEmpty()) {
            TextView tvRoom = new TextView(requireContext());
            tvRoom.setText("📍 " + card.room);
            tvRoom.setTextColor(0xDDFFFFFF);
            tvRoom.setTextSize(10);
            layout.addView(tvRoom);
        }

        TextView tvType = new TextView(requireContext());
        tvType.setText(card.type);
        tvType.setTextColor(0xFFFFFFFF);
        tvType.setTextSize(10);
        tvType.setBackgroundColor(0x33000000);
        tvType.setPadding(6, 2, 6, 2);
        layout.addView(tvType);

        if (card.isDeletable) {
            TextView tvDelete = new TextView(requireContext());
            tvDelete.setText("✕");
            tvDelete.setTextColor(0xFFFFFFFF);
            tvDelete.setTextSize(14);
            tvDelete.setPadding(0, 4, 0, 0);
            tvDelete.setOnClickListener(vv -> viewModel.deleteUserSlot(card.slotId));
            layout.addView(tvDelete);
        }

        return layout;
    }

    private String formatTime(int h) { return h + ":00"; }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}