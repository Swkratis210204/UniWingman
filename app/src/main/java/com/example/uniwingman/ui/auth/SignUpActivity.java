package com.example.uniwingman.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.uniwingman.R;
import com.example.uniwingman.data.SupabaseAuth;
import com.google.android.material.textfield.TextInputEditText;

public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText usernameEditText; // ΝΕΟ ΠΕΔΙΟ
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private Button registerButton;
    private Button goToLoginButton;
    private SupabaseAuth supabaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Σιγουρέψου ότι έφτιαξες αυτό το ID στο activity_sign_up.xml σου
        usernameEditText = findViewById(R.id.signUpUsernameEditText);
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
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty() || email.isEmpty() || password.length() < 6) {
            Toast.makeText(this, "Συμπλήρωσε όλα τα πεδία. Κωδικός 6+ χαρακτήρες.", Toast.LENGTH_SHORT).show();
            return;
        }

        registerButton.setEnabled(false);

        // Καλούμε την ανανεωμένη συνάρτηση που παίρνει και το username
        supabaseAuth.signUp(username, email, password, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String resultMsg) {
                runOnUiThread(() -> {
                    Toast.makeText(SignUpActivity.this, "Η εγγραφή πέτυχε! Τώρα κάνε σύνδεση.", Toast.LENGTH_LONG).show();
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