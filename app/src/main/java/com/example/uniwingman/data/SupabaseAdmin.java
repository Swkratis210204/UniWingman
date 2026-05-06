package com.example.uniwingman.data;

import okhttp3.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.IOException;

public class SupabaseAdmin {
    private final String url;
    private final String apiKey;
    private final OkHttpClient client;

    public SupabaseAdmin() {
        // Use the same logic as SupabaseAuth to load credentials from the .env file
        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                .directory("./assets")
                .filename("env")
                .load();
        this.url = dotenv.get("DB_URL");
        this.apiKey = dotenv.get("DB_PASSWORD");
        this.client = new OkHttpClient();
    }

    /**
     * Callback interface to return statistics to the ViewModel
     */
    public interface StatsCallback {
        void onSuccess(int count);
        void onError(String error);
    }

    /**
     * Method to count the total number of users in the system
     */
    public void getTotalUserCount(StatsCallback callback) {
        // --- ΠΡΟΣΩΡΙΝΗ ΛΥΣΗ (MOCK) ---
        // Επιστρέφουμε έναν τυχαίο αριθμό (π.χ. 142) για να δούμε αν το UI δουλεύει σωστά
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            callback.onSuccess(142);
        }, 500);

        /* --- Ο ΚΑΝΟΝΙΚΟΣ ΚΩΔΙΚΑΣ ΣΕ ΣΧΟΛΙΑ ---
        String fetchUrl = this.url + "/rest/v1/users?select=count";
        Request request = new Request.Builder()
                .url(fetchUrl)
                .addHeader("apikey", this.apiKey)
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .addHeader("Prefer", "count=exact")
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
                    String range = response.header("content-range");
                    if (range != null && range.contains("/")) {
                        try {
                            int count = Integer.parseInt(range.split("/")[1]);
                            callback.onSuccess(count);
                        } catch (Exception e) {
                            callback.onError("Parsing error: " + e.getMessage());
                        }
                    } else {
                        callback.onSuccess(0);
                    }
                } else {
                    callback.onError("Error code: " + response.code());
                }
            }
        });
        */
    }
}