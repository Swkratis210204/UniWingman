package com.example.uniwingman;

import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;
import com.example.uniwingman.databinding.ActivityMainBinding;
import com.example.uniwingman.ui.auth.OnboardingActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!OnboardingActivity.isOnboardingDone(this)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- ΕΛΕΓΧΟΣ ΚΑΙ ΕΜΦΑΝΙΣΗ ADMIN MENU (ΝΕΟΣ ΚΩΔΙΚΑΣ) ---
        // 1. Διαβάζουμε αν ο χρήστης είναι Admin από τα SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("UniWingmanPrefs", MODE_PRIVATE);
        boolean isAdmin = prefs.getBoolean("IS_ADMIN", false);

        // 2. Επειδή χρησιμοποιείς ViewBinding, παίρνουμε το μενού απευθείας από το binding.navView
        android.view.Menu menu = binding.navView.getMenu();
        android.view.MenuItem adminItem = menu.findItem(R.id.navigation_admin);

        // 3. Αν βρεθεί το κουμπί στο XML, του αλλάζουμε την ορατότητα ανάλογα με το αν είναι Admin
        if (adminItem != null) {
            adminItem.setVisible(isAdmin);
        }
        // -------------------------------------------------------

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Accessibility: Add haptic feedback and announcements on tab selection
        binding.navView.setOnItemSelectedListener(item -> {
            // Δόνηση για επιβεβαίωση πατήματος (χρήσιμο για τυφλούς χρήστες)
            binding.navView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);

            // Εκτέλεση της πλοήγησης
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);

            if (handled) {
                // Ανακοίνωση αλλαγής ενότητας στο TalkBack
                String title = item.getTitle().toString();
                getWindow().getDecorView().announceForAccessibility("Μετάβαση στην ενότητα: " + title);
            }
            return handled;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}