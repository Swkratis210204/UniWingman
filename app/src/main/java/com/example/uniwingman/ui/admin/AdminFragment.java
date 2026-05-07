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
import java.util.ArrayList;
import java.util.List;

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

        // --- ΡΥΘΜΙΣΗ ΤΗΣ ΛΙΣΤΑΣ (RecyclerView) ---
        // 1. Λέμε στη λίστα να δείχνει τα στοιχεία από πάνω προς τα κάτω
        rvRecentUsers.setLayoutManager(new LinearLayoutManager(getContext()));

        // 2. Δημιουργούμε τον Adapter που φτιάξαμε πριν
        userAdapter = new AdminUserAdapter();
        rvRecentUsers.setAdapter(userAdapter);

        // 3. Βάζουμε προσωρινά (Mock) δεδομένα για να δούμε αν δουλεύει το UI
        loadMockUsers();

        // --- ΠΑΡΑΤΗΡΗΣΗ ΔΕΔΟΜΕΝΩΝ ΑΠΟ ΤΟ VIEWMODEL ---
        adminViewModel.getTotalUsers().observe(getViewLifecycleOwner(), count -> {
            tvTotalUsers.setText(String.valueOf(count));
        });

        adminViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), "Σφάλμα: " + error, Toast.LENGTH_LONG).show();
            }
        });

        // Κουμπί δράσης (Mock)
        btnSendNotification.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Η λειτουργία Push Notification θα προστεθεί σύντομα!", Toast.LENGTH_SHORT).show();
        });

        adminViewModel.loadStatistics();
    }

    // Μέθοδος για να γεμίσουμε τη λίστα με ψεύτικους χρήστες προσωρινά
    private void loadMockUsers() {
        List<AdminUserItem> mockList = new ArrayList<>();
        mockList.add(new AdminUserItem("maria_p", "maria@aueb.gr"));
        mockList.add(new AdminUserItem("nikos_k", "nikos.k@gmail.com"));
        mockList.add(new AdminUserItem("eleni99", "eleni.pappas@yahoo.com"));
        mockList.add(new AdminUserItem("giorgos_dev", "giorgos.dev@aueb.gr"));

        userAdapter.setUsers(mockList);
    }
}