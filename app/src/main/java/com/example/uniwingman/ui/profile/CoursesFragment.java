package com.example.uniwingman.ui.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uniwingman.R;
import com.example.uniwingman.model.CourseItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CoursesFragment extends Fragment {

    private static final String TAG = "CoursesFragment";
    public static final String ARG_STATUS = "status";

    private RecyclerView   recycler;
    private ProgressBar    progressBar;
    private TextView       tvEmpty;
    private String         statusFilter;
    private String         userId;
    private int            userCurrentSemester;
    private String         supabaseUrl;
    private String         supabaseKey;
    private final OkHttpClient client = new OkHttpClient();

    public static CoursesFragment newInstance(String status) {
        CoursesFragment f = new CoursesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATUS, status);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            statusFilter = getArguments().getString(ARG_STATUS, "passed");
        }

        Dotenv dotenv = Dotenv.configure().directory("./assets").filename("env").load();
        supabaseUrl = dotenv.get("DB_URL");
        supabaseKey = dotenv.get("DB_PASSWORD");

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("UniWingmanPrefs", android.content.Context.MODE_PRIVATE);
        userId              = prefs.getString("userId", null);
        userCurrentSemester = prefs.getInt("currentSemester", 8);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_courses, container, false);

        Toolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setTitle(getTitleForStatus(statusFilter));
        toolbar.setNavigationIcon(R.drawable.baseline_arrow_back_24);
        toolbar.setNavigationOnClickListener(v -> requireActivity().finish());

        recycler    = root.findViewById(R.id.recyclerCourses);
        progressBar = root.findViewById(R.id.progressBar);
        tvEmpty     = root.findViewById(R.id.tvEmpty);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userId != null) {
            fetchCourses();
        } else {
            tvEmpty.setVisibility(View.VISIBLE);
            tvEmpty.setText("Δεν βρέθηκε χρήστης.");
        }
    }

    private void fetchCourses() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        String url = supabaseUrl
                + "/rest/v1/student_courses"
                + "?user_id=eq." + userId
                + "&status=eq." + statusFilter
                + "&select=id,course_id,grade,status,academic_year,Semester,courses(id,code,title,ects,semester,description)";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                    tvEmpty.setText("Σφάλμα δικτύου: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "[]";

                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    try {
                        JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                        List<CourseItem> items = new ArrayList<>();

                        for (int i = 0; i < arr.size(); i++) {
                            JsonObject sc     = arr.get(i).getAsJsonObject();
                            JsonObject course = sc.has("courses") && !sc.get("courses").isJsonNull()
                                    ? sc.getAsJsonObject("courses") : null;
                            if (course == null) continue;

                            CourseItem item = new CourseItem();
                            item.studentCourseId = sc.get("id").getAsString();
                            item.courseId        = sc.get("course_id").getAsString();
                            item.status          = sc.has("status") ? sc.get("status").getAsString() : "";
                            item.grade           = sc.has("grade") && !sc.get("grade").isJsonNull()
                                    ? sc.get("grade").getAsFloat() : null;
                            item.academicYear    = sc.has("academic_year") && !sc.get("academic_year").isJsonNull()
                                    ? sc.get("academic_year").getAsInt() : 0;
                            item.takenSemester   = sc.has("Semester") && !sc.get("Semester").isJsonNull()
                                    ? sc.get("Semester").getAsString() : "";

                            item.code        = course.has("code") ? course.get("code").getAsString() : "";
                            item.title       = course.has("title") ? course.get("title").getAsString() : "";
                            item.ects        = course.has("ects") && !course.get("ects").isJsonNull()
                                    ? course.get("ects").getAsFloat() : 0f;
                            item.description = course.has("description") && !course.get("description").isJsonNull()
                                    ? course.get("description").getAsString() : "";

                            String semText   = course.has("semester") && !course.get("semester").isJsonNull()
                                    ? course.get("semester").getAsString() : "0";
                            item.rawSemester = semText;
                            if (semText.contains(",")) semText = semText.split(",")[0].trim();
                            try { item.semester = Integer.parseInt(semText); }
                            catch (NumberFormatException ignored) { item.semester = 0; }

                            items.add(item);
                        }

                        if (items.isEmpty()) {
                            tvEmpty.setVisibility(View.VISIBLE);
                        } else {
                            tvEmpty.setVisibility(View.GONE);
                            CourseCardAdapter adapter = new CourseCardAdapter(
                                    items, userCurrentSemester,
                                    courseItem -> {
                                        Intent intent = new Intent(requireContext(), CourseDetailActivity.class);
                                        intent.putExtra(CourseDetailActivity.EXTRA_STUDENT_COURSE_ID, courseItem.studentCourseId);
                                        intent.putExtra(CourseDetailActivity.EXTRA_COURSE_ID, courseItem.courseId);
                                        intent.putExtra(CourseDetailActivity.EXTRA_TITLE, courseItem.title);
                                        intent.putExtra(CourseDetailActivity.EXTRA_CODE, courseItem.code);
                                        intent.putExtra(CourseDetailActivity.EXTRA_ECTS, courseItem.ects);
                                        intent.putExtra(CourseDetailActivity.EXTRA_SEMESTER, String.valueOf(courseItem.semester));
                                        intent.putExtra(CourseDetailActivity.EXTRA_DESCRIPTION, courseItem.description);
                                        intent.putExtra(CourseDetailActivity.EXTRA_STATUS, courseItem.status);
                                        intent.putExtra(CourseDetailActivity.EXTRA_GRADE, courseItem.grade != null ? courseItem.grade : -1f);
                                        intent.putExtra(CourseDetailActivity.EXTRA_ACADEMIC_YEAR, courseItem.academicYear);
                                        intent.putExtra(CourseDetailActivity.EXTRA_TAKEN_SEMESTER, courseItem.takenSemester);
                                        startActivity(intent);
                                    }
                            );
                            recycler.setAdapter(adapter);
                        }

                    } catch (Exception e) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        tvEmpty.setText("Σφάλμα ανάλυσης δεδομένων.");
                    }
                });
            }
        });
    }

    private String getTitleForStatus(String status) {
        switch (status) {
            case "passed":      return "Περασμένα Μαθήματα";
            case "in_progress": return "Μαθήματα σε Εξέλιξη";
            case "failed":      return "Κομμένα Μαθήματα";
            default:            return "Μαθήματα";
        }
    }
}