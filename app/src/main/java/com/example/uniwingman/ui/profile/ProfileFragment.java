package com.example.uniwingman.ui.profile;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.uniwingman.R;
import com.example.uniwingman.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ProfileFragment extends Fragment {

    private static final String PREFS_NAME = "UniWingmanPrefs";
    private static final String KEY_ECTS   = "ects_done";

    private TextView tvAvatar, tvUsername, tvEmail, tvAm;
    private TextView tvEctsDone, tvEctsCount, tvSemester, tvGpa;
    private ProgressBar progressEcts;
    private TextInputEditText etEctsInput;
    private MaterialButton btnUpdateEcts, btnEditProfile, btnOptions;

    private SharedPreferences prefs;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        tvAvatar      = root.findViewById(R.id.tv_avatar);
        tvUsername    = root.findViewById(R.id.tv_username);
        tvEmail       = root.findViewById(R.id.tv_email);
        tvAm          = root.findViewById(R.id.tv_am);
        tvEctsDone    = root.findViewById(R.id.tv_ects_done);
        tvEctsCount   = root.findViewById(R.id.tv_ects_count);
        tvSemester    = root.findViewById(R.id.tv_semester);
        tvGpa         = root.findViewById(R.id.tv_gpa);
        progressEcts  = root.findViewById(R.id.progress_ects);
        etEctsInput   = root.findViewById(R.id.et_ects_input);
        btnUpdateEcts = root.findViewById(R.id.btn_update_ects);
        btnEditProfile = root.findViewById(R.id.btn_edit_profile);
        btnOptions    = root.findViewById(R.id.btn_options);

        loadUserInfo();
        loadEcts();

        // Κουμπί Επεξεργασία
        btnEditProfile.setOnClickListener(v -> showEditDialog());

        // Κουμπί ··· (options)
        btnOptions.setOnClickListener(v -> showOptionsMenu(v));

        // Κουμπί Ενημέρωση ECTS
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
                    "ECTS ενημερώθηκαν!", Toast.LENGTH_SHORT).show();
        });

        return root;
    }

    // ── Dropdown menu ──────────────────────────────────────────
    private void showOptionsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenuInflater().inflate(R.menu.menu_profile_options, popup.getMenu());

        // Κάνε το Αποσύνδεση κόκκινο
        MenuItem logoutItem = popup.getMenu().findItem(R.id.action_logout);
        android.text.SpannableString redTitle = new android.text.SpannableString(logoutItem.getTitle());
        redTitle.setSpan(
                new android.text.style.ForegroundColorSpan(
                        requireContext().getColor(R.color.profile_red)),
                0, redTitle.length(), 0);
        logoutItem.setTitle(redTitle);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit_profile) {
                showEditDialog();
                return true;
            } else if (id == R.id.action_change_password) {
                showChangePasswordDialog();
                return true;
            } else if (id == R.id.action_logout) {
                showLogoutConfirmation();
                return true;
            }
            return false;
        });

        popup.show();
    }

    // ── Dialog Επεξεργασίας Στοιχείων ─────────────────────────
    private void showEditDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_profile, null);

        TextInputEditText etUsername = dialogView.findViewById(R.id.et_edit_username);
        TextInputEditText etEmail    = dialogView.findViewById(R.id.et_edit_email);
        TextInputEditText etAm       = dialogView.findViewById(R.id.et_edit_am);

        // Γέμισε με τρέχουσες τιμές
        etUsername.setText(prefs.getString("username", ""));
        etEmail.setText(prefs.getString("email", ""));
        etAm.setText(prefs.getString("am", ""));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Επεξεργασία στοιχείων")
                .setView(dialogView)
                .setPositiveButton("Αποθήκευση", null)
                .setNegativeButton("Ακύρωση", null)
                .show();

        // Override του positive button για confirmation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newUsername = etUsername.getText() != null
                    ? etUsername.getText().toString().trim() : "";
            String newEmail = etEmail.getText() != null
                    ? etEmail.getText().toString().trim() : "";
            String newAm = etAm.getText() != null
                    ? etAm.getText().toString().trim() : "";

            if (newUsername.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Username και Email είναι υποχρεωτικά",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Confirmation dialog
            new AlertDialog.Builder(requireContext())
                    .setTitle("Επιβεβαίωση αλλαγών")
                    .setMessage("Είσαι σίγουρος ότι θέλεις να αποθηκεύσεις τις αλλαγές;")
                    .setPositiveButton("Ναι, αποθήκευση", (d, which) -> {
                        prefs.edit()
                                .putString("username", newUsername)
                                .putString("email", newEmail)
                                .putString("am", newAm)
                                .apply();
                        loadUserInfo();
                        dialog.dismiss();
                        Toast.makeText(requireContext(),
                                "Τα στοιχεία αποθηκεύτηκαν!",
                                Toast.LENGTH_SHORT).show();
                        // TODO: Ενημέρωση Supabase όταν έχουμε access token
                    })
                    .setNegativeButton("Ακύρωση", null)
                    .show();
        });
    }

    // ── Dialog Αλλαγής Κωδικού ────────────────────────────────
    private void showChangePasswordDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Αλλαγή κωδικού")
                .setMessage("Θα σου στείλουμε email για επαναφορά κωδικού στη διεύθυνση:\n\n"
                        + prefs.getString("email", "—"))
                .setPositiveButton("Αποστολή email", (d, which) -> {
                    // TODO: Supabase reset password endpoint
                    Toast.makeText(requireContext(),
                            "Email επαναφοράς στάλθηκε! (σύντομα)",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Ακύρωση", null)
                .show();
    }

    // ── Confirmation Αποσύνδεσης ──────────────────────────────
    private void showLogoutConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Αποσύνδεση")
                .setMessage("Είσαι σίγουρος ότι θέλεις να αποσυνδεθείς;")
                .setPositiveButton("Αποσύνδεση", (d, which) -> {
                    prefs.edit().clear().apply();
                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Ακύρωση", null)
                .show();
    }

    // ── Load data ─────────────────────────────────────────────
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
    }

    private void loadEcts() {
        int done     = prefs.getInt(KEY_ECTS, 0);
        int semester = Math.min((done / 30) + 1, 8);
        int percent  = (int) ((done / 240f) * 100);

        tvEctsDone.setText(String.valueOf(done));
        tvEctsCount.setText(done + " / 240  ·  " + percent + "%");
        tvSemester.setText(semester + "ο");
        progressEcts.setProgress(done);
    }
}