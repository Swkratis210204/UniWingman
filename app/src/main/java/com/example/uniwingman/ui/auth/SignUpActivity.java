package com.example.uniwingman.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uniwingman.MainActivity;
import com.example.uniwingman.R;
import com.example.uniwingman.data.SupabaseAuth;
import com.google.android.material.textfield.TextInputEditText;

public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText usernameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private Button registerButton;
    private Button goToLoginButton;
    private SupabaseAuth supabaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        usernameEditText = findViewById(R.id.signUpUsernameEditText);
        emailEditText    = findViewById(R.id.signUpEmailEditText);
        passwordEditText = findViewById(R.id.signUpPasswordEditText);
        registerButton   = findViewById(R.id.registerButton);
        goToLoginButton  = findViewById(R.id.goToLoginButton);

        supabaseAuth = new SupabaseAuth();

        // Enter στο username → email
        usernameEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                emailEditText.requestFocus();
                return true;
            }
            return false;
        });

        // Enter στο email → password
        emailEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                passwordEditText.requestFocus();
                return true;
            }
            return false;
        });

        // Enter στο password → sign up
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                    actionId == EditorInfo.IME_ACTION_GO ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                performSignUp();
                return true;
            }
            return false;
        });

        registerButton.setOnClickListener(v -> performSignUp());

        goToLoginButton.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void performSignUp() {
        String username = usernameEditText.getText().toString().trim();
        String email    = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty()) {
            Toast.makeText(this, "Συμπλήρωσε το username.", Toast.LENGTH_SHORT).show();
            usernameEditText.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            Toast.makeText(this, "Συμπλήρωσε το email.", Toast.LENGTH_SHORT).show();
            emailEditText.requestFocus();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Ο κωδικός πρέπει να έχει τουλάχιστον 6 χαρακτήρες.", Toast.LENGTH_SHORT).show();
            passwordEditText.requestFocus();
            return;
        }

        registerButton.setEnabled(false);

        supabaseAuth.signUp(username, email, password, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String resultMsg) {
                runOnUiThread(() -> {
                    Toast.makeText(SignUpActivity.this,
                            "Έχουμε στείλει email επιβεβαίωσης! Επιβεβαίωσε το email σου και μετά συνδέσου.",
                            Toast.LENGTH_LONG).show();
                    // Πήγαινε στο Login, ΟΧΙ στο Onboarding
                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
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