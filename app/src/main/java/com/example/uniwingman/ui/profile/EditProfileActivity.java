package com.example.uniwingman.ui.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.uniwingman.R;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etUsername;
    private Spinner  spinnerSemester;
    private EditText etOldPassword, etNewPassword, etConfirmPassword;
    private Button   btnSaveProfile, btnChangePassword;

    private String supabaseUrl;
    private String supabaseKey;
    private String userId;
    private String accessToken;
    private String currentUsername;
    private int    currentSemester;

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Επεξεργασία Προφίλ");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        Dotenv dotenv = Dotenv.configure().directory("./assets").filename("env").load();
        supabaseUrl = dotenv.get("DB_URL");
        supabaseKey = dotenv.get("DB_PASSWORD");

        SharedPreferences prefs = getSharedPreferences("UniWingmanPrefs", MODE_PRIVATE);
        userId          = prefs.getString("userId", null);
        accessToken     = prefs.getString("accessToken", null);
        currentUsername = prefs.getString("username", "");
        currentSemester = prefs.getInt("currentSemester", 1);

        etUsername        = findViewById(R.id.etUsername);
        spinnerSemester   = findViewById(R.id.spinnerSemester);
        etOldPassword     = findViewById(R.id.etOldPassword);
        etNewPassword     = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSaveProfile    = findViewById(R.id.btnSaveProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // Προ-συμπλήρωσε
        etUsername.setText(currentUsername);

        String[] semesters = {"1ο", "2ο", "3ο", "4ο", "5ο", "6ο", "7ο", "8ο"};
        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, semesters);
        semAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semAdapter);
        if (currentSemester >= 1 && currentSemester <= 8) {
            spinnerSemester.setSelection(currentSemester - 1);
        }

        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void saveProfile() {
        String newUsername = etUsername.getText().toString().trim();
        int newSemester    = spinnerSemester.getSelectedItemPosition() + 1;

        if (newUsername.isEmpty()) {
            Toast.makeText(this, "Το username δεν μπορεί να είναι κενό.", Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("username", newUsername);
        body.addProperty("current_semester", newSemester);

        String url = supabaseUrl + "/rest/v1/users?id=eq." + userId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + (accessToken != null ? accessToken : supabaseKey))
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        btnSaveProfile.setEnabled(false);
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(EditProfileActivity.this,
                            "Σφάλμα δικτύου: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) response.body().close();
                runOnUiThread(() -> {
                    btnSaveProfile.setEnabled(true);
                    if (response.isSuccessful()) {
                        // Ενημέρωσε SharedPreferences
                        getSharedPreferences("UniWingmanPrefs", MODE_PRIVATE)
                                .edit()
                                .putString("username", newUsername)
                                .putInt("currentSemester", newSemester)
                                .apply();
                        Toast.makeText(EditProfileActivity.this,
                                "Το προφίλ αποθηκεύτηκε!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(EditProfileActivity.this,
                                "Σφάλμα αποθήκευσης (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void changePassword() {
        String oldPass     = etOldPassword.getText().toString().trim();
        String newPass     = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (oldPass.isEmpty()) {
            Toast.makeText(this, "Συμπλήρωσε τον παλιό κωδικό.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPass.length() < 8) {
            Toast.makeText(this, "Ο νέος κωδικός πρέπει να έχει τουλάχιστον 8 χαρακτήρες.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPass.matches(".*[A-Z].*")) {
            Toast.makeText(this, "Ο νέος κωδικός πρέπει να έχει τουλάχιστον ένα κεφαλαίο.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPass.matches(".*[0-9].*")) {
            Toast.makeText(this, "Ο νέος κωδικός πρέπει να έχει τουλάχιστον έναν αριθμό.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPass.equals(confirmPass)) {
            Toast.makeText(this, "Οι κωδικοί δεν ταιριάζουν.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Πρώτα verify παλιό password μέσω login
        verifyOldPassword(oldPass, newPass);
    }

    private void verifyOldPassword(String oldPass, String newPass) {
        btnChangePassword.setEnabled(false);

        SharedPreferences prefs = getSharedPreferences("UniWingmanPrefs", MODE_PRIVATE);
        String email = prefs.getString("email", "");

        JsonObject loginBody = new JsonObject();
        loginBody.addProperty("email", email);
        loginBody.addProperty("password", oldPass);

        Request loginRequest = new Request.Builder()
                .url(supabaseUrl + "/auth/v1/token?grant_type=password")
                .addHeader("apikey", supabaseKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(loginBody.toString(), MediaType.get("application/json")))
                .build();

        client.newCall(loginRequest).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnChangePassword.setEnabled(true);
                    Toast.makeText(EditProfileActivity.this,
                            "Σφάλμα δικτύου.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        btnChangePassword.setEnabled(true);
                        Toast.makeText(EditProfileActivity.this,
                                "Λάθος παλιός κωδικός.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Παλιός κωδικός σωστός — άλλαξε τον
                    try {
                        String token = JsonParser.parseString(body)
                                .getAsJsonObject().get("access_token").getAsString();
                        doChangePassword(token, newPass);
                    } catch (Exception e) {
                        btnChangePassword.setEnabled(true);
                        Toast.makeText(EditProfileActivity.this,
                                "Σφάλμα ανάλυσης.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void doChangePassword(String token, String newPass) {
        JsonObject body = new JsonObject();
        body.addProperty("password", newPass);

        Request request = new Request.Builder()
                .url(supabaseUrl + "/auth/v1/user")
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .put(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnChangePassword.setEnabled(true);
                    Toast.makeText(EditProfileActivity.this,
                            "Σφάλμα δικτύου.", Toast.LENGTH_SHORT).show();
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) response.body().close();
                runOnUiThread(() -> {
                    btnChangePassword.setEnabled(true);
                    if (response.isSuccessful()) {
                        etOldPassword.setText("");
                        etNewPassword.setText("");
                        etConfirmPassword.setText("");
                        Toast.makeText(EditProfileActivity.this,
                                "Ο κωδικός άλλαξε επιτυχώς!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(EditProfileActivity.this,
                                "Σφάλμα αλλαγής κωδικού (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}