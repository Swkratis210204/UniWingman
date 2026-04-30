package com.example.uniwingman.data;

import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Φέρνει τα στατιστικά προφίλ του χρήστη από τη Supabase.
 *
 * Προϋποθέτει τους εξής πίνακες στη Supabase:
 *   - student_courses (user_id, status: 'declared'|'passed'|'failed', grade)
 *   - user_stats      (user_id, streak)
 *
 * Αν οι πίνακες σου έχουν διαφορετικά ονόματα, άλλαξε τα παρακάτω URLs.
 */
public class SupabaseProfile {

    public static class ProfileStats {
        public String gpa          = "—";
        public int    streak       = 0;
        public int    totalCourses = 0;
        public int    declaredCount = 0;
        public int    passedCount   = 0;
        public int    failedCount   = 0;
    }

    public interface ProfileCallback {
        void onSuccess(ProfileStats stats);
        void onError(String error);
    }

    private final String        url;
    private final String        apiKey;
    private final OkHttpClient  client;

    public SupabaseProfile(Context context) {
        Dotenv dotenv = Dotenv.configure()
                .directory("./assets")
                .filename("env")
                .load();
        this.url    = dotenv.get("DB_URL");
        this.apiKey = dotenv.get("DB_PASSWORD");
        this.client = new OkHttpClient();
    }

    public void fetchStats(String userId, ProfileCallback callback) {
        ProfileStats stats = new ProfileStats();

        // --- 1. Φέρε τα μαθήματα του χρήστη ---
        String coursesUrl = url
                + "/rest/v1/student_courses"
                + "?user_id=eq." + userId
                + "&select=status,grade";

        Request coursesReq = new Request.Builder()
                .url(coursesUrl)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        client.newCall(coursesReq).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "[]";
                if (!response.isSuccessful()) {
                    callback.onError("Courses fetch failed: " + response.code());
                    return;
                }

                try {
                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();

                    int declared = 0, passed = 0, failed = 0;
                    double gradeSum = 0;
                    int    gradeCount = 0;

                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject course = arr.get(i).getAsJsonObject();
                        String status = course.has("status")
                                ? course.get("status").getAsString() : "";

                        switch (status) {
                            case "in_progress": declared++; break;
                            case "passed":
                                passed++;
                                if (course.has("grade") && !course.get("grade").isJsonNull()) {
                                    gradeSum += course.get("grade").getAsDouble();
                                    gradeCount++;
                                }
                                break;
                            case "failed": failed++; break;
                        }
                    }

                    stats.declaredCount = declared;
                    stats.passedCount   = passed;
                    stats.failedCount   = failed;
                    stats.totalCourses  = declared + passed + failed;

                    if (gradeCount > 0) {
                        double avg = gradeSum / gradeCount;
                        stats.gpa = String.format("%.1f", avg);
                    }

                } catch (Exception e) {
                    // Κράτα defaults αν αποτύχει το parsing
                }

                // --- 2. Φέρε streak ---
                fetchStreak(userId, stats, callback);
            }
        });
    }

    private void fetchStreak(String userId, ProfileStats stats, ProfileCallback callback) {
        String streakUrl = url
                + "/rest/v1/user_stats"
                + "?user_id=eq." + userId
                + "&select=streak"
                + "&limit=1";

        Request req = new Request.Builder()
                .url(streakUrl)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + apiKey)
                .get()
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Streak απέτυχε — επέστρεψε ό,τι έχουμε
                callback.onSuccess(stats);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "[]";
                try {
                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                    if (arr.size() > 0) {
                        JsonObject row = arr.get(0).getAsJsonObject();
                        if (row.has("streak") && !row.get("streak").isJsonNull()) {
                            stats.streak = row.get("streak").getAsInt();
                        }
                    }
                } catch (Exception ignored) {}

                callback.onSuccess(stats);
            }
        });
    }
}