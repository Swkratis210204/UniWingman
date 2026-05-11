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
import java.io.IOException;
import io.github.cdimascio.dotenv.Dotenv;

public class SupabaseAuth {

    private final String url;
    private final String apiKey;
    private final OkHttpClient client;

    public SupabaseAuth() {
        Dotenv dotenv = Dotenv.configure()
                .directory("./assets")
                .filename("env")
                .load();
        this.url    = dotenv.get("DB_URL");
        this.apiKey = dotenv.get("DB_PASSWORD");
        this.client = new OkHttpClient();
    }

    public interface AuthCallback {
        void onSuccess(String result);
        void onError(String errorMsg);
    }

    // ─────────────────────────────────────────────
    //  SIGN UP
    //  1. Supabase Auth signup  (/auth/v1/signup)
    //  2. Insert στο public.users
    // ─────────────────────────────────────────────
    public void signUp(String username, String email, String password, AuthCallback callback) {
        String endpointUrl = this.url + "/auth/v1/signup";

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("email", email);
        jsonBody.addProperty("password", password);

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
                if (!response.isSuccessful()) {
                    try {
                        JsonObject json = JsonParser.parseString(responseData).getAsJsonObject();
                        String msg = json.has("msg") ? json.get("msg").getAsString()
                                : json.has("message") ? json.get("message").getAsString()
                                : "Σφάλμα εγγραφής (" + response.code() + ")";
                        callback.onError(msg);
                    } catch (Exception e) {
                        callback.onError("Σφάλμα εγγραφής (" + response.code() + ")");
                    }
                    return;
                }

                // Auth signup επιτυχές — παίρνουμε το userId και κάνουμε insert στο public.users
                try {
                    JsonObject json = JsonParser.parseString(responseData).getAsJsonObject();
                    String userId = null;

                    // Supabase επιστρέφει είτε { "id": "..." } είτε { "user": { "id": "..." } }
                    if (json.has("id")) {
                        userId = json.get("id").getAsString();
                    } else if (json.has("user") && !json.get("user").isJsonNull()) {
                        userId = json.getAsJsonObject("user").get("id").getAsString();
                    }

                    if (userId == null) {
                        // Πιθανώς χρειάζεται email confirmation — δεν επιστρέφει user αμέσως
                        callback.onSuccess("Επιτυχής εγγραφή! Παρακαλώ επιβεβαιώστε το email σας.");
                        return;
                    }

                    insertPublicUser(userId, username, email, password, callback);

                } catch (Exception e) {
                    callback.onSuccess("Επιτυχής εγγραφή!");
                }
            }
        });
    }

    // Insert στο public.users μετά το Auth signup
    private void insertPublicUser(String userId, String username, String email,
                                  String password, AuthCallback callback) {
        String insertUrl = this.url + "/rest/v1/users";

        JsonObject userBody = new JsonObject();
        userBody.addProperty("id", userId);
        userBody.addProperty("username", username);
        userBody.addProperty("email", email);
        userBody.addProperty("password", password);
        // department και current_semester αφήνονται null — ο χρήστης θα τα συμπληρώσει αργότερα

        RequestBody body = RequestBody.create(
                userBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(insertUrl)
                .addHeader("apikey", this.apiKey)
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onSuccess("Επιτυχής εγγραφή!");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) response.body().close();
                callback.onSuccess("Επιτυχής εγγραφή!");
            }
        });
    }

    // ─────────────────────────────────────────────
    //  LOGIN
    //  1. Supabase Auth login   (/auth/v1/token)
    //  2. Fetch από public.users για username, department, current_semester
    // ─────────────────────────────────────────────
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
                if (!response.isSuccessful()) {
                    try {
                        JsonObject json = JsonParser.parseString(responseData).getAsJsonObject();
                        String msg = json.has("error_description") ? json.get("error_description").getAsString()
                                : json.has("msg") ? json.get("msg").getAsString()
                                : "Σφάλμα σύνδεσης (" + response.code() + ")";
                        callback.onError(msg);
                    } catch (Exception e) {
                        callback.onError("Σφάλμα σύνδεσης (" + response.code() + ")");
                    }
                    return;
                }

                try {
                    JsonObject json      = JsonParser.parseString(responseData).getAsJsonObject();
                    String accessToken   = json.get("access_token").getAsString();
                    JsonObject user      = json.getAsJsonObject("user");
                    String userId        = user.get("id").getAsString();

                    // Fetch public.users με το access_token
                    fetchPublicUser(userId, accessToken, callback);

                } catch (Exception e) {
                    callback.onError("Σφάλμα ανάλυσης απάντησης.");
                }
            }
        });
    }

    // Fetch χρήστη από public.users χρησιμοποιώντας το access_token
    private void fetchPublicUser(String userId, String accessToken, AuthCallback callback) {
        String fetchUrl = this.url + "/rest/v1/users?id=eq." + userId
                + "&select=id,username,email,department,current_semester&limit=1";

        Request request = new Request.Builder()
                .url(fetchUrl)
                .addHeader("apikey", this.apiKey)
                .addHeader("Authorization", "Bearer " + accessToken)
                .addHeader("Content-Type", "application/json")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Fallback χωρίς public.users data
                JsonObject result = new JsonObject();
                result.addProperty("userId", userId);
                result.addProperty("email", "");
                result.addProperty("username", "");
                result.addProperty("department", "");
                result.addProperty("currentSemester", 0);
                result.addProperty("accessToken", accessToken);
                callback.onSuccess(result.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "[]";
                try {
                    JsonArray arr = JsonParser.parseString(responseData).getAsJsonArray();

                    JsonObject result = new JsonObject();
                    result.addProperty("userId", userId);
                    result.addProperty("accessToken", accessToken);

                    if (arr.size() > 0) {
                        JsonObject row = arr.get(0).getAsJsonObject();
                        result.addProperty("email",
                                row.has("email") && !row.get("email").isJsonNull()
                                        ? row.get("email").getAsString() : "");
                        result.addProperty("username",
                                row.has("username") && !row.get("username").isJsonNull()
                                        ? row.get("username").getAsString() : "");
                        result.addProperty("department",
                                row.has("department") && !row.get("department").isJsonNull()
                                        ? row.get("department").getAsString() : "");
                        result.addProperty("currentSemester",
                                row.has("current_semester") && !row.get("current_semester").isJsonNull()
                                        ? row.get("current_semester").getAsInt() : 0);
                    } else {
                        result.addProperty("email", "");
                        result.addProperty("username", "");
                        result.addProperty("department", "");
                        result.addProperty("currentSemester", 0);
                    }

                    callback.onSuccess(result.toString());

                } catch (Exception e) {
                    JsonObject result = new JsonObject();
                    result.addProperty("userId", userId);
                    result.addProperty("email", "");
                    result.addProperty("username", "");
                    result.addProperty("accessToken", accessToken);
                    callback.onSuccess(result.toString());
                }
            }
        });
    }

    public void resetPassword(String email, AuthCallback callback) {
        callback.onError("Η επαναφορά κωδικού δεν υποστηρίζεται ακόμα.");
    }

    public interface AdminCheckCallback {
        void onResult(boolean isAdmin);
        void onError(String error);
    }

    // for this to work we need to go to the supabase dashboard and add a collumn type:boolean named is_admin
    public void checkIfUserIsAdmin(String userId, AdminCheckCallback callback) {

        String fetchUrl = this.url + "/rest/v1/users?id=eq." + userId + "&select=is_admin&limit=1";

        Request request = new Request.Builder()
                .url(fetchUrl)
                .addHeader("apikey", this.apiKey)
                // Προσοχή: Εδώ ιδανικά θα έπρεπε να χρησιμοποιούμε το accessToken του χρήστη
                // αλλά επειδή έχουμε το RLS policy (SELECT ... WHERE id = auth.uid() = true),
                // το apiKey (anon key) μπορεί να χτυπήσει σε τοίχο αν δεν έχουμε το token.
                // Θα το δοκιμάσουμε ως έχει πρώτα.
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .addHeader("Content-Type", "application/json")
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
                    callback.onError("Σφάλμα διακομιστή: " + response.code());
                    return;
                }
                String responseData = response.body().string();
                try {
                    JsonArray arr = JsonParser.parseString(responseData).getAsJsonArray();
                    if (arr.size() > 0) {
                        JsonObject row = arr.get(0).getAsJsonObject();
                        boolean isAdmin = row.has("is_admin") && !row.get("is_admin").isJsonNull()
                                && row.get("is_admin").getAsBoolean();
                        callback.onResult(isAdmin);
                    } else {
                        callback.onResult(false);
                    }
                } catch (Exception e) {
                    callback.onError("Σφάλμα ανάλυσης δεδομένων admin.");
                }
            }
        });
    }
}