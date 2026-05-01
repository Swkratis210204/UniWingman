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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
                passwordEditText.requestFocus(); // απλώς πήγαινε στο password
                return true;
            }
            return false;
        });

        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN)) {
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
                showMessage("Η επαναφορά κωδικού είναι προσωρινά μη διαθέσιμη.")
        );
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void announceForAccessibility(String message) {
        getWindow().getDecorView().announceForAccessibility(message);
    }

    private void performLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            showMessage("Παρακαλώ συμπληρώστε το email σας.");
            emailEditText.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            showMessage("Παρακαλώ συμπληρώστε τον κωδικό σας.");
            passwordEditText.requestFocus();
            return;
        }

        loginButton.setEnabled(false);

        supabaseAuth.login(email, password, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    try {
                        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                        String userId   = json.get("userId").getAsString();
                        String email    = json.get("email").getAsString();
                        String username = json.get("username").getAsString();

                        getSharedPreferences("UniWingmanPrefs", MODE_PRIVATE)
                                .edit()
                                .putString("userId",   userId)
                                .putString("email",    email)
                                .putString("username", username)
                                .apply();
                    } catch (Exception e) {
                        // fallback — αποθήκευσε τουλάχιστον το email
                        getSharedPreferences("UniWingmanPrefs", MODE_PRIVATE)
                                .edit()
                                .putString("email", email)
                                .putString("username", email.split("@")[0])
                                .apply();
                    }

                    showMessage("Σύνδεση επιτυχής.");
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
                    showMessage("Σφάλμα σύνδεσης: " + errorMsg);
                });
            }
        });
    }
}