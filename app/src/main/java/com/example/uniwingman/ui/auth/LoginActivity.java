package com.example.uniwingman.ui.auth; // Άλλαξέ το αν χρειάζεται

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.uniwingman.MainActivity;
import com.example.uniwingman.R;
import com.example.uniwingman.data.SupabaseAuth;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private Button loginButton;
    private TextView goToSignUpText;
    private TextView forgotPasswordText;
    private SupabaseAuth supabaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        goToSignUpText = findViewById(R.id.goToSignUpText);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);

        supabaseAuth = new SupabaseAuth();

        // Λειτουργία Login
        loginButton.setOnClickListener(v -> performLogin());

        // Λειτουργία μετάβασης σε Εγγραφή
        goToSignUpText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        // Λειτουργία "Ξέχασες τον κωδικό"
        forgotPasswordText.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Γράψε πρώτα το email σου στο πεδίο από πάνω!", Toast.LENGTH_LONG).show();
                return;
            }

            // Καλούμε την Supabase για επαναφορά χρησιμοποιώντας το AuthCallback
            supabaseAuth.resetPassword(email, new SupabaseAuth.AuthCallback() {
                @Override
                public void onSuccess(String result) {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Το email στάλθηκε! Έλεγξε τα εισερχόμενά σου.", Toast.LENGTH_LONG).show();
                    });
                }

                @Override
                public void onError(String errorMsg) {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Σφάλμα! Βεβαιώσου ότι το email είναι σωστό. (" + errorMsg + ")", Toast.LENGTH_LONG).show();
                    });
                }
            });
        });
    }

    private void performLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Συμπλήρωσε και τα δύο πεδία.", Toast.LENGTH_SHORT).show();
            return;
        }

        loginButton.setEnabled(false);

        supabaseAuth.login(email, password, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String token) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Επιτυχής Σύνδεση!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish(); // Κλείνουμε το Login
                });
            }

            @Override
            public void onError(String errorMsg) {
                runOnUiThread(() -> {
                    loginButton.setEnabled(true);
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}