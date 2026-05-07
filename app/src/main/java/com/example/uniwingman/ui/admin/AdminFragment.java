package com.example.uniwingman.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_admin, container, false);

        tvTotalUsers = root.findViewById(R.id.tvTotalUsers);
        rvRecentUsers = root.findViewById(R.id.rvRecentUsers);
        btnSendNotification = root.findViewById(R.id.btnSendNotification);

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
            tvTotalUsers.setText(String.valueOf(count));
        });

        // 2. ΝΕΟ: Παρατήρηση της πραγματικής λίστας χρηστών από τη Βάση
        adminViewModel.getRecentUsers().observe(getViewLifecycleOwner(), users -> {
            if (users != null) {
                userAdapter.setUsers(users); // Δίνουμε τους πραγματικούς χρήστες στον Adapter!
            }
        });

        // 3. Παρατήρηση για τυχόν σφάλματα δικτύου
        adminViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            }
        });

        // Κουμπί δράσης
        btnSendNotification.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Η λειτουργία Push Notification θα προστεθεί σύντομα!", Toast.LENGTH_SHORT).show();
        });

        // Ξεκινάμε τη φόρτωση όλων των στατιστικών!
        adminViewModel.loadStatistics();
    }
}