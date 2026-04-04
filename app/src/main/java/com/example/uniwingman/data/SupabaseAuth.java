package com.example.uniwingman.data; // Βεβαιώσου ότι ταιριάζει με τον φάκελό σου

import okhttp3.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import io.github.cdimascio.dotenv.Dotenv;

public class SupabaseAuth {

    private String url;
    private String user;
    private String password;
    private final OkHttpClient client;

    public SupabaseAuth() {
        // Διαβάζει πλέον το αρχείο 'env' από τον φάκελο assets του Android
        Dotenv dotenv = Dotenv.configure()
                .directory("/assets")
                .filename("env") // Ψάχνει το 'env', όχι το '.env'
                .load();

        this.url = dotenv.get("DB_URL");
        this.user = dotenv.get("DB_USER");
        this.password = dotenv.get("DB_PASSWORD");

        // --- DEBUG PRINTS ΓΙΑ ΤΟ LOGCAT ---
        System.out.println("🔗 Σύνδεση με URL: " + this.url);
        System.out.println("🔑 ΤΟ ΚΛΕΙΔΙ ΠΟΥ ΔΙΑΒΑΣΕ ΕΙΝΑΙ: " + this.password);
        // ----------------------------------

        this.client = new OkHttpClient();
    }

    public interface AuthCallback {
        void onSuccess(String result);
        void onError(String errorMsg);
    }

    // --- SIGN UP ---
    public void signUp(String email, String userPassword, AuthCallback callback) {
        String endpointUrl = this.url + "/auth/v1/signup";

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("email", email);
        jsonBody.addProperty("password", userPassword);

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(endpointUrl)
                .addHeader("apikey", this.password) // Χρήση του DB_PASSWORD ως API Key
                .addHeader("Authorization", "Bearer " + this.password) // ΑΠΑΡΑΙΤΗΤΟ για να μην τρως 401
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
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess("Επιτυχής εγγραφή! (Έλεγξε το email σου)");
                } else {
                    callback.onError("Σφάλμα εγγραφής (Κωδικός): " + response.code());
                }
            }
        });
    }

    // --- LOGIN (Sign in) ---
    public void login(String email, String userPassword, AuthCallback callback) {
        String endpointUrl = this.url + "/auth/v1/token?grant_type=password";

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("email", email);
        jsonBody.addProperty("password", userPassword);

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(endpointUrl)
                .addHeader("apikey", this.password)
                .addHeader("Authorization", "Bearer " + this.password) // ΑΠΑΡΑΙΤΗΤΟ και εδώ
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
                if (response.isSuccessful() && response.body() != null) {
                    // Παίρνουμε το Access Token από το JSON της απάντησης
                    String responseData = response.body().string();
                    JsonObject jsonObject = JsonParser.parseString(responseData).getAsJsonObject();
                    String accessToken = jsonObject.get("access_token").getAsString();

                    callback.onSuccess(accessToken);
                } else {
                    callback.onError("Λάθος στοιχεία ή σφάλμα σύνδεσης. Κωδικός: " + response.code());
                }
            }
        });
    }
}