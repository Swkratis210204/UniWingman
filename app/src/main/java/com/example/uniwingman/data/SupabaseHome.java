package com.example.uniwingman.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseHome {

    public interface HomeCallback {
        void onSuccess(String gpa, int streak);
        void onError(String error);
    }

    private final String url;
    private final String apiKey;
    private final OkHttpClient client;
    private final Context context;

    public SupabaseHome(Context context) {
        this.context = context;
        Dotenv dotenv = Dotenv.configure()
                .directory("./assets")
                .filename("env")
                .load();
        this.url    = dotenv.get("DB_URL");
        this.apiKey = dotenv.get("DB_PASSWORD");
        this.client = new OkHttpClient();
    }

    public void fetchGpaAndStreak(String userId, HomeCallback callback) {
        // 1. Ενημέρωσε streak (login σήμερα)
        updateStreak(userId, new Runnable() {
            @Override
            public void run() {
                // 2. Φέρε GPA και streak
                fetchStats(userId, callback);
            }
        });
    }

    private void updateStreak(String userId, Runnable onDone) {
        SharedPreferences prefs = context.getSharedPreferences("UniWingmanPrefs", Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastLogin = prefs.getString("lastLoginDate", "");

        if (today.equals(lastLogin)) {
            // Έχει ήδη συνδεθεί σήμερα, δεν αυξάνουμε
            onDone.run();
            return;
        }

        // Αποθήκευσε τη σημερινή ημερομηνία
        prefs.edit().putString("lastLoginDate", today).apply();

        // Φέρε τρέχον streak
        String streakUrl = url + "/rest/v1/user_stats?user_id=eq." + userId + "&select=streak&limit=1";
        Request req = new Request.Builder()
                .url(streakUrl)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + apiKey)
                .get().build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { onDone.run(); }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "[]";
                try {
                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                    int currentStreak = 0;
                    boolean exists = arr.size() > 0;

                    if (exists) {
                        JsonObject row = arr.get(0).getAsJsonObject();
                        currentStreak = row.has("streak") && !row.get("streak").isJsonNull()
                                ? row.get("streak").getAsInt() : 0;
                    }

                    int newStreak = currentStreak + 1;

                    // Upsert streak
                    JsonObject body2 = new JsonObject();
                    body2.addProperty("user_id", userId);
                    body2.addProperty("streak", newStreak);

                    String upsertUrl = url + "/rest/v1/user_stats";
                    Request upsertReq = new Request.Builder()
                            .url(upsertUrl)
                            .addHeader("apikey", apiKey)
                            .addHeader("Authorization", "Bearer " + apiKey)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Prefer", "resolution=merge-duplicates")
                            .post(RequestBody.create(body2.toString(),
                                    MediaType.get("application/json")))
                            .build();

                    client.newCall(upsertReq).enqueue(new Callback() {
                        @Override public void onFailure(Call c, IOException e) { onDone.run(); }
                        @Override public void onResponse(Call c, Response r) throws IOException {
                            if (r.body() != null) r.body().close();
                            onDone.run();
                        }
                    });
                } catch (Exception e) { onDone.run(); }
            }
        });
    }

    private void fetchStats(String userId, HomeCallback callback) {
        // GPA από student_courses
        String coursesUrl = url + "/rest/v1/student_courses?user_id=eq." + userId
                + "&status=eq.passed&select=grade";

        Request req = new Request.Builder()
                .url(coursesUrl)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + apiKey)
                .get().build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "[]";
                String gpa = "—";
                try {
                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                    double sum = 0; int count = 0;
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject o = arr.get(i).getAsJsonObject();
                        if (o.has("grade") && !o.get("grade").isJsonNull()) {
                            sum += o.get("grade").getAsDouble();
                            count++;
                        }
                    }
                    if (count > 0) gpa = String.format("%.1f", sum / count);
                } catch (Exception ignored) {}

                final String finalGpa = gpa;

                // Τώρα φέρε streak
                String streakUrl = url + "/rest/v1/user_stats?user_id=eq." + userId
                        + "&select=streak&limit=1";
                Request sreq = new Request.Builder()
                        .url(streakUrl)
                        .addHeader("apikey", apiKey)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .get().build();

                client.newCall(sreq).enqueue(new Callback() {
                    @Override public void onFailure(Call c, IOException e) {
                        callback.onSuccess(finalGpa, 0);
                    }
                    @Override public void onResponse(Call c, Response r) throws IOException {
                        String sb = r.body() != null ? r.body().string() : "[]";
                        int streak = 0;
                        try {
                            JsonArray a = JsonParser.parseString(sb).getAsJsonArray();
                            if (a.size() > 0 && a.get(0).getAsJsonObject().has("streak"))
                                streak = a.get(0).getAsJsonObject().get("streak").getAsInt();
                        } catch (Exception ignored) {}
                        callback.onSuccess(finalGpa, streak);
                    }
                });
            }
        });
    }
}
