package com.example.uniwingman.data;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import io.github.cdimascio.dotenv.Dotenv;

public class SupabaseAuth {

    private String url;
    private String apiKey;
    private final OkHttpClient client;

    public SupabaseAuth() {
        Dotenv dotenv = Dotenv.configure()
                .directory("./assets")
                .filename("env")
                .load();

        this.url = dotenv.get("DB_URL");
        this.apiKey = dotenv.get("DB_PASSWORD");
        this.client = new OkHttpClient();
    }

    public interface AuthCallback {
        void onSuccess(String result);
        void onError(String errorMsg);
    }

    // --- SIGN UP ---
    public void signUp(String username, String email, String password, AuthCallback callback) {
        String endpointUrl = this.url + "/auth/v1/signup";

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("email", email);
        jsonBody.addProperty("password", password);

        // Περνάμε το username ως metadata
        JsonObject metadata = new JsonObject();
        metadata.addProperty("username", username);
        jsonBody.add("data", metadata);

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(endpointUrl)
                .addHeader("apikey", this.apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Αποτυχία δικτύου: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    callback.onSuccess("Επιτυχής εγγραφή!");
                } else {
                    // Προσπαθούμε να πάρουμε το error message από το Supabase
                    try {
                        JsonObject json = JsonParser.parseString(responseData).getAsJsonObject();
                        String msg = json.has("msg") ? json.get("msg").getAsString()
                                : json.has("message") ? json.get("message").getAsString()
                                : "Σφάλμα εγγραφής (" + response.code() + ")";
                        callback.onError(msg);
                    } catch (Exception e) {
                        callback.onError("Σφάλμα εγγραφής (" + response.code() + ")");
                    }
                }
            }
        });
    }

    // --- LOGIN ---
    public void login(String email, String password, AuthCallback callback) {
        String endpointUrl = this.url + "/auth/v1/token?grant_type=password";

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("email", email);
        jsonBody.addProperty("password", password);

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(endpointUrl)
                .addHeader("apikey", this.apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Αποτυχία δικτύου: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        JsonObject json = JsonParser.parseString(responseData).getAsJsonObject();
                        JsonObject user = json.getAsJsonObject("user");
                        String userId = user.get("id").getAsString();
                        String email = user.get("email").getAsString();

                        // Παίρνουμε το username από τα metadata του signup
                        String username = email.split("@")[0]; // default
                        if (user.has("user_metadata")) {
                            JsonObject meta = user.getAsJsonObject("user_metadata");
                            if (meta.has("username")) {
                                username = meta.get("username").getAsString();
                            }
                        }

                        // Περνάμε όλα μαζί ως JSON string
                        JsonObject result = new JsonObject();
                        result.addProperty("userId", userId);
                        result.addProperty("email", email);
                        result.addProperty("username", username);
                        callback.onSuccess(result.toString());
                    } catch (Exception e) {
                        callback.onError("Σφάλμα ανάλυσης απάντησης.");
                    }
                } else {
                    try {
                        JsonObject json = JsonParser.parseString(responseData).getAsJsonObject();
                        String msg = json.has("error_description") ? json.get("error_description").getAsString()
                                : json.has("msg") ? json.get("msg").getAsString()
                                : "Σφάλμα σύνδεσης (" + response.code() + ")";
                        callback.onError(msg);
                    } catch (Exception e) {
                        callback.onError("Σφάλμα σύνδεσης (" + response.code() + ")");
                    }
                }
            }
        });
    }

    public void resetPassword(String email, AuthCallback callback) {
        callback.onError("Η επαναφορά κωδικού δεν υποστηρίζεται ακόμα.");
    }
}