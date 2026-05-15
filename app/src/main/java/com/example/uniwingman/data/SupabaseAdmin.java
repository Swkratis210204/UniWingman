package com.example.uniwingman.data;

import okhttp3.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.example.uniwingman.ui.admin.AdminUserItem;

public class SupabaseAdmin {
    private final String url;
    private final String apiKey;
    private final OkHttpClient client;

    public SupabaseAdmin() {
        // Φόρτωση κλειδιών από το .env αρχείο
        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                .directory("./assets")
                .filename("env")
                .load();
        this.url = dotenv.get("DB_URL");
        this.apiKey = dotenv.get("DB_PASSWORD");
        this.client = new OkHttpClient();
    }

    /**
     * Callback interface για τα στατιστικά (συνολικός αριθμός)
     */
    public interface StatsCallback {
        void onSuccess(int count);
        void onError(String error);
    }

    /**
     * Callback interface για τη λίστα χρηστών
     */
    public interface UserListCallback {
        void onSuccess(List<AdminUserItem> users);
        void onError(String error);
    }

    /**
     * Μετράει τον συνολικό αριθμό των χρηστών στο σύστημα (Χωρίς να κατεβάζει τα δεδομένα τους)
     */
    public void getTotalUserCount(StatsCallback callback) {
        // Ζητάμε μόνο το id και λέμε στο Supabase να επιστρέψει 0 γραμμές (Range: 0-0)
        // αλλά να υπολογίσει το ακριβές σύνολο στα Headers (count=exact).
        String fetchUrl = this.url + "/rest/v1/users?select=id";

        Request request = new Request.Builder()
                .url(fetchUrl)
                .addHeader("apikey", this.apiKey)
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .addHeader("Prefer", "count=exact")
                .addHeader("Range-Unit", "items")
                .addHeader("Range", "0-0") // Το μυστικό για μηδενική κατανάλωση data!
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Το Supabase επιστρέφει το σύνολο στο header "Content-Range" (π.χ. "0-0/152")
                    String range = response.header("Content-Range");
                    if (range != null && range.contains("/")) {
                        try {
                            int count = Integer.parseInt(range.split("/")[1]);
                            callback.onSuccess(count);
                        } catch (Exception e) {
                            callback.onError("Σφάλμα ανάγνωσης αριθμού: " + e.getMessage());
                        }
                    } else {
                        callback.onSuccess(0);
                    }
                } else {
                    callback.onError("Σφάλμα κωδικός: " + response.code());
                }
                response.close(); // Σημαντικό να κλείνουμε το response
            }
        });
    }

    /**
     * Μετράει τον συνολικό αριθμό των μαθημάτων (Courses) στη βάση
     */
    public void getTotalCoursesCount(StatsCallback callback) {
        // Χτυπάμε τον πίνακα "courses" και ζητάμε ακριβή μέτρηση (count=exact)
        // με μηδενική επιστροφή δεδομένων (Range: 0-0)
        String fetchUrl = this.url + "/rest/v1/courses?select=id";

        Request request = new Request.Builder()
                .url(fetchUrl)
                .addHeader("apikey", this.apiKey)
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .addHeader("Prefer", "count=exact")
                .addHeader("Range-Unit", "items")
                .addHeader("Range", "0-0") // Το μυστικό για μηδενική κατανάλωση
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Το Supabase επιστρέφει το σύνολο στο header "Content-Range" (π.χ. "0-0/62")
                    String range = response.header("Content-Range");
                    if (range != null && range.contains("/")) {
                        try {
                            int count = Integer.parseInt(range.split("/")[1]);
                            callback.onSuccess(count);
                        } catch (Exception e) {
                            callback.onError("Σφάλμα ανάγνωσης αριθμού μαθημάτων: " + e.getMessage());
                        }
                    } else {
                        callback.onSuccess(0);
                    }
                } else {
                    callback.onError("Σφάλμα κωδικός: " + response.code());
                }
                response.close();
            }
        });
    }

    /**
     * Τραβάει τους πιο πρόσφατους χρήστες από τη βάση
     */
    public void getRecentUsers(UserListCallback callback) {
        // Ζητάμε τα username και email, ταξινομημένα κατά ID φθίνουσα, με όριο 3 εγγραφές
        String fetchUrl = this.url + "/rest/v1/users?select=username,email&order=id.desc&limit=3";

        Request request = new Request.Builder()
                .url(fetchUrl)
                .addHeader("apikey", this.apiKey)
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    try {
                        JsonArray jsonArray = JsonParser.parseString(responseData).getAsJsonArray();
                        List<AdminUserItem> users = new ArrayList<>();
                        for (int i = 0; i < jsonArray.size(); i++) {
                            JsonObject obj = jsonArray.get(i).getAsJsonObject();

                            String name = obj.has("username") && !obj.get("username").isJsonNull() ? obj.get("username").getAsString() : "Unknown";
                            String email = obj.has("email") && !obj.get("email").isJsonNull() ? obj.get("email").getAsString() : "No Email";

                            users.add(new AdminUserItem(name, email));
                        }
                        callback.onSuccess(users);
                    } catch (Exception e) {
                        callback.onError("Σφάλμα επεξεργασίας δεδομένων: " + e.getMessage());
                    }
                } else {
                    callback.onError("Σφάλμα: " + response.code());
                }
                if (response.body() != null) {
                    response.body().close();
                }
            }
        });
    }

    /**
     * Ψάχνει χρήστες με βάση το όνομά τους (username)
     */
    public void searchUsers(String query, UserListCallback callback) {
        // Χρησιμοποιούμε το ilike.*λέξη* για να βρει όσα usernames περιέχουν το κείμενο που γράψαμε
        String fetchUrl = this.url + "/rest/v1/users?select=username,email&username=ilike.*" + query + "*";

        Request request = new Request.Builder()
                .url(fetchUrl)
                .addHeader("apikey", this.apiKey)
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    try {
                        JsonArray jsonArray = JsonParser.parseString(responseData).getAsJsonArray();
                        List<AdminUserItem> users = new ArrayList<>();
                        for (int i = 0; i < jsonArray.size(); i++) {
                            JsonObject obj = jsonArray.get(i).getAsJsonObject();

                            String name = obj.has("username") && !obj.get("username").isJsonNull() ? obj.get("username").getAsString() : "Unknown";
                            String email = obj.has("email") && !obj.get("email").isJsonNull() ? obj.get("email").getAsString() : "No Email";

                            users.add(new AdminUserItem(name, email));
                        }
                        callback.onSuccess(users);
                    } catch (Exception e) {
                        callback.onError("Σφάλμα επεξεργασίας δεδομένων: " + e.getMessage());
                    }
                } else {
                    callback.onError("Σφάλμα: " + response.code());
                }
                if (response.body() != null) {
                    response.body().close();
                }
            }
        });
    }
}