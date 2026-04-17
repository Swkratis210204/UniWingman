package com.example.uniwingman.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.uniwingman.MainActivity;
import com.example.uniwingman.R;

public class OnboardingActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "UniWingmanPrefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private int currentPage = 0;

    private final int[] icons = {
            R.drawable.baseline_home_24,
            R.drawable.baseline_psychology_24,
            R.drawable.baseline_map_24,
            R.drawable.baseline_accessibility_new_24
    };

    private final String[] titles = {
            "Αρχική", "AI Βοηθός", "Χάρτης", "Προφίλ"
    };

    private final String[] descriptions = {
            "Ειδοποιήσεις, ανακοινώσεις και γρήγορες απαντήσεις για γραμματεία, ωράρια και βιβλία — χωρίς σύνδεση!",
            "Δύο chatbots:\n• Offline: Γρήγορες απαντήσεις χωρίς internet\n• Online AI: Ρώτα για οδηγό σπουδών, κύκλους, Erasmus και πρακτική.",
            "Βρες κτήρια ΟΠΑ, βιβλιοπωλεία και στάσεις συγκοινωνιών — με οδηγίες απευθείας στο Google Maps.",
            "Το προφίλ σου με ημερολόιο, βαθμούς και ακαδημαϊκή πρόοδο όλα σε ένα μέρος."
    };

    private ImageView ivSlideIcon;
    private TextView tvSlideTitle, tvSlideDescription, tvSkip, tvBack;
    private Button btnNext;
    private LinearLayout dotsContainer;
    private View[] dots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        ivSlideIcon = findViewById(R.id.ivSlideIcon);
        tvSlideTitle = findViewById(R.id.tvSlideTitle);
        tvSlideDescription = findViewById(R.id.tvSlideDescription);
        btnNext = findViewById(R.id.btnNext);
        tvSkip = findViewById(R.id.tvSkip);
        tvBack = findViewById(R.id.tvBack);
        dotsContainer = findViewById(R.id.dotsContainer);

        setupDots();
        updateSlideContent(0);

        btnNext.setOnClickListener(v -> {
            if (currentPage < titles.length - 1) {
                currentPage++;
                animateSlideChange(currentPage);
            } else {
                finishOnboarding();
            }
        });

        tvBack.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                animateSlideChange(currentPage);
            }
        });

        tvSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void setupDots() {
        dots = new View[titles.length];
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (8 * density);
        int margin = (int) (4 * density);

        for (int i = 0; i < titles.length; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(R.drawable.bg_drag_handle);
            dot.setAlpha(0.3f);
            // Hide individual dots from accessibility to avoid clutter
            dot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            dotsContainer.addView(dot);
            dots[i] = dot;
        }
    }

    private void animateSlideChange(int index) {
        AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setDuration(150);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation a) {}
            @Override public void onAnimationRepeat(Animation a) {}
            @Override
            public void onAnimationEnd(Animation a) {
                updateSlideContent(index);
                AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
                fadeIn.setDuration(200);
                startFadeIn(fadeIn);
            }
        });

        ivSlideIcon.startAnimation(fadeOut);
        tvSlideTitle.startAnimation(fadeOut);
        tvSlideDescription.startAnimation(fadeOut);
    }

    private void updateSlideContent(int index) {
        ivSlideIcon.setImageResource(icons[index]);
        tvSlideTitle.setText(titles[index]);
        tvSlideDescription.setText(descriptions[index]);

        tvBack.setVisibility(index == 0 ? View.GONE : View.VISIBLE);
        tvSkip.setVisibility(index == titles.length - 1 ? View.GONE : View.VISIBLE);
        btnNext.setText(index == titles.length - 1 ? "Ξεκινάμε! 🚀" : "Επόμενο →");
        btnNext.setContentDescription(index == titles.length - 1 ? "Ολοκλήρωση και είσοδος" : "Μετάβαση στην επόμενη σελίδα");

        for (int i = 0; i < dots.length; i++) {
            dots[i].setAlpha(i == index ? 1f : 0.3f);
        }

        updateAccessibilityData(index);
    }

    private void updateAccessibilityData(int index) {
        String announcement = "Σελίδα " + (index + 1) + " από " + titles.length + ": " + titles[index];
        dotsContainer.setContentDescription(announcement);

        // Force TalkBack to announce the new slide content
        getWindow().getDecorView().announceForAccessibility(announcement + ". " + descriptions[index]);
    }

    private void startFadeIn(AlphaAnimation fadeIn) {
        ivSlideIcon.startAnimation(fadeIn);
        tvSlideTitle.startAnimation(fadeIn);
        tvSlideDescription.startAnimation(fadeIn);
    }

    private void finishOnboarding() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ONBOARDING_DONE, true)
                .commit();

        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public static boolean isOnboardingDone(android.content.Context context) {
        return context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_ONBOARDING_DONE, false);
    }
}