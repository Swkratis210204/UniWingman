package com.example.uniwingman;

import android.content.Intent;
import android.os.Bundle;

import com.example.uniwingman.ui.auth.OnboardingActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.uniwingman.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. CHECK FIRST
        if (!OnboardingActivity.isOnboardingDone(this)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        // 2. INITIALIZE UI ONLY IF DONE
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Inside MainActivity onCreate
        if (!OnboardingActivity.isOnboardingDone(this)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
    }

}