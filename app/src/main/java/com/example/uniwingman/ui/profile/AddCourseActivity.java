package com.example.uniwingman.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uniwingman.R;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddCourseActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "userId";

    private Spinner      spinnerYear, spinnerSemesterType, spinnerCourseType;
    private Button       btnSearch;
    private RecyclerView recycler;
    private ProgressBar  progressBar;
    private TextView     tvEmpty;

    private String supabaseUrl;
    private String supabaseKey;
    private String userId;
    private final OkHttpClient client = new OkHttpClient();

    private static final String[] YEARS     = {"Όλα", "1ο Έτος", "2ο Έτος", "3ο Έτος", "4ο Έτος"};
    private static final String[] SEM_TYPES = {"Όλα", "Χειμερινό", "Εαρινό"};
    private static final String[] TYPES     = {"Όλα", "Υποχρεωτικό", "Επιλογής", "Επιλογής Κύκλου", "Ελεύθερης"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_course);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Προσθήκη Μαθήματος");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        Dotenv dotenv = Dotenv.configure().directory("./assets").filename("env").load();
        supabaseUrl = dotenv.get("DB_URL");
        supabaseKey = dotenv.get("DB_PASSWORD");
        userId = getIntent().getStringExtra(EXTRA_USER_ID);

        spinnerYear         = findViewById(R.id.spinnerYear);
        spinnerSemesterType = findViewById(R.id.spinnerSemesterType);
        spinnerCourseType   = findViewById(R.id.spinnerCourseType);
        btnSearch           = findViewById(R.id.btnSearch);
        recycler            = findViewById(R.id.recyclerAddCourse);
        progressBar         = findViewById(R.id.progressBar);
        tvEmpty             = findViewById(R.id.tvEmpty);

        spinnerYear.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, YEARS));
        spinnerSemesterType.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, SEM_TYPES));
        spinnerCourseType.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, TYPES));

        recycler.setLayoutManager(new LinearLayoutManager(this));
        btnSearch.setOnClickListener(v -> searchCourses());
    }

    private void searchCourses() {
        tvEmpty.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        recycler.setAdapter(null);

        String yearSel = spinnerYear.getSelectedItem().toString();
        String semSel  = spinnerSemesterType.getSelectedItem().toString();
        String typeSel = spinnerCourseType.getSelectedItem().toString();

        List<String> semNumbers = getSemesterNumbers(yearSel, semSel);

        StringBuilder url = new StringBuilder(supabaseUrl
                + "/rest/v1/courses?select=id,code,title,ects,semester,Type");

        if (!semNumbers.isEmpty()) {
            if (semNumbers.size() == 1) {
                url.append("&semester=eq.").append(semNumbers.get(0));
            } else {
                url.append("&semester=in.(");
                for (int i = 0; i < semNumbers.size(); i++) {
                    url.append(semNumbers.get(i));
                    if (i < semNumbers.size() - 1) url.append(",");
                }
                url.append(")");
            }
        }

        if (!typeSel.equals("Όλα")) {
            url.append("&Type=eq.").append(typeSel);
        }

        url.append("&order=semester.asc,title.asc");

        Request request = new Request.Builder()
                .url(url.toString())
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Σφάλμα δικτύου: " + e.getMessage());
                });
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "[]";
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                        if (arr.size() == 0) {
                            tvEmpty.setVisibility(View.VISIBLE);
                            tvEmpty.setText("Δεν βρέθηκαν μαθήματα.");
                            return;
                        }

                        List<AddCourseItem> items = new ArrayList<>();
                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject c = arr.get(i).getAsJsonObject();
                            AddCourseItem item = new AddCourseItem();
                            item.id       = c.has("id") ? c.get("id").getAsString() : "";
                            item.code     = c.has("code") ? c.get("code").getAsString() : "";
                            item.title    = c.has("title") ? c.get("title").getAsString() : "";
                            item.ects     = c.has("ects") && !c.get("ects").isJsonNull() ? c.get("ects").getAsFloat() : 0f;
                            item.semester = c.has("semester") && !c.get("semester").isJsonNull() ? c.get("semester").getAsString() : "0";
                            item.type     = c.has("Type") && !c.get("Type").isJsonNull() ? c.get("Type").getAsString() : "—";
                            // Parse semester number
                            String semText = item.semester;
                            if (semText.contains(",")) semText = semText.split(",")[0].trim();
                            try { item.semesterNum = Integer.parseInt(semText); }
                            catch (NumberFormatException ignored) { item.semesterNum = 1; }
                            items.add(item);
                        }

                        AddCourseAdapter adapter = new AddCourseAdapter(items,
                                courseItem -> showAddDialog(courseItem));
                        recycler.setAdapter(adapter);

                    } catch (Exception e) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("Σφάλμα ανάλυσης δεδομένων.");
                    }
                });
            }
        });
    }

    private void showAddDialog(AddCourseItem item) {
        String[] statuses     = {"Σε Εξέλιξη", "Περασμένο", "Κομμένο"};
        String[] statusValues = {"in_progress", "passed", "failed"};

        View dialogView       = getLayoutInflater().inflate(R.layout.dialog_add_course, null);
        Spinner spinnerStatus = dialogView.findViewById(R.id.dialogSpinnerStatus);
        EditText etGrade      = dialogView.findViewById(R.id.dialogEtGrade);

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        // Αρχικά κρύψε το grade (default = in_progress)
        etGrade.setVisibility(View.GONE);

        spinnerStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                etGrade.setVisibility(position == 0 ? View.GONE : View.VISIBLE);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(item.title.trim())
                .setView(dialogView)
                .setPositiveButton("Προσθήκη", (dialog, which) -> {
                    String status   = statusValues[spinnerStatus.getSelectedItemPosition()];
                    String gradeStr = etGrade.getText().toString().trim();
                    Float grade = null;

                    if (!status.equals("in_progress") && !gradeStr.isEmpty()) {
                        try {
                            float g = Float.parseFloat(gradeStr);
                            if (status.equals("passed") && g < 5) {
                                Toast.makeText(this, "Περασμένο μάθημα πρέπει να έχει βαθμό ≥ 5", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (status.equals("failed") && g >= 5) {
                                Toast.makeText(this, "Κομμένο μάθημα πρέπει να έχει βαθμό < 5", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            grade = g;
                        } catch (NumberFormatException ignored) {}
                    }

                    // Υπολόγισε αυτόματα academicYear και Semester από courses.semester
                    int semNum       = item.semesterNum > 0 ? item.semesterNum : 1;
                    int academicYear = (semNum + 1) / 2;
                    String semester  = (semNum % 2 != 0) ? "Χειμερινό" : "Εαρινό";

                    // Πρώτα check αν υπάρχει ήδη
                    checkAndInsert(item, status, grade, academicYear, semester);
                })
                .setNegativeButton("Άκυρο", null)
                .show();
    }

    // Έλεγξε αν υπάρχει ήδη, μετά κάνε insert
    private void checkAndInsert(AddCourseItem item, String status, Float grade,
                                int academicYear, String semester) {
        String checkUrl = supabaseUrl + "/rest/v1/student_courses"
                + "?user_id=eq." + userId
                + "&course_id=eq." + item.id
                + "&select=id,status&limit=1";

        android.util.Log.d("AddCourse", "Checking: user_id=" + userId + " course_id=" + item.id);

        Request checkRequest = new Request.Builder()
                .url(checkUrl)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .get().build();

        client.newCall(checkRequest).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(AddCourseActivity.this,
                        "Σφάλμα δικτύου: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }

            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "[]";
                android.util.Log.d("AddCourse", "Check body: " + body);

                runOnUiThread(() -> {
                    try {
                        JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                        if (arr.size() > 0) {
                            String existingStatus = arr.get(0).getAsJsonObject()
                                    .get("status").getAsString();
                            if (existingStatus.equals("passed")) {
                                Toast.makeText(AddCourseActivity.this,
                                        "Το μάθημα έχει ήδη περαστεί!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(AddCourseActivity.this,
                                        "Το μάθημα υπάρχει ήδη ως: " + existingStatus, Toast.LENGTH_SHORT).show();
                            }
                            return;
                        }
                        doInsertCourse(item, status, grade, academicYear, semester);
                    } catch (Exception e) {
                        doInsertCourse(item, status, grade, academicYear, semester);
                    }
                });
            }
        });
    }

    private void doInsertCourse(AddCourseItem item, String status, Float grade,
                                int academicYear, String semester) {
        JsonObject body = new JsonObject();
        body.addProperty("user_id", userId);
        body.addProperty("course_id", item.id);
        body.addProperty("status", status);
        body.addProperty("academic_year", academicYear);
        body.addProperty("Semester", semester);
        if (grade != null) body.addProperty("grade", grade);

        android.util.Log.d("AddCourse", "Inserting: " + body.toString());

        String url = supabaseUrl + "/rest/v1/student_courses";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(AddCourseActivity.this,
                        "Σφάλμα: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String respBody = response.body() != null ? response.body().string() : "";
                android.util.Log.d("AddCourse", "Insert response: " + response.code() + " " + respBody);
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(AddCourseActivity.this,
                                "Προστέθηκε: " + item.title.trim(), Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                    } else {
                        Toast.makeText(AddCourseActivity.this,
                                "Σφάλμα (" + response.code() + "): " + respBody, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private List<String> getSemesterNumbers(String year, String semType) {
        List<String> result = new ArrayList<>();
        if (year.equals("Όλα") && semType.equals("Όλα")) return result;

        int baseYear = 0;
        if (year.equals("1ο Έτος"))      baseYear = 1;
        else if (year.equals("2ο Έτος")) baseYear = 2;
        else if (year.equals("3ο Έτος")) baseYear = 3;
        else if (year.equals("4ο Έτος")) baseYear = 4;

        if (baseYear == 0) {
            for (int y = 1; y <= 4; y++) {
                if (semType.equals("Χειμερινό"))   result.add(String.valueOf((y - 1) * 2 + 1));
                else if (semType.equals("Εαρινό")) result.add(String.valueOf(y * 2));
            }
        } else {
            int winterSem = (baseYear - 1) * 2 + 1;
            int summerSem = baseYear * 2;
            if (semType.equals("Όλα")) {
                result.add(String.valueOf(winterSem));
                result.add(String.valueOf(summerSem));
            } else if (semType.equals("Χειμερινό")) {
                result.add(String.valueOf(winterSem));
            } else {
                result.add(String.valueOf(summerSem));
            }
        }
        return result;
    }

    public static class AddCourseItem {
        public String id, code, title, semester, type;
        public float  ects;
        public int    semesterNum; // parsed int version of semester
    }
}