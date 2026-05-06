package com.example.uniwingman.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class StudentProfileRepository {

    private static final String TAG = "StudentProfileRepo";

    private final Context context;
    private final OkHttpClient client;
    private final String supabaseUrl;
    private final String supabaseKey;

    private String cachedProfileString = null;
    private boolean isLoaded = false;

    // ── Profile data fields ──
    private String username = "";
    private String department = "";
    private int currentSemester = 0;
    private double gpa = 0.0;
    private int totalEcts = 0;

    // course_id → CourseEntry
    private final Map<String, CourseEntry> allCoursesById = new HashMap<>();
    private final List<CourseEntry> enrolledCourses = new ArrayList<>();
    private final List<CourseEntry> completedCourses = new ArrayList<>();
    private final List<CourseEntry> failedCourses = new ArrayList<>();

    // major_id (1-8) → major name
    private static final Map<Integer, String> MAJOR_NAMES = new HashMap<>();
    static {
        MAJOR_NAMES.put(1, "Επιστήμη Δεδ. και ΜΜ");
        MAJOR_NAMES.put(2, "Επιχειρησιακή Έρευνα");
        MAJOR_NAMES.put(3, "Εφαρμοσμένα Μαθηματικά");
        MAJOR_NAMES.put(4, "Θεωρητική Πληροφορική");
        MAJOR_NAMES.put(5, "Συστήματα και Δίκτυα");
        MAJOR_NAMES.put(6, "Συστήματα Λογισμικού");
        MAJOR_NAMES.put(7, "Διαχείριση Δεδομένων και Γνώσεων");
        MAJOR_NAMES.put(8, "Κυβερνοασφάλεια");
    }

    // course_id → set of major_ids it belongs to
    private final Map<String, Set<Integer>> courseMajorMap = new HashMap<>();

    private static final String[] THESIS_KEYWORDS = {"πτυχιακή", "πτυχιακη", "ερευνητική", "ερευνητικη"};

    public interface ProfileCallback {
        void onReady(String profileString);
        void onError(String error);
    }

    public static class CourseEntry {
        public String id;
        public String title;
        public float ects;
        public float grade;
        public String status;
        public String semester;

        public CourseEntry(String id, String title, float ects, float grade, String status, String semester) {
            this.id = id;
            this.title = title;
            this.ects = ects;
            this.grade = grade;
            this.status = status;
            this.semester = semester;
        }

        public boolean isThesisOrResearch() {
            String lower = title.toLowerCase();
            for (String kw : THESIS_KEYWORDS) {
                if (lower.contains(kw)) return true;
            }
            return false;
        }
    }

    public StudentProfileRepository(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build();

        Dotenv dotenv = Dotenv.configure().directory("./assets").filename("env").load();
        this.supabaseUrl = dotenv.get("DB_URL");
        this.supabaseKey = dotenv.get("DB_PASSWORD");
    }

    public void loadProfile(ProfileCallback callback) {
        if (isLoaded && cachedProfileString != null) {
            callback.onReady(cachedProfileString);
            return;
        }

        new Thread(() -> {
            try {
                String userId = getUserId();
                if (userId == null || userId.isEmpty()) {
                    Log.w(TAG, "No userId found in SharedPreferences");
                    callback.onError("Δεν βρέθηκε χρήστης.");
                    return;
                }

                fetchUserInfo(userId);
                fetchStudentCourses(userId);
                fetchCourseMajors();
                computeStats();

                cachedProfileString = buildProfileString();
                isLoaded = true;

                Log.d(TAG, "Profile loaded successfully for: " + username);
                Log.d(TAG, cachedProfileString);
                callback.onReady(cachedProfileString);

            } catch (Exception e) {
                Log.e(TAG, "Profile load error: " + e.getMessage());
                callback.onError("Σφάλμα φόρτωσης προφίλ: " + e.getMessage());
            }
        }).start();
    }

    public void invalidate() {
        isLoaded = false;
        cachedProfileString = null;
        enrolledCourses.clear();
        completedCourses.clear();
        failedCourses.clear();
        allCoursesById.clear();
        courseMajorMap.clear();
    }

    public String getCachedProfile() {
        return cachedProfileString != null ? cachedProfileString : "";
    }

    // ─────────────────────────────────────────────
    //  STEP 1: Fetch user basic info
    // ─────────────────────────────────────────────
    private void fetchUserInfo(String userId) throws IOException {
        String url = supabaseUrl + "/rest/v1/users"
                + "?id=eq." + userId
                + "&select=username,department,current_semester"
                + "&limit=1";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "fetchUserInfo failed: " + response.code());
                return;
            }
            JsonArray arr = JsonParser.parseString(response.body().string()).getAsJsonArray();
            if (arr.size() > 0) {
                JsonObject row = arr.get(0).getAsJsonObject();
                username = getStringSafe(row, "username");
                department = getStringSafe(row, "department");
                currentSemester = getIntSafe(row, "current_semester");
            }
        }
    }

    // ─────────────────────────────────────────────
    //  STEP 2: Fetch student_courses JOIN courses
    // ─────────────────────────────────────────────
    private void fetchStudentCourses(String userId) throws IOException {
        String url = supabaseUrl + "/rest/v1/student_courses"
                + "?user_id=eq." + userId
                + "&select=grade,status,Semester,course_id,courses(id,title,ects,semester)";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "fetchStudentCourses failed: " + response.code());
                return;
            }

            JsonArray arr = JsonParser.parseString(response.body().string()).getAsJsonArray();

            for (JsonElement el : arr) {
                JsonObject row = el.getAsJsonObject();

                float grade = -1f;
                if (row.has("grade") && !row.get("grade").isJsonNull()) {
                    grade = row.get("grade").getAsFloat();
                }

                String status = getStringSafe(row, "status").toLowerCase();
                String semesterLabel = getStringSafe(row, "Semester");
                String courseId = getStringSafe(row, "course_id");

                String title = "";
                float ects = 0f;
                if (row.has("courses") && !row.get("courses").isJsonNull()) {
                    JsonObject course = row.getAsJsonObject("courses");
                    title = getStringSafe(course, "title");
                    if (course.has("ects") && !course.get("ects").isJsonNull()) {
                        ects = course.get("ects").getAsFloat();
                    }
                }

                if (title.isEmpty()) continue;

                CourseEntry entry = new CourseEntry(courseId, title, ects, grade, status, semesterLabel);

                if (!courseId.isEmpty()) {
                    allCoursesById.put(courseId, entry);
                }

                switch (status) {
                    case "completed":
                    case "passed":
                        completedCourses.add(entry);
                        break;
                    case "failed":
                        failedCourses.add(entry);
                        break;
                    default:
                        enrolledCourses.add(entry);
                        break;
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    //  STEP 3: Fetch course_majors for student's courses
    // ─────────────────────────────────────────────
    private void fetchCourseMajors() throws IOException {
        if (allCoursesById.isEmpty()) return;

        StringBuilder inFilter = new StringBuilder("(");
        List<String> ids = new ArrayList<>(allCoursesById.keySet());
        for (int i = 0; i < ids.size(); i++) {
            inFilter.append(ids.get(i));
            if (i < ids.size() - 1) inFilter.append(",");
        }
        inFilter.append(")");

        String url = supabaseUrl + "/rest/v1/course_majors"
                + "?course_id=in." + inFilter
                + "&select=course_id,major_id";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "fetchCourseMajors failed: " + response.code());
                return;
            }

            JsonArray arr = JsonParser.parseString(response.body().string()).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject row = el.getAsJsonObject();
                String courseId = getStringSafe(row, "course_id");
                int majorId = getIntSafe(row, "major_id");
                if (!courseId.isEmpty() && majorId >= 1 && majorId <= 8) {
                    courseMajorMap.computeIfAbsent(courseId, k -> new HashSet<>()).add(majorId);
                }
            }
            Log.d(TAG, "Loaded course_majors: " + courseMajorMap.size() + " course mappings");
        }
    }

    // ─────────────────────────────────────────────
    //  STEP 4: Compute GPA + total ECTS
    // ─────────────────────────────────────────────
    private void computeStats() {
        double sum = 0.0;
        int count = 0;
        totalEcts = 0;

        for (CourseEntry c : completedCourses) {
            totalEcts += (int) c.ects;
            if (c.grade > 0) {
                sum += c.grade;
                count++;
            }
        }

        gpa = count > 0 ? sum / count : 0.0;
    }

    // ─────────────────────────────────────────────
    //  STEP 5: Build profile string with cycle analysis
    // ─────────────────────────────────────────────
    private String buildProfileString() {
        StringBuilder sb = new StringBuilder();

        // ── Basic info ──
        sb.append("=== ΠΡΟΦΙΛ ΦΟΙΤΗΤΗ ===\n");
        sb.append("Όνομα: ").append(username.isEmpty() ? "Άγνωστο" : username).append("\n");
        sb.append("Τμήμα: ").append(department.isEmpty() ? "Πληροφορική ΟΠΑ" : department).append("\n");
        sb.append("Τρέχον Εξάμηνο: ").append(currentSemester > 0 ? currentSemester + "ο" : "Άγνωστο").append("\n");
        sb.append("Συνολικά ECTS που έχουν περαστεί: ").append(totalEcts).append(" / 240\n");
        if (gpa > 0) {
            sb.append(String.format("Μέσος Όρος Βαθμολογίας: %.2f\n", gpa));
        } else {
            sb.append("Μέσος Όρος Βαθμολογίας: Δεν υπάρχουν βαθμοί ακόμα\n");
        }

        // ── Enrolled courses ──
        if (!enrolledCourses.isEmpty()) {
            sb.append("\nΔηλωμένα Μαθήματα (τρέχον εξάμηνο):\n");
            for (CourseEntry c : enrolledCourses) {
                sb.append("  - ").append(c.title)
                        .append(" (").append((int) c.ects).append(" ECTS)");
                Set<Integer> majors = courseMajorMap.get(c.id);
                if (majors != null && !majors.isEmpty()) {
                    sb.append(" [Κύκλοι: ").append(majorsToString(majors)).append("]");
                }
                sb.append(" — Σε εξέλιξη\n");
            }
        }

        // ── Completed courses ──
        if (!completedCourses.isEmpty()) {
            sb.append("\nΟλοκληρωμένα Μαθήματα:\n");
            for (CourseEntry c : completedCourses) {
                sb.append("  - ").append(c.title)
                        .append(" (").append((int) c.ects).append(" ECTS)");
                Set<Integer> majors = courseMajorMap.get(c.id);
                if (majors != null && !majors.isEmpty()) {
                    sb.append(" [Κύκλοι: ").append(majorsToString(majors)).append("]");
                }
                if (c.grade > 0) {
                    sb.append(String.format(" — Βαθμός: %.1f", c.grade));
                }
                sb.append("\n");
            }
        }

        // ── Failed courses ──
        if (!failedCourses.isEmpty()) {
            sb.append("\nΑποτυχημένα Μαθήματα (πρέπει να επαναληφθούν):\n");
            for (CourseEntry c : failedCourses) {
                sb.append("  - ").append(c.title)
                        .append(" (").append((int) c.ects).append(" ECTS)\n");
            }
        }

        // ── Cycle analysis ──
        sb.append("\n=== ΑΝΑΛΥΣΗ ΚΥΚΛΩΝ ΣΠΟΥΔΩΝ ===\n");
        sb.append("(Κάθε κύκλος χρειάζεται 5 μαθήματα. Ένα μάθημα δηλώνεται σε ΕΝΑΝ μόνο κύκλο.)\n");
        sb.append("(Τα μαθήματα εμφανίζονται μόνο στον πρώτο κύκλο τους. Αν ανήκουν και σε άλλον, αναφέρεται.)\n\n");

        // Track which course titles have already been shown (per list: completed / enrolled)
        Set<String> shownCompleted = new HashSet<>();
        Set<String> shownEnrolled = new HashSet<>();

        for (int majorId = 1; majorId <= 8; majorId++) {
            String majorName = MAJOR_NAMES.get(majorId);

            // Build lists — only include courses not yet shown
            List<String> completedForMajor = new ArrayList<>();
            List<String> enrolledForMajor = new ArrayList<>();

            // Count includes ALL courses for this major (even already shown) for accurate status
            int completedCount = 0;
            int enrolledCount = 0;

            for (CourseEntry c : completedCourses) {
                Set<Integer> majors = courseMajorMap.get(c.id);
                boolean belongs = (majors != null && majors.contains(majorId)) || c.isThesisOrResearch();
                if (belongs) {
                    completedCount++;
                    if (!shownCompleted.contains(c.title)) {
                        // Build suffix noting other cycles
                        String suffix = buildOtherCyclesSuffix(c, majorId);
                        completedForMajor.add(c.title + suffix);
                        shownCompleted.add(c.title);
                    }
                }
            }

            for (CourseEntry c : enrolledCourses) {
                Set<Integer> majors = courseMajorMap.get(c.id);
                boolean belongs = (majors != null && majors.contains(majorId)) || c.isThesisOrResearch();
                if (belongs) {
                    enrolledCount++;
                    if (!shownEnrolled.contains(c.title)) {
                        String suffix = buildOtherCyclesSuffix(c, majorId);
                        enrolledForMajor.add(c.title + suffix);
                        shownEnrolled.add(c.title);
                    }
                }
            }

            int remaining = Math.max(0, 5 - completedCount - enrolledCount);
            boolean closed = completedCount >= 5;
            boolean closedWithEnrolled = !closed && (completedCount + enrolledCount) >= 5;

            sb.append("Κύκλος ").append(majorId).append(" - ").append(majorName).append(":\n");

            if (closed) {
                sb.append("  Κατάσταση: ΚΛΕΙΣΤΟΣ (").append(completedCount).append(" περασμένα)\n");
            } else if (closedWithEnrolled) {
                sb.append("  Κατάσταση: Κλείνει αν περαστούν τα δηλωμένα (")
                        .append(completedCount).append(" περασμένα + ").append(enrolledCount).append(" δηλωμένα)\n");
            } else {
                sb.append("  Κατάσταση: Ανοιχτός — ").append(completedCount)
                        .append(" περασμένα, χρειάζονται ").append(remaining).append(" ακόμα\n");
            }

            if (!completedForMajor.isEmpty()) {
                sb.append("  Περασμένα (νέα σε αυτόν τον κύκλο):\n");
                for (String t : completedForMajor) sb.append("    - ").append(t).append("\n");
            }

            if (!enrolledForMajor.isEmpty()) {
                sb.append("  Δηλωμένα φέτος (νέα σε αυτόν τον κύκλο):\n");
                for (String t : enrolledForMajor) sb.append("    - ").append(t).append("\n");
            }

            sb.append("\n");
        }

        sb.append("=== ΤΕΛΟΣ ΑΝΑΛΥΣΗΣ ===\n");
        return sb.toString();
    }

    // Build suffix like " (ανήκει και στον Κύκλο 7, 8)" for courses in multiple majors
    private String buildOtherCyclesSuffix(CourseEntry c, int currentMajorId) {
        if (c.isThesisOrResearch()) {
            return " (μετράει σε όλους τους κύκλους)";
        }
        Set<Integer> majors = courseMajorMap.get(c.id);
        if (majors == null || majors.size() <= 1) return "";

        List<Integer> others = new ArrayList<>();
        for (int id : majors) {
            if (id != currentMajorId) others.add(id);
        }
        if (others.isEmpty()) return "";

        StringBuilder sb = new StringBuilder(" (ανήκει και στον Κύκλο ");
        for (int i = 0; i < others.size(); i++) {
            sb.append(others.get(i));
            if (i < others.size() - 1) sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }

    private String majorsToString(Set<Integer> majors) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int id : majors) {
            if (!first) sb.append(", ");
            sb.append(id);
            first = false;
        }
        return sb.toString();
    }

    private String getUserId() {
        SharedPreferences prefs = context.getSharedPreferences("UniWingmanPrefs", Context.MODE_PRIVATE);
        return prefs.getString("userId", "");
    }

    private String getStringSafe(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return "";
    }

    private int getIntSafe(JsonObject obj, String key) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsInt();
        }
        return 0;
    }
}