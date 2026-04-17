package com.example.uniwingman.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
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

        emailEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                if (!email.isEmpty() && !password.isEmpty()) {
                    performLogin();
                } else {
                    passwordEditText.requestFocus();
                }
                return true;
            }
            return false;
        });

        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performLogin();
                return true;
            }
            return false;
        });

        loginButton.setOnClickListener(v -> performLogin());

        goToSignUpText.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        forgotPasswordText.setOnClickListener(v ->
                announceForAccessibility("Η επαναφορά κωδικού είναι προσωρινά μη διαθέσιμη.")
        );
    }

    private void announceForAccessibility(String message) {
        getWindow().getDecorView().announceForAccessibility(message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void performLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            announceForAccessibility("Παρακαλώ συμπληρώστε το email σας.");
            emailEditText.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            announceForAccessibility("Παρακαλώ συμπληρώστε τον κωδικό σας.");
            passwordEditText.requestFocus();
            return;
        }

        loginButton.setEnabled(false);

        supabaseAuth.login(email, password, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String userId) {
                runOnUiThread(() -> {
                    announceForAccessibility("Σύνδεση επιτυχής.");
                    Intent intent;
                    if (!OnboardingActivity.isOnboardingDone(LoginActivity.this)) {
                        intent = new Intent(LoginActivity.this, OnboardingActivity.class);
                    } else {
                        intent = new Intent(LoginActivity.this, MainActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String errorMsg) {
                runOnUiThread(() -> {
                    loginButton.setEnabled(true);
                    announceForAccessibility("Σφάλμα σύνδεσης: " + errorMsg);
                });
            }
        });
    }
}