package com.example.uniwingman.ui.auth; // Άλλαξέ το αν χρειάζεται

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.uniwingman.R;
import com.example.uniwingman.data.SupabaseAuth;
import com.google.android.material.textfield.TextInputEditText;

public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private Button registerButton;
    private Button goToLoginButton;
    private SupabaseAuth supabaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        emailEditText = findViewById(R.id.signUpEmailEditText);
        passwordEditText = findViewById(R.id.signUpPasswordEditText);
        registerButton = findViewById(R.id.registerButton);
        goToLoginButton = findViewById(R.id.goToLoginButton);

        supabaseAuth = new SupabaseAuth();

        registerButton.setOnClickListener(v -> performSignUp());
        goToLoginButton.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void performSignUp() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.length() < 6) {
            Toast.makeText(this, "Βάλε έγκυρο email και κωδικό 6+ χαρακτήρων.", Toast.LENGTH_SHORT).show();
            return;
        }

        registerButton.setEnabled(false);

        supabaseAuth.signUp(email, password, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String resultMsg) {
                runOnUiThread(() -> {
                    Toast.makeText(SignUpActivity.this, "Η εγγραφή πέτυχε! Τώρα κάνε σύνδεση.", Toast.LENGTH_LONG).show();
                    // ΑΥΤΟ ΚΑΝΕΙ ΤΟ REDIRECT: Κλείνει την οθόνη Εγγραφής και σε γυρνάει πίσω στο Login!
                    finish();
                });
            }

            @Override
            public void onError(String errorMsg) {
                runOnUiThread(() -> {
                    registerButton.setEnabled(true);
                    Toast.makeText(SignUpActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}