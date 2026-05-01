package com.example.uniwingman.ui.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.example.uniwingman.R;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS             = "UniWingmanPrefs";
    public static final String KEY_NIGHT_MODE    = "nightMode";
    public static final String KEY_TEXT_SIZE     = "textSize";
    public static final String KEY_COLORBLIND    = "colorblindMode";
    public static final String KEY_LANGUAGE      = "language";
    public static final String KEY_NOTIFICATIONS = "notifications";

    private androidx.appcompat.widget.SwitchCompat switchNightMode, switchNotifications;
    private android.widget.Spinner spinnerTextSize, spinnerColorblind, spinnerLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Ρυθμίσεις");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        switchNightMode     = findViewById(R.id.switchNightMode);
        switchNotifications = findViewById(R.id.switchNotifications);
        spinnerTextSize     = findViewById(R.id.spinnerTextSize);
        spinnerColorblind   = findViewById(R.id.spinnerColorblind);
        spinnerLanguage     = findViewById(R.id.spinnerLanguage);

        // ── Night Mode ──
        switchNightMode.setChecked(prefs.getBoolean(KEY_NIGHT_MODE, false));
        switchNightMode.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(KEY_NIGHT_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // ── Text Size ──
        String[] textSizes = {"Μικρό", "Κανονικό", "Μεγάλο"};
        spinnerTextSize.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, textSizes));
        spinnerTextSize.setSelection(prefs.getInt(KEY_TEXT_SIZE, 1));
        spinnerTextSize.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            boolean first = true;
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                if (first) { first = false; return; }
                prefs.edit().putInt(KEY_TEXT_SIZE, pos).apply();
                Toast.makeText(SettingsActivity.this, "Αποθηκεύτηκε!", Toast.LENGTH_SHORT).show();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        // ── Colorblind Mode ──
        String[] colorModes = {"Κανένα", "Deuteranopia", "Protanopia", "Tritanopia"};
        spinnerColorblind.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, colorModes));
        spinnerColorblind.setSelection(prefs.getInt(KEY_COLORBLIND, 0));
        spinnerColorblind.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            boolean first = true;
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                if (first) { first = false; return; }
                prefs.edit().putInt(KEY_COLORBLIND, pos).apply();
                Toast.makeText(SettingsActivity.this, "Αποθηκεύτηκε!", Toast.LENGTH_SHORT).show();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        // ── Language ──
        String[] languages = {"Ελληνικά", "English"};
        spinnerLanguage.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, languages));
        spinnerLanguage.setSelection(prefs.getInt(KEY_LANGUAGE, 0));
        spinnerLanguage.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            boolean first = true;
            @Override public void onItemSelected(android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
                if (first) { first = false; return; }
                prefs.edit().putInt(KEY_LANGUAGE, pos).apply();
                Toast.makeText(SettingsActivity.this, "Αποθηκεύτηκε!", Toast.LENGTH_SHORT).show();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        // ── Notifications ──
        switchNotifications.setChecked(prefs.getBoolean(KEY_NOTIFICATIONS, true));
        switchNotifications.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply();
            Toast.makeText(this, isChecked ? "Ειδοποιήσεις ενεργές" : "Ειδοποιήσεις ανενεργές", Toast.LENGTH_SHORT).show();
        });
    }

    public static void applySettings(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE);
        boolean nightMode = prefs.getBoolean(KEY_NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                nightMode ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO);
    }
}