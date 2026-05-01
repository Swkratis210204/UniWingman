package com.example.uniwingman.ui.profile;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.os.LocaleListCompat;

import com.example.uniwingman.R;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS             = "UniWingmanPrefs";
    public static final String KEY_NIGHT_MODE    = "nightMode";
    public static final String KEY_TEXT_SIZE     = "textSize";
    public static final String KEY_COLORBLIND    = "colorblindMode";
    public static final String KEY_LANGUAGE      = "language";
    public static final String KEY_NOTIFICATIONS = "notifications";

    private Switch switchNightMode, switchNotifications;
    private Spinner spinnerTextSize, spinnerColorblind, spinnerLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySettings(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setupToolbar();
        loadPreferences();
    }

    private void initViews() {
        switchNightMode     = findViewById(R.id.switchNightMode);
        switchNotifications = findViewById(R.id.switchNotifications);
        spinnerTextSize     = findViewById(R.id.spinnerTextSize);
        spinnerColorblind   = findViewById(R.id.spinnerColorblind);
        spinnerLanguage     = findViewById(R.id.spinnerLanguage);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Ρυθμίσεις");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        // Night Mode
        switchNightMode.setChecked(prefs.getBoolean(KEY_NIGHT_MODE, false));
        switchNightMode.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean(KEY_NIGHT_MODE, isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ?
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // Text Size
        String[] textSizes = {"Μικρό", "Κανονικό", "Μεγάλο"};
        setupSpinner(spinnerTextSize, textSizes, prefs.getInt(KEY_TEXT_SIZE, 1), (pos) -> {
            prefs.edit().putInt(KEY_TEXT_SIZE, pos).apply();
        });

        // Colorblind (requires theme swap logic in applySettings to actually change colors)
        String[] colorModes = {"Κανένα", "Deuteranopia", "Protanopia", "Tritanopia"};
        setupSpinner(spinnerColorblind, colorModes, prefs.getInt(KEY_COLORBLIND, 0), (pos) -> {
            prefs.edit().putInt(KEY_COLORBLIND, pos).apply();
        });

        // Language (Fixed: Uses modern AndroidX LocaleListCompat)
        String[] languages = {"Ελληνικά", "English"};
        setupSpinner(spinnerLanguage, languages, prefs.getInt(KEY_LANGUAGE, 0), (pos) -> {
            prefs.edit().putInt(KEY_LANGUAGE, pos).apply();
            String tag = (pos == 1) ? "en" : "el";
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
        });

        switchNotifications.setChecked(prefs.getBoolean(KEY_NOTIFICATIONS, true));
        switchNotifications.setOnCheckedChangeListener((btn, isChecked) ->
                prefs.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply());

        findViewById(R.id.btnApply).setOnClickListener(v -> recreate());
    }

    private void setupSpinner(Spinner spinner, String[] items, int selection, OnSelectionAction action) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        spinner.setAdapter(adapter);
        spinner.setSelection(selection);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean isFirst = true;
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (isFirst) { isFirst = false; return; }
                action.onSelect(pos);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    public static void applySettings(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE);

        // Apply Night Mode
        boolean nightMode = prefs.getBoolean(KEY_NIGHT_MODE, false);
        AppCompatDelegate.setDefaultNightMode(nightMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        // Apply Font Scale
        int textSize = prefs.getInt(KEY_TEXT_SIZE, 1);
        float scale = (textSize == 0) ? 0.85f : (textSize == 2) ? 1.15f : 1.0f;

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.fontScale = scale;
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
    }

    interface OnSelectionAction {
        void onSelect(int position);
    }
}