package com.example.uniwingman.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.uniwingman.databinding.FragmentProfileBinding;
import com.example.uniwingman.ui.auth.LoginActivity;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // Φόρτωσε username/email από SharedPreferences
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("UniWingmanPrefs", android.content.Context.MODE_PRIVATE);
        String username = prefs.getString("username", "Φοιτητής");
        String email    = prefs.getString("email", "");
        String userId   = prefs.getString("userId", null);

        setupHeader(username, email);
        setupMenuRows();
        setupLogout(prefs);

        if (userId != null) {
            viewModel.loadProfileStats(userId);
        }

        observeViewModel();

        return binding.getRoot();
    }

    private void setupHeader(String username, String email) {
        binding.tvUsername.setText(username);
        binding.tvEmail.setText(email);

        // Initials avatar (π.χ. "Γιάννης Κωνσταντίνου" → "ΓΚ")
        String initials = getInitials(username);
        binding.tvAvatar.setText(initials);
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private void setupMenuRows() {
        // Placeholder click listeners — θα υλοποιηθούν αργότερα
        binding.rowDeclared.setOnClickListener(v ->
                Toast.makeText(getContext(), "Δηλωμένα Μαθήματα — Σύντομα!", Toast.LENGTH_SHORT).show());

        binding.rowPassed.setOnClickListener(v ->
                Toast.makeText(getContext(), "Περασμένα Μαθήματα — Σύντομα!", Toast.LENGTH_SHORT).show());

        binding.rowFailed.setOnClickListener(v ->
                Toast.makeText(getContext(), "Κομμένα Μαθήματα — Σύντομα!", Toast.LENGTH_SHORT).show());

        binding.rowSettings.setOnClickListener(v ->
                Toast.makeText(getContext(), "Ρυθμίσεις — Σύντομα!", Toast.LENGTH_SHORT).show());

        binding.rowNotifications.setOnClickListener(v ->
                Toast.makeText(getContext(), "Ειδοποιήσεις — Σύντομα!", Toast.LENGTH_SHORT).show());

        binding.rowEditProfile.setOnClickListener(v ->
                Toast.makeText(getContext(), "Επεξεργασία Προφίλ — Σύντομα!", Toast.LENGTH_SHORT).show());

        binding.rowHelp.setOnClickListener(v ->
                Toast.makeText(getContext(), "Βοήθεια & Υποστήριξη — Σύντομα!", Toast.LENGTH_SHORT).show());
    }

    private void setupLogout(SharedPreferences prefs) {
        binding.rowLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void observeViewModel() {
        viewModel.getGpa().observe(getViewLifecycleOwner(), gpa -> {
            if (gpa != null) binding.tvGpa.setText(gpa);
        });

        viewModel.getStreak().observe(getViewLifecycleOwner(), streak -> {
            if (streak != null) binding.tvStreak.setText(String.valueOf(streak));
        });

        viewModel.getTotalCourses().observe(getViewLifecycleOwner(), total -> {
            if (total != null) binding.tvTotalCourses.setText(String.valueOf(total));
        });

        viewModel.getDeclaredCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) binding.badgeDeclared.setText(String.valueOf(count));
        });

        viewModel.getPassedCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) binding.badgePassed.setText(String.valueOf(count));
        });

        viewModel.getFailedCount().observe(getViewLifecycleOwner(), count -> {
            if (count != null) binding.badgeFailed.setText(String.valueOf(count));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}