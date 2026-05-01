package com.example.uniwingman.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.uniwingman.R;
import com.example.uniwingman.ui.auth.OnboardingActivity;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Βοήθεια & Υποστήριξη");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // ── FAQ toggles ──
        setupFaq(R.id.faqQuestion1, R.id.faqAnswer1);
        setupFaq(R.id.faqQuestion2, R.id.faqAnswer2);
        setupFaq(R.id.faqQuestion3, R.id.faqAnswer3);
        setupFaq(R.id.faqQuestion4, R.id.faqAnswer4);

        // ── Complaint form ──
        EditText etSubject  = findViewById(R.id.etComplaintSubject);
        EditText etMessage  = findViewById(R.id.etComplaintMessage);
        Button   btnSubmit  = findViewById(R.id.btnSubmitComplaint);

        btnSubmit.setOnClickListener(v -> {
            String subject = etSubject.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            if (subject.isEmpty()) {
                Toast.makeText(this, "Συμπλήρωσε το θέμα.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (message.isEmpty()) {
                Toast.makeText(this, "Συμπλήρωσε το μήνυμα.", Toast.LENGTH_SHORT).show();
                return;
            }

            etSubject.setText("");
            etMessage.setText("");
            Toast.makeText(this, "Το παράπονό σας καταγράφτηκε. Ευχαριστούμε!", Toast.LENGTH_LONG).show();
        });

        // ── Replay Onboarding ──
        Button btnOnboarding = findViewById(R.id.btnReplayOnboarding);
        btnOnboarding.setOnClickListener(v -> {
            // Καθάρισε το flag
            getSharedPreferences("UniWingmanPrefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("onboarding_done", false)
                    .apply();
            Intent intent = new Intent(this, OnboardingActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void setupFaq(int questionId, int answerId) {
        TextView question = findViewById(questionId);
        TextView answer   = findViewById(answerId);

        question.setOnClickListener(v -> {
            if (answer.getVisibility() == View.VISIBLE) {
                answer.setVisibility(View.GONE);
            } else {
                answer.setVisibility(View.VISIBLE);
            }
        });
    }
}