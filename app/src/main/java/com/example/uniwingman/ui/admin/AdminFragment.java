package com.example.uniwingman.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.uniwingman.R;

public class AdminFragment extends Fragment {

    private AdminViewModel adminViewModel;
    private TextView tvTotalUsers;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Συνδέουμε τον κώδικα με το XML layout που φτιάξαμε
        View root = inflater.inflate(R.layout.fragment_admin, container, false);

        // Βρίσκουμε το TextView με βάση το ID του
        tvTotalUsers = root.findViewById(R.id.tvTotalUsers);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ζητάμε από το Android να μας δώσει το ViewModel αυτής της οθόνης
        adminViewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        // Παρατηρούμε (Observe) τα δεδομένα. Όταν αλλάξουν, θα τρέξει αυτός ο κώδικας
        adminViewModel.getTotalUsers().observe(getViewLifecycleOwner(), count -> {
            tvTotalUsers.setText(String.valueOf(count));
        });

        // Παρατηρούμε για τυχόν σφάλματα και τα δείχνουμε με ένα Toast (αναδυόμενο μήνυμα)
        adminViewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(getContext(), "Σφάλμα: " + error, Toast.LENGTH_LONG).show();
            }
        });

        // Δίνουμε το σήμα στο ViewModel να ξεκινήσει να φορτώνει τα δεδομένα από το ίντερνετ
        adminViewModel.loadStatistics();
    }
}