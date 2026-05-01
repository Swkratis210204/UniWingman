package com.example.uniwingman.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.uniwingman.databinding.FragmentProfileBinding;
import com.example.uniwingman.ui.auth.LoginActivity;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == getActivity().RESULT_OK
                                && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            requireActivity()
                                    .getSharedPreferences("UniWingmanPrefs", android.content.Context.MODE_PRIVATE)
                                    .edit()
                                    .putString("profileImageUri", imageUri.toString())
                                    .apply();
                            loadProfileImage(imageUri);
                        }
                    }
            );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("UniWingmanPrefs", android.content.Context.MODE_PRIVATE);
        String username = prefs.getString("username", "Φοιτητής");
        String email    = prefs.getString("email", "");
        String userId   = prefs.getString("userId", null);

        setupHeader(username, email);
        setupMenuRows(userId);
        setupLogout(prefs);

        String savedUri = prefs.getString("profileImageUri", null);
        if (savedUri != null) {
            loadProfileImage(Uri.parse(savedUri));
        }

        if (userId != null) {
            viewModel.loadProfileStats(userId);
        }

        observeViewModel();

        return binding.getRoot();
    }

    private void setupHeader(String username, String email) {
        binding.tvUsername.setText(username);
        binding.tvEmail.setText(email);
        binding.tvAvatar.setText(getInitials(username));
        binding.tvAvatar.setOnClickListener(v -> pickImage());
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void loadProfileImage(Uri uri) {
        binding.ivAvatar.setImageURI(uri);
        binding.tvAvatar.setVisibility(View.GONE);
        binding.ivAvatar.setVisibility(View.VISIBLE);
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    private void navigateToCourses(String status) {
        Intent intent = new Intent(requireActivity(), CoursesActivity.class);
        intent.putExtra(CoursesActivity.ARG_STATUS, status);
        startActivity(intent);
    }

    private void navigateToAddCourse(String userId) {
        Intent intent = new Intent(requireActivity(), AddCourseActivity.class);
        intent.putExtra(AddCourseActivity.EXTRA_USER_ID, userId);
        startActivity(intent);
    }

    private void setupMenuRows(String userId) {
        binding.rowDeclared.setOnClickListener(v -> navigateToCourses("in_progress"));
        binding.rowPassed.setOnClickListener(v   -> navigateToCourses("passed"));
        binding.rowFailed.setOnClickListener(v   -> navigateToCourses("failed"));

        binding.rowEditProfile.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), EditProfileActivity.class)));

        binding.rowAddCourse.setOnClickListener(v -> {
            if (userId != null) navigateToAddCourse(userId);
            else Toast.makeText(getContext(), "Δεν βρέθηκε χρήστης.", Toast.LENGTH_SHORT).show();
        });

        binding.rowSettings.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), SettingsActivity.class)));

        binding.rowNotifications.setOnClickListener(v ->
                Toast.makeText(getContext(), "Ειδοποιήσεις — Σύντομα!", Toast.LENGTH_SHORT).show());

        binding.rowHelp.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), HelpActivity.class)));
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
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("UniWingmanPrefs", android.content.Context.MODE_PRIVATE);
        String userId = prefs.getString("userId", null);
        if (userId != null) {
            viewModel.loadProfileStats(userId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}