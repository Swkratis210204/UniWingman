package com.example.uniwingman.ui.admin;

import android.app.AlertDialog; // ΝΕΟ import για το αναδυόμενο παράθυρο
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.uniwingman.R;

public class AdminFragment extends Fragment {

    private AdminViewModel adminViewModel;
    private TextView tvTotalUsers;
    private RecyclerView rvRecentUsers;
    private AdminUserAdapter userAdapter;
    private Button btnSendNotification;
    private Button btnGeneralStats; // ΝΕΟ: Το κουμπί των στατιστικών
    private EditText etSearchUser;

    // Μεταβλητές για να κρατάμε τα νούμερα και να τα δείχνουμε στο Dialog
    private int currentTotalUsers = 0;
    private int currentTotalCourses = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admin, container, false);

        tvTotalUsers = root.findViewById(R.id.tvTotalUsers);
        rvRecentUsers = root.findViewById(R.id.rvRecentUsers);
        btnSendNotification = root.findViewById(R.id.btnSendNotification);
        btnGeneralStats = root.findViewById(R.id.btnGeneralStats); // Αρχικοποίηση νέου κουμπιού
        etSearchUser = root.findViewById(R.id.etSearchUser);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        // --- ΡΥΘΜΙΣΗ ΤΗΣ ΛΙΣΤΑΣ ---
        rvRecentUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        userAdapter = new AdminUserAdapter();
        rvRecentUsers.setAdapter(userAdapter);

        // --- ΠΑΡΑΤΗΡΗΣΗ ΔΕΔΟΜΕΝΩΝ ΑΠΟ ΤΟ VIEWMODEL ---

        // 1. Παρατήρηση του συνολικού αριθμού χρηστών
        adminViewModel.getTotalUsers().observe(getViewLifecycleOwner(), count -> {
            currentTotalUsers = count; // Το κρατάμε στη μνήμη για το Dialog
            tvTotalUsers.setText(String.valueOf(count));
        });

        // 2. ΝΕΟ: Παρατήρηση του συνολικού αριθμού μαθημάτων
        adminViewModel.getTotalCourses().observe(getViewLifecycleOwner(), count -> {
            currentTotalCourses = count; // Το κρατάμε στη μνήμη για το Dialog
        });

        // 3. Παρατήρηση της πραγματικής λίστας χρηστών
        adminViewModel.getRecentUsers().observe(getViewLifecycleOwner(), users -> {
            if (users != null) {
                userAdapter.setUsers(users);
            }
        });

        // 4. Σφάλματα δικτύου
        adminViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        // --- ΛΟΓΙΚΗ ΑΝΑΖΗΤΗΣΗΣ (TEXT WATCHER) ---
        etSearchUser.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    adminViewModel.loadStatistics();
                } else {
                    adminViewModel.searchUser(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // --- ΚΟΥΜΠΙΑ ΔΡΑΣΕΩΝ ---

        btnSendNotification.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Η λειτουργία Push Notification θα προστεθεί σύντομα!", Toast.LENGTH_SHORT).show();
        });

        // ΝΕΟ: Όταν πατάμε τα Στατιστικά, ανοίγει το Dialog!
        btnGeneralStats.setOnClickListener(v -> {
            showStatisticsDialog();
        });

        // Ξεκινάμε τη φόρτωση όλων των στατιστικών!
        adminViewModel.loadStatistics();
    }

    /**
     * Εμφανίζει ένα αναδυόμενο παράθυρο (AlertDialog) με τα γενικά στατιστικά
     */
    private void showStatisticsDialog() {
        // Διαμορφώνουμε το κείμενο που θα φαίνεται
        String statsMessage = "📊 Εγγεγραμμένοι Φοιτητές: " + currentTotalUsers + "\n\n"
                + "📚 Συνολικά Μαθήματα: " + currentTotalCourses + "\n\n"
                + "🔥 Ενεργοί Χρήστες τώρα: 2\n\n"
                + "💡 Περισσότερα insights θα προστεθούν σύντομα!";

        // Φτιάχνουμε το παράθυρο (Native Android Dialog)
        new AlertDialog.Builder(getContext())
                .setTitle("Γενικά Στατιστικά")
                .setMessage(statsMessage)
                .setPositiveButton("ΟΚ", (dialog, which) -> dialog.dismiss()) // Κουμπί κλεισίματος
                .show();
    }
}