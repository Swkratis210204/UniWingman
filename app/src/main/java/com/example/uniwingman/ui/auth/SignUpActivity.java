package com.example.uniwingman.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.uniwingman.R;
import com.example.uniwingman.data.SupabaseAuth;
import com.google.android.material.textfield.TextInputEditText;

public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText usernameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText confirmPasswordEditText;
    private Button registerButton;
    private Button goToLoginButton;
    private SupabaseAuth supabaseAuth;
    private TextView checkLength, checkUppercase, checkNumber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        usernameEditText        = findViewById(R.id.signUpUsernameEditText);
        emailEditText           = findViewById(R.id.signUpEmailEditText);
        passwordEditText        = findViewById(R.id.signUpPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.signUpConfirmPasswordEditText);
        registerButton          = findViewById(R.id.registerButton);
        goToLoginButton         = findViewById(R.id.goToLoginButton);
        checkLength   = findViewById(R.id.checkLength);
        checkUppercase= findViewById(R.id.checkUppercase);
        checkNumber   = findViewById(R.id.checkNumber);

        passwordEditText.setText("");
        confirmPasswordEditText.setText("");

        supabaseAuth = new SupabaseAuth();

        // Enter chain: username → email → password → confirm → sign up
        usernameEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                emailEditText.requestFocus();
                return true;
            }
            return false;
        });

        emailEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                passwordEditText.requestFocus();
                return true;
            }
            return false;
        });

        passwordEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String pwd = s.toString();

                // Length check
                if (pwd.length() >= 8) {
                    checkLength.setText("✓ Τουλάχιστον 8 χαρακτήρες");
                    checkLength.setTextColor(0xFF4CAF50); // green
                } else {
                    checkLength.setText("✗ Τουλάχιστον 8 χαρακτήρες");
                    checkLength.setTextColor(0xFFF44336); // red
                }

                // Uppercase check
                if (pwd.matches(".*[A-Z].*")) {
                    checkUppercase.setText("✓ Ένα κεφαλαίο γράμμα (A-Z)");
                    checkUppercase.setTextColor(0xFF4CAF50);
                } else {
                    checkUppercase.setText("✗ Ένα κεφαλαίο γράμμα (A-Z)");
                    checkUppercase.setTextColor(0xFFF44336);
                }

                // Number check
                if (pwd.matches(".*[0-9].*")) {
                    checkNumber.setText("✓ Έναν αριθμό (0-9)");
                    checkNumber.setTextColor(0xFF4CAF50);
                } else {
                    checkNumber.setText("✗ Έναν αριθμό (0-9)");
                    checkNumber.setTextColor(0xFFF44336);
                }
            }
        });

        confirmPasswordEditText.setOnEditorActionListener((v, actionId, event) -> {
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
        String confirm  = confirmPasswordEditText.getText().toString().trim();

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
        if (password.length() < 8) {
            Toast.makeText(this, "Ο κωδικός πρέπει να έχει τουλάχιστον 8 χαρακτήρες.", Toast.LENGTH_SHORT).show();
            passwordEditText.requestFocus();
            return;
        }
        if (!password.matches(".*[A-Z].*")) {
            Toast.makeText(this, "Ο κωδικός πρέπει να περιέχει τουλάχιστον ένα κεφαλαίο γράμμα.", Toast.LENGTH_SHORT).show();
            passwordEditText.requestFocus();
            return;
        }
        if (!password.matches(".*[0-9].*")) {
            Toast.makeText(this, "Ο κωδικός πρέπει να περιέχει τουλάχιστον έναν αριθμό.", Toast.LENGTH_SHORT).show();
            passwordEditText.requestFocus();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Οι κωδικοί δεν ταιριάζουν.", Toast.LENGTH_SHORT).show();
            confirmPasswordEditText.setText("");
            confirmPasswordEditText.requestFocus();
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