package com.example.uniwingman.ui.home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.uniwingman.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("UniWingmanPrefs", android.content.Context.MODE_PRIVATE);
        String username = prefs.getString("username", "Φοιτητή");
        String userId   = prefs.getString("userId", null);

        binding.tvWelcome.setText("Καλωσόρισες, " + username);

        binding.rvSchedule.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvTasks.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));

        observeViewModel();

        if (userId != null) {
            viewModel.load(userId);
        }

        return binding.getRoot();
    }

    private void observeViewModel() {
        viewModel.getGpa().observe(getViewLifecycleOwner(), gpa ->
                binding.tvGpa.setText(gpa != null ? gpa : "—"));

        viewModel.getStreak().observe(getViewLifecycleOwner(), streak ->
                binding.tvStreak.setText(streak != null ? String.valueOf(streak) : "0"));

        viewModel.getScheduleDays().observe(getViewLifecycleOwner(), days -> {
            if (days != null) {
                binding.rvSchedule.setAdapter(new ScheduleDayAdapter(days));
            }
        });

        viewModel.getTasks().observe(getViewLifecycleOwner(), tasks -> {
            if (tasks != null) {
                binding.rvTasks.setAdapter(new TaskAdapter(tasks));
            }
        });

        viewModel.getNotifications().observe(getViewLifecycleOwner(), notifs -> {
            if (notifs != null) {
                binding.rvNotifications.setAdapter(new NotificationAdapter(notifs));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}