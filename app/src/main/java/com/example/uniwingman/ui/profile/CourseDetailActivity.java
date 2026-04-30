package com.example.uniwingman.ui.profile;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.uniwingman.R;
import com.google.gson.JsonArray;
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

public class CourseDetailActivity extends AppCompatActivity {

    public static final String EXTRA_STUDENT_COURSE_ID = "studentCourseId";
    public static final String EXTRA_COURSE_ID         = "courseId";
    public static final String EXTRA_TITLE             = "title";
    public static final String EXTRA_CODE              = "code";
    public static final String EXTRA_ECTS              = "ects";
    public static final String EXTRA_SEMESTER          = "semester";
    public static final String EXTRA_DESCRIPTION       = "description";
    public static final String EXTRA_STATUS            = "status";
    public static final String EXTRA_GRADE             = "grade";
    public static final String EXTRA_ACADEMIC_YEAR     = "academicYear";
    public static final String EXTRA_TAKEN_SEMESTER    = "takenSemester";

    private String studentCourseId;
    private String courseId;
    private String supabaseUrl;
    private String supabaseKey;
    private Button btnDelete;
    private final OkHttpClient client = new OkHttpClient();

    private TextView tvTitle, tvCode, tvEcts, tvSemester, tvDescription;
    private TextView tvProfessors, tvSchedule, tvExams;
    private EditText etGrade;
    private Spinner  spinnerStatus;
    private Button   btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Λεπτομέρειες Μαθήματος");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        Dotenv dotenv = Dotenv.configure().directory("./assets").filename("env").load();
        supabaseUrl = dotenv.get("DB_URL");
        supabaseKey = dotenv.get("DB_PASSWORD");

        studentCourseId = getIntent().getStringExtra(EXTRA_STUDENT_COURSE_ID);
        courseId        = getIntent().getStringExtra(EXTRA_COURSE_ID);
        String title        = getIntent().getStringExtra(EXTRA_TITLE);
        String code         = getIntent().getStringExtra(EXTRA_CODE);
        float  ects         = getIntent().getFloatExtra(EXTRA_ECTS, 0f);
        String semester     = getIntent().getStringExtra(EXTRA_SEMESTER);
        String description  = getIntent().getStringExtra(EXTRA_DESCRIPTION);
        String status       = getIntent().getStringExtra(EXTRA_STATUS);
        float  grade        = getIntent().getFloatExtra(EXTRA_GRADE, -1f);

        tvTitle       = findViewById(R.id.tvDetailTitle);
        tvCode        = findViewById(R.id.tvDetailCode);
        tvEcts        = findViewById(R.id.tvDetailEcts);
        tvSemester    = findViewById(R.id.tvDetailSemester);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvProfessors  = findViewById(R.id.tvDetailProfessors);
        tvSchedule    = findViewById(R.id.tvDetailSchedule);
        tvExams       = findViewById(R.id.tvDetailExams);
        etGrade       = findViewById(R.id.etGrade);
        spinnerStatus       = findViewById(R.id.spinnerStatus);
        btnSave       = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(v -> confirmDelete());

        tvTitle.setText(title != null ? title.trim() : "—");
        tvCode.setText("Κωδικός: " + (code != null ? code : "—"));
        tvEcts.setText((int) ects + " ECTS");
        tvSemester.setText("Εξάμηνο: " + (semester != null ? semester : "—"));
        tvDescription.setText(description != null && !description.isEmpty()
                ? description.trim() : "Αναμένονται πληροφορίες.");

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"passed", "in_progress", "failed"});
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);
        if (status != null) {
            int pos = statusAdapter.getPosition(status);
            if (pos >= 0) spinnerStatus.setSelection(pos);
        }

        if (grade >= 0) etGrade.setText(grade == (int) grade
                ? String.valueOf((int) grade) : String.valueOf(grade));

        fetchProfessors();
        fetchSchedule();
        fetchExams();

        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void fetchProfessors() {
        tvProfessors.setText("Φόρτωση...");
        String url = supabaseUrl
                + "/rest/v1/course_professors"
                + "?course_id=eq." + courseId
                + "&select=professors(name,email,office,phone)";

        makeGet(url, body -> {
            try {
                JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                if (arr.size() == 0) {
                    runOnUiThread(() -> tvProfessors.setText("Αναμένονται πληροφορίες."));
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject cp = arr.get(i).getAsJsonObject();
                    if (!cp.has("professors") || cp.get("professors").isJsonNull()) continue;
                    JsonObject p = cp.getAsJsonObject("professors");
                    String name   = p.has("name") ? p.get("name").getAsString() : "—";
                    String email  = p.has("email") && !p.get("email").isJsonNull() ? p.get("email").getAsString() : "—";
                    String office = p.has("office") && !p.get("office").isJsonNull() ? p.get("office").getAsString() : "—";
                    String phone  = p.has("phone") && !p.get("phone").isJsonNull() ? p.get("phone").getAsString() : "—";
                    sb.append("• ").append(name).append("\n");
                    sb.append("  Email: ").append(email).append("\n");
                    sb.append("  Γραφείο: ").append(office).append("\n");
                    sb.append("  Τηλ: ").append(phone).append("\n\n");
                }
                String result = sb.toString().trim();
                runOnUiThread(() -> tvProfessors.setText(result.isEmpty() ? "Αναμένονται πληροφορίες." : result));
            } catch (Exception e) {
                runOnUiThread(() -> tvProfessors.setText("Αναμένονται πληροφορίες."));
            }
        });
    }

    private void fetchSchedule() {
        tvSchedule.setText("Φόρτωση...");
        String url = supabaseUrl
                + "/rest/v1/schedule"
                + "?course_id=eq." + courseId
                + "&select=day,time_start,time_end,room,professors(name)";

        makeGet(url, body -> {
            try {
                JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                if (arr.size() == 0) {
                    runOnUiThread(() -> tvSchedule.setText("Αναμένονται πληροφορίες."));
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject s = arr.get(i).getAsJsonObject();
                    String day   = s.has("day") && !s.get("day").isJsonNull() ? s.get("day").getAsString() : "—";
                    String start = s.has("time_start") && !s.get("time_start").isJsonNull() ? s.get("time_start").getAsString() : "—";
                    String end   = s.has("time_end") && !s.get("time_end").isJsonNull() ? s.get("time_end").getAsString() : "—";
                    String room  = s.has("room") && !s.get("room").isJsonNull() ? s.get("room").getAsString() : "—";
                    String prof  = "";
                    if (s.has("professors") && !s.get("professors").isJsonNull()) {
                        prof = " · " + s.getAsJsonObject("professors").get("name").getAsString();
                    }
                    sb.append("• ").append(day).append(": ").append(start).append("–").append(end)
                            .append(" | ").append(room).append(prof).append("\n");
                }
                String result = sb.toString().trim();
                runOnUiThread(() -> tvSchedule.setText(result.isEmpty() ? "Αναμένονται πληροφορίες." : result));
            } catch (Exception e) {
                runOnUiThread(() -> tvSchedule.setText("Αναμένονται πληροφορίες."));
            }
        });
    }

    private void fetchExams() {
        tvExams.setText("Φόρτωση...");
        String url = supabaseUrl
                + "/rest/v1/exams"
                + "?course_id=eq." + courseId
                + "&select=exam_date,time_start,room,period";

        makeGet(url, body -> {
            try {
                JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                if (arr.size() == 0) {
                    runOnUiThread(() -> tvExams.setText("Αναμένονται πληροφορίες."));
                    return;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject e = arr.get(i).getAsJsonObject();
                    String date   = e.has("exam_date") && !e.get("exam_date").isJsonNull() ? e.get("exam_date").getAsString() : "—";
                    String time   = e.has("time_start") && !e.get("time_start").isJsonNull() ? e.get("time_start").getAsString() : "—";
                    String room   = e.has("room") && !e.get("room").isJsonNull() ? e.get("room").getAsString() : "—";
                    String period = e.has("period") && !e.get("period").isJsonNull() ? e.get("period").getAsString() : "—";
                    sb.append("• ").append(date).append(" ").append(time)
                            .append(" | ").append(room).append(" | ").append(period).append("\n");
                }
                String result = sb.toString().trim();
                runOnUiThread(() -> tvExams.setText(result.isEmpty() ? "Αναμένονται πληροφορίες." : result));
            } catch (Exception e) {
                runOnUiThread(() -> tvExams.setText("Αναμένονται πληροφορίες."));
            }
        });
    }
    private void confirmDelete() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Διαγραφή Μαθήματος")
                .setMessage("Είσαι σίγουρος ότι θέλεις να διαγράψεις αυτό το μάθημα;")
                .setPositiveButton("Διαγραφή", (dialog, which) -> deleteCourse())
                .setNegativeButton("Άκυρο", null)
                .show();
    }

    private void deleteCourse() {
        String url = supabaseUrl + "/rest/v1/student_courses?id=eq." + studentCourseId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Prefer", "return=minimal")
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(CourseDetailActivity.this,
                        "Σφάλμα: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) response.body().close();
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(CourseDetailActivity.this,
                                "Το μάθημα διαγράφηκε.", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(CourseDetailActivity.this,
                                "Σφάλμα διαγραφής (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void saveChanges() {
        String gradeStr  = etGrade.getText().toString().trim();
        String newStatus = spinnerStatus.getSelectedItem().toString();

        JsonObject body = new JsonObject();
        body.addProperty("status", newStatus);

        if (!gradeStr.isEmpty()) {
            try {
                body.addProperty("grade", Float.parseFloat(gradeStr));
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Μη έγκυρος βαθμός.", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            body.add("grade", com.google.gson.JsonNull.INSTANCE);
        }

        String url = supabaseUrl + "/rest/v1/student_courses?id=eq." + studentCourseId;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        btnSave.setEnabled(false);
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(CourseDetailActivity.this,
                            "Σφάλμα δικτύου: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) response.body().close();
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(CourseDetailActivity.this,
                                "Αποθηκεύτηκε!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(CourseDetailActivity.this,
                                "Σφάλμα αποθήκευσης (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void makeGet(String url, OnResponseBody callback) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onBody("[]"); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "[]";
                callback.onBody(body);
            }
        });
    }

    interface OnResponseBody {
        void onBody(String body);
    }
}