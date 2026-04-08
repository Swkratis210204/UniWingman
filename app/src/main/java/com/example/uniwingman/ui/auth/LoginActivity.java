package com.example.uniwingman.ui.auth;

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

        loginButton.setOnClickListener(v -> performLogin());

        goToSignUpText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });

        forgotPasswordText.setOnClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Η επαναφορά κωδικού είναι προσωρινά μη διαθέσιμη.", Toast.LENGTH_SHORT).show();
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
            public void onSuccess(String userId) {
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Επιτυχής Σύνδεση!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
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