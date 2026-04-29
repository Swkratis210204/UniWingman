package com.example.uniwingman.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.uniwingman.R;
import com.example.uniwingman.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "UniWingmanPrefs";
    private static final String KEY_ECTS = "ects_done";

    private TextView tvAvatar, tvUsername, tvEmail, tvAm;
    private ProgressBar progressEcts;
    private TextView tvEctsCount, tvEctsDone, tvEctsRemaining, tvEctsPercent;
    private TextInputEditText etEctsInput;
    private MaterialButton btnUpdateEcts;
    private TextView tvSemester, tvGpa;
    private MaterialButton btnLogout;

    private SharedPreferences prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        tvAvatar        = root.findViewById(R.id.tv_avatar);
        tvUsername      = root.findViewById(R.id.tv_username);
        tvEmail         = root.findViewById(R.id.tv_email);
        tvAm            = root.findViewById(R.id.tv_am);
        progressEcts    = root.findViewById(R.id.progress_ects);
        tvEctsCount     = root.findViewById(R.id.tv_ects_count);
        tvEctsDone      = root.findViewById(R.id.tv_ects_done);
        tvEctsRemaining = root.findViewById(R.id.tv_ects_remaining);
        tvEctsPercent   = root.findViewById(R.id.tv_ects_percent);
        etEctsInput     = root.findViewById(R.id.et_ects_input);
        btnUpdateEcts   = root.findViewById(R.id.btn_update_ects);
        tvSemester      = root.findViewById(R.id.tv_semester);
        tvGpa           = root.findViewById(R.id.tv_gpa);
        btnLogout       = root.findViewById(R.id.btn_logout);

        loadUserInfo();
        loadEcts();

        btnUpdateEcts.setOnClickListener(v -> {
            String input = etEctsInput.getText() != null
                    ? etEctsInput.getText().toString().trim() : "";
            if (input.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Εισάγετε αριθμό ECTS", Toast.LENGTH_SHORT).show();
                return;
            }
            int ects = Integer.parseInt(input);
            if (ects < 0 || ects > 240) {
                Toast.makeText(requireContext(),
                        "Τα ECTS πρέπει να είναι μεταξύ 0 και 240",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putInt(KEY_ECTS, ects).apply();
            loadEcts();
            etEctsInput.setText("");
            Toast.makeText(requireContext(),
                    "Τα ECTS ενημερώθηκαν!", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return root;
    }

    private void loadUserInfo() {
        String username = prefs.getString("username", "Φοιτητής ΟΠΑ");
        String email    = prefs.getString("email", "—");
        String am       = prefs.getString("am", "—");

        tvUsername.setText(username);
        tvEmail.setText(email);
        tvAm.setText("ΑΜ: " + am);

        if (!username.isEmpty()) {
            tvAvatar.setText(String.valueOf(username.charAt(0)).toUpperCase());
        }

        int ects     = prefs.getInt(KEY_ECTS, 0);
        int semester = Math.min((ects / 30) + 1, 8);
        tvSemester.setText(semester + "ο");
        tvGpa.setText("—");
    }

    private void loadEcts() {
        int done      = prefs.getInt(KEY_ECTS, 0);
        int remaining = 240 - done;
        int percent   = (int) ((done / 240f) * 100);

        progressEcts.setProgress(done);
        tvEctsCount.setText(done + " / 240");
        tvEctsDone.setText(String.valueOf(done));
        tvEctsRemaining.setText(String.valueOf(remaining));
        tvEctsPercent.setText(percent + "%");

        int semester = Math.min((done / 30) + 1, 8);
        tvSemester.setText(semester + "ο");
    }
}