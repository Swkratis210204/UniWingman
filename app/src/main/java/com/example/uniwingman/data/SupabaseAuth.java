package com.example.uniwingman.data;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.mindrot.jbcrypt.BCrypt; // Η βιβλιοθήκη κρυπτογράφησης!
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

    // --- SIGN UP (Εγγραφή στον δικό μας πίνακα) ---
    public void signUp(String username, String email, String plainPassword, AuthCallback callback) {
        String endpointUrl = this.url + "/rest/v1/users";

        // 1. ΚΡΥΠΤΟΓΡΑΦΗΣΗ ΤΟΥ ΚΩΔΙΚΟΥ!
        String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());

        // 2. Δημιουργία των δεδομένων που θα σταλούν στη βάση
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("username", username);
        jsonBody.addProperty("email", email);
        jsonBody.addProperty("password", hashedPassword); // Στέλνουμε τον κρυπτογραφημένο

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(endpointUrl)
                .addHeader("apikey", this.apiKey)
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation") // Μας επιστρέφει το ID που μόλις δημιουργήθηκε
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Αποτυχία δικτύου: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    callback.onSuccess("Επιτυχής εγγραφή!");
                } else {
                    callback.onError("Σφάλμα! Ίσως το email υπάρχει ήδη. (" + response.code() + ")");
                }
            }
        });
    }

    // --- LOGIN (Σύνδεση ελέγχοντας τον δικό μας πίνακα) ---
    public void login(String email, String plainPassword, AuthCallback callback) {
        // Ζητάμε από τη βάση τον χρήστη με αυτό το email
        String endpointUrl = this.url + "/rest/v1/users?email=eq." + email + "&select=id,password,username";

        Request request = new Request.Builder()
                .url(endpointUrl)
                .addHeader("apikey", this.apiKey)
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Αποτυχία δικτύου: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError("Σφάλμα επικοινωνίας με τη βάση.");
                    return;
                }

                String responseData = response.body().string();
                JsonArray jsonArray = JsonParser.parseString(responseData).getAsJsonArray();

                // Αν το Array είναι άδειο, το email δεν υπάρχει στη βάση
                if (jsonArray.size() == 0) {
                    callback.onError("Το email δεν βρέθηκε.");
                    return;
                }

                // Παίρνουμε τα στοιχεία του χρήστη
                JsonObject userObj = jsonArray.get(0).getAsJsonObject();
                String dbHashedPassword = userObj.get("password").getAsString();
                String userId = userObj.get("id").getAsString();

                // 3. ΕΛΕΓΧΟΣ ΚΩΔΙΚΟΥ! (Συγκρίνουμε αυτό που έγραψε με το Hash της βάσης)
                boolean isPasswordCorrect = BCrypt.checkpw(plainPassword, dbHashedPassword);

                if (isPasswordCorrect) {
                    callback.onSuccess(userId); // Επιστρέφουμε το ID του για μελλοντική χρήση
                } else {
                    callback.onError("Λάθος κωδικός πρόσβασης.");
                }
            }
        });
    }

    // Το Reset Password προς το παρόν το απενεργοποιούμε, γιατί χρειάζεται custom backend λειτουργία όταν φτιάχνουμε δικό μας πίνακα.
    public void resetPassword(String email, AuthCallback callback) {
        callback.onError("Η επαναφορά κωδικού δεν υποστηρίζεται ακόμα με Custom Auth.");
    }
}