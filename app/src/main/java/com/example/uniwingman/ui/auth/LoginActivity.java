package com.example.uniwingman.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.uniwingman.MainActivity; // Adjust import if MainActivity is elsewhere
import com.example.uniwingman.R;
import com.example.uniwingman.data.SupabaseAuth; // Adjust to match where you saved SupabaseAuth.java

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button signUpButton;
    private SupabaseAuth supabaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Link the code to the XML views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signUpButton = findViewById(R.id.signUpButton);

        // 2. Initialize our Supabase helper
        supabaseAuth = new SupabaseAuth();

        // 3. Set click listeners for the buttons
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performSignUp();
            }
        });
    }

    private void performLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable buttons so the user doesn't spam click them while loading
        setButtonsEnabled(false);

        supabaseAuth.login(email, password, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String token) {
                runOnUiThread(() -> {
                    setButtonsEnabled(true);
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                    // Navigate to your app's main screen
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish(); // This removes LoginActivity from the back-stack so the user can't hit "back" to return here
                });
            }

            @Override
            public void onError(String errorMsg) {
                runOnUiThread(() -> {
                    setButtonsEnabled(true);
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void performSignUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
            return;
        }

        setButtonsEnabled(false);

        supabaseAuth.signUp(email, password, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String resultMsg) {
                runOnUiThread(() -> {
                    setButtonsEnabled(true);
                    // Show success message (usually telling them to check their email for confirmation)
                    Toast.makeText(LoginActivity.this, resultMsg, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String errorMsg) {
                runOnUiThread(() -> {
                    setButtonsEnabled(true);
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        signUpButton.setEnabled(enabled);
    }
}