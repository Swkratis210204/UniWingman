package com.example.uniwingman.data;

import android.content.Context;

import com.example.uniwingman.ui.home.CourseItem;
import com.example.uniwingman.ui.home.RawSlot;
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

public class SupabaseSchedule {

    public interface ScheduleCallback {
        void onSuccess(List<RawSlot> slots);
        void onError(String error);
    }

    public interface CoursesCallback {
        void onSuccess(List<CourseItem> courses);
        void onError(String error);
    }

    private final String url;
    private final String apiKey;
    private final OkHttpClient client;

    public SupabaseSchedule(Context context) {
        Dotenv dotenv = Dotenv.configure()
                .directory("./assets")
                .filename("env")
                .load();
        this.url    = dotenv.get("DB_URL");
        this.apiKey = dotenv.get("DB_PASSWORD");
        this.client = new OkHttpClient();
    }

    // Φέρε schedule του χρήστη
    public void fetchSchedule(String userId, ScheduleCallback callback) {
        String reqUrl = url
                + "/rest/v1/student_courses"
                + "?user_id=eq." + userId
                + "&status=eq.in_progress"
                + "&select=courses(title,schedule(day,time_start,time_end,type,room))";

        Request req = new Request.Builder()
                .url(reqUrl)
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
                try {
                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                    List<RawSlot> slots = new ArrayList<>();

                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject sc = arr.get(i).getAsJsonObject();
                        if (!sc.has("courses") || sc.get("courses").isJsonNull()) continue;

                        JsonObject course = sc.getAsJsonObject("courses");
                        String title = course.has("title") && !course.get("title").isJsonNull()
                                ? course.get("title").getAsString().trim() : "—";

                        if (!course.has("schedule") || !course.get("schedule").isJsonArray()) continue;
                        JsonArray schedArr = course.getAsJsonArray("schedule");

                        for (int j = 0; j < schedArr.size(); j++) {
                            JsonObject s = schedArr.get(j).getAsJsonObject();
                            String day   = s.has("day") && !s.get("day").isJsonNull()
                                    ? s.get("day").getAsString() : "";
                            int start    = s.has("time_start") && !s.get("time_start").isJsonNull()
                                    ? s.get("time_start").getAsInt() : 0;
                            int end      = s.has("time_end") && !s.get("time_end").isJsonNull()
                                    ? s.get("time_end").getAsInt() : 0;
                            String type  = s.has("type") && !s.get("type").isJsonNull()
                                    ? s.get("type").getAsString() : "Διάλεξη";
                            String room  = s.has("room") && !s.get("room").isJsonNull()
                                    ? s.get("room").getAsString() : "";

                            slots.add(new RawSlot(title, day, start, end, type, room));
                        }
                    }
                    callback.onSuccess(slots);
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Φέρε όλα τα μαθήματα για το dialog προσθήκης
    public void fetchAllCourses(CoursesCallback callback) {
        String reqUrl = url + "/rest/v1/courses?select=id,title&order=title.asc";

        Request req = new Request.Builder()
                .url(reqUrl)
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
                try {
                    JsonArray arr = JsonParser.parseString(body).getAsJsonArray();
                    List<CourseItem> courses = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject o = arr.get(i).getAsJsonObject();
                        String id    = o.has("id")    ? o.get("id").getAsString()    : "";
                        String title = o.has("title") ? o.get("title").getAsString() : "";
                        courses.add(new CourseItem(id, title));
                    }
                    callback.onSuccess(courses);
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }
}
