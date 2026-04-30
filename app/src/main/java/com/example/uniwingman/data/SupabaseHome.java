package com.example.uniwingman.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.uniwingman.ui.home.CourseSlot;
import com.example.uniwingman.ui.home.ScheduleDay;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    public interface ScheduleCallback {
        void onSuccess(List<ScheduleDay> days);
        void onError(String error);
    }

    private final String url;
    private final String apiKey;
    private final OkHttpClient client;
    private final Context context;

    // Πλήρη ονόματα μερών όπως έρχονται από τη βάση
    private static final List<String> DAY_ORDER =
            Arrays.asList("Δευτέρα", "Τρίτη", "Τετάρτη", "Πέμπτη", "Παρασκευή");

    // Συντομογραφίες για εμφάνιση στο UI
    private static final Map<String, String> DAY_SHORT = new java.util.HashMap<String, String>() {{
        put("Δευτέρα",    "Δευ");
        put("Τρίτη",      "Τρι");
        put("Τετάρτη",    "Τετ");
        put("Πέμπτη",     "Πεμ");
        put("Παρασκευή",  "Παρ");
    }};

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

    // ─────────────────────────────────────────────
    // SCHEDULE
    // ─────────────────────────────────────────────
    public void fetchSchedule(String userId, ScheduleCallback callback) {
        String scheduleUrl = url
                + "/rest/v1/student_courses"
                + "?user_id=eq." + userId
                + "&status=eq.in_progress"
                + "&select=course_id,courses(title,schedule(day,time_start,time_end,type,room))";

        Request req = new Request.Builder()
                .url(scheduleUrl)
                .addHeader("apikey", apiKey)
                .addHeader("Authorization", "Bearer " + apiKey)
                .get().build();

        client.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "[]";
                android.util.Log.d("SCHEDULE", "Response: " + body);
                try {
                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();

                    // Ένα map ανά πλήρες όνομα μέρας
                    Map<String, List<CourseSlot>> dayMap = new LinkedHashMap<>();
                    for (String d : DAY_ORDER) dayMap.put(d, new ArrayList<>());

                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject sc = arr.get(i).getAsJsonObject();
                        if (!sc.has("courses") || sc.get("courses").isJsonNull()) continue;

                        JsonObject course = sc.getAsJsonObject("courses");
                        String title = "—";
                        if (course.has("title") && !course.get("title").isJsonNull()) {
                            title = course.get("title").getAsString().trim();
                        }

                        if (!course.has("schedule") || !course.get("schedule").isJsonArray()) continue;
                        JsonArray slots = course.getAsJsonArray("schedule");

                        for (int j = 0; j < slots.size(); j++) {
                            JsonObject slot = slots.get(j).getAsJsonObject();

                            String day    = slot.has("day") && !slot.get("day").isJsonNull()
                                    ? slot.get("day").getAsString() : "";
                            int startHour = slot.has("time_start") && !slot.get("time_start").isJsonNull()
                                    ? slot.get("time_start").getAsInt() : 0;
                            int endHour   = slot.has("time_end") && !slot.get("time_end").isJsonNull()
                                    ? slot.get("time_end").getAsInt() : 0;
                            // null type → "Διάλεξη" ως default
                            String type   = slot.has("type") && !slot.get("type").isJsonNull()
                                    ? slot.get("type").getAsString() : "Διάλεξη";

                            String timeStr = startHour + ":00-" + endHour + ":00";

                            if (dayMap.containsKey(day)) {
                                dayMap.get(day).add(new CourseSlot(title, timeStr, type));
                            }
                        }
                    }

                    // Ταξινόμηση + μετατροπή σε ScheduleDay με συντομογραφία
                    List<ScheduleDay> result = new ArrayList<>();
                    for (String day : DAY_ORDER) {
                        List<CourseSlot> slots = dayMap.get(day);
                        slots.sort((a, b) -> {
                            int aH = Integer.parseInt(a.time.split(":")[0]);
                            int bH = Integer.parseInt(b.time.split(":")[0]);
                            return Integer.compare(aH, bH);
                        });
                        String shortDay = DAY_SHORT.containsKey(day) ? DAY_SHORT.get(day) : day;
                        result.add(new ScheduleDay(shortDay, slots));
                    }

                    callback.onSuccess(result);

                } catch (Exception e) {
                    android.util.Log.e("SCHEDULE", "Error: " + e.getMessage());
                    callback.onError("Parse error: " + e.getMessage());
                }
            }
        });
    }

    // ─────────────────────────────────────────────
    // GPA + STREAK
    // ─────────────────────────────────────────────
    public void fetchGpaAndStreak(String userId, HomeCallback callback) {
        updateStreak(userId, () -> fetchStats(userId, callback));
    }

    private void updateStreak(String userId, Runnable onDone) {
        SharedPreferences prefs = context.getSharedPreferences("UniWingmanPrefs", Context.MODE_PRIVATE);
        String today     = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastLogin = prefs.getString("lastLoginDate", "");

        if (today.equals(lastLogin)) { onDone.run(); return; }
        prefs.edit().putString("lastLoginDate", today).apply();

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
                int newStreak = 1;
                try {
                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                    if (arr.size() > 0) {
                        JsonObject row = arr.get(0).getAsJsonObject();
                        int cur = row.has("streak") && !row.get("streak").isJsonNull()
                                ? row.get("streak").getAsInt() : 0;
                        newStreak = cur + 1;
                    }
                } catch (Exception ignored) {}

                JsonObject upsertBody = new JsonObject();
                upsertBody.addProperty("user_id", userId);
                upsertBody.addProperty("streak", newStreak);

                Request upsertReq = new Request.Builder()
                        .url(url + "/rest/v1/user_stats")
                        .addHeader("apikey", apiKey)
                        .addHeader("Authorization", "Bearer " + apiKey)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "resolution=merge-duplicates")
                        .post(RequestBody.create(upsertBody.toString(),
                                MediaType.get("application/json")))
                        .build();

                client.newCall(upsertReq).enqueue(new Callback() {
                    @Override public void onFailure(Call c, IOException e) { onDone.run(); }
                    @Override public void onResponse(Call c, Response r) throws IOException {
                        if (r.body() != null) r.body().close();
                        onDone.run();
                    }
                });
            }
        });
    }

    private void fetchStats(String userId, HomeCallback callback) {
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
                            sum += o.get("grade").getAsDouble(); count++;
                        }
                    }
                    if (count > 0) gpa = String.format("%.2f", sum / count);
                } catch (Exception ignored) {}

                final String finalGpa = gpa;
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