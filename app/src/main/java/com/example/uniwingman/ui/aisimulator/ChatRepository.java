package com.example.uniwingman.ui.aisimulator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatRepository {
    private final OkHttpClient client;
    private final String apiKey;

    // Separate API history for each mode
    private final List<JsonObject> thinkingHistory = new ArrayList<>();

    public static final String BASIC_GREETING    = "Basic Mode: Γρήγορες πληροφορίες για το ΟΠΑ.";
    public static final String THINKING_GREETING = "Citical Thinking: Έτοιμος για ανάλυση.";

    public interface AICallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public ChatRepository() {
        this.client = new OkHttpClient();
        Dotenv dotenv = Dotenv.configure().directory("./assets").filename("env").load();
        this.apiKey = dotenv.get("DEEPSEEK_API_KEY");
        initThinkingSystemMessage();
    }

    private void initThinkingSystemMessage() {
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "Είσαι ο UniWingman, βοηθός φοιτητών ΟΠΑ. Απαντάς με βάθος και ανάλυση.");
        thinkingHistory.add(systemMessage);
    }

    // --- ROUTING ---
    public void fetchBasicResponse(String prompt, AICallback callback) {
        handleBasicResponse(prompt, callback);
    }

    public void fetchThinkingResponse(String prompt, AICallback callback) {
        handleCriticalResponse(prompt, callback);
    }

    // --- LOCAL / BASIC LOGIC (OFFLINE) ---
    private void handleBasicResponse(String prompt, AICallback callback) {
        String response;
        String input = prompt.toLowerCase();

        if (input.contains("πρόγραμμα") || input.contains("μαθήματα")) {
            response = "[Basic] Το πρόγραμμα σπουδών του ΟΠΑ βρίσκεται στο επίσημο site (aueb.gr).";
        } else if (input.contains("λέσχη") || input.contains("φαγητό")) {
            response = "[Basic] Η λέσχη του ΟΠΑ λειτουργεί καθημερινά 12:00-16:00.";
        } else {
            response = "[Basic Mode] Έλαβα το μήνυμά σου: \"" + prompt + "\". Για πιο έξυπνες απαντήσεις, ενεργοποίησε το Thinking Model!";
        }

        callback.onSuccess(response);
    }

    // --- SMART / CRITICAL LOGIC (ONLINE API) ---
    private void handleCriticalResponse(String prompt, AICallback callback) {
        String url = "https://api.deepseek.com/chat/completions";

        addToThinkingHistory("user", prompt);

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("model", "deepseek-reasoner");

        JsonArray messagesArray = new JsonArray();
        for (JsonObject msg : thinkingHistory) {
            messagesArray.add(msg);
        }
        jsonBody.add("messages", messagesArray);

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Roll back the user message on failure
                if (!thinkingHistory.isEmpty()) {
                    thinkingHistory.remove(thinkingHistory.size() - 1);
                }
                callback.onError("Σφάλμα δικτύου: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    JsonObject jsonObject = JsonParser.parseString(responseData).getAsJsonObject();
                    String aiText = jsonObject.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();
                    addToThinkingHistory("assistant", aiText);
                    callback.onSuccess(aiText);
                } else {
                    if (!thinkingHistory.isEmpty()) {
                        thinkingHistory.remove(thinkingHistory.size() - 1);
                    }
                    callback.onError("Σφάλμα API: " + response.code());
                }
            }
        });
    }

    public void clearThinkingHistory() {
        thinkingHistory.clear();
        initThinkingSystemMessage();
    }

    private void addToThinkingHistory(String role, String content) {
        JsonObject msg = new JsonObject();
        msg.addProperty("role", role);
        msg.addProperty("content", content);
        thinkingHistory.add(msg);
    }
}
