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
        // Query the 'users' table specifically for a count of records
        String fetchUrl = this.url + "/rest/v1/users?select=count";

        Request request = new Request.Builder()
                .url(fetchUrl)
                .addHeader("apikey", this.apiKey)
                .addHeader("Authorization", "Bearer " + this.apiKey)
                // 'count=exact' tells Supabase to return the total row count in the headers
                .addHeader("Prefer", "count=exact")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Network error or server unreachable
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Read the 'content-range' header which contains the total count
                    // Format example: "0-0/15" where 15 is the total count
                    String range = response.header("content-range");
                    if (range != null && range.contains("/")) {
                        try {
                            // Extract the number after the "/"
                            int count = Integer.parseInt(range.split("/")[1]);
                            callback.onSuccess(count);
                        } catch (Exception e) {
                            callback.onError("Parsing error: " + e.getMessage());
                        }
                    } else {
                        callback.onSuccess(0);
                    }
                } else {
                    // API returned an error (e.g., 401 Unauthorized or 404 Not Found)
                    callback.onError("Error code: " + response.code());
                }
            }
        });
    }
}