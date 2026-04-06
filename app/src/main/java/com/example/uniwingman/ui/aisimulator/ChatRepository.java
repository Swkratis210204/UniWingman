package com.example.uniwingman.ui.aisimulator;

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

public class ChatRepository {
    private boolean isThinkingMode = false;
    private final OkHttpClient client;
    private final String apiKey;

    // Interface για να στείλουμε την απάντηση πίσω στο ViewModel
    public interface AICallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public ChatRepository() {
        this.client = new OkHttpClient();

        // Φόρτωση του API Key από το .env αρχείο
        Dotenv dotenv = Dotenv.configure()
                .directory("./assets")
                .filename("env") // ή ".env" ανάλογα πώς το έχεις ονομάσει
                .load();
        this.apiKey = dotenv.get("DEEPSEEK_API_KEY");
    }

    public void setThinkingMode(boolean enabled) {
        this.isThinkingMode = enabled;
    }

    public String getInitialGreeting() {
        return isThinkingMode
                ? "Γεια! Είμαι το DeepSeek Reasoner (Βαθιά Σκέψη). Πώς μπορώ να βοηθήσω;"
                : "Γεια! Είμαι το DeepSeek Chat (Γρήγορο). Ρώτα με οτιδήποτε για το ΟΠΑ!";
    }

    public void fetchAIResponse(String prompt, AICallback callback) {
        // Το επίσημο URL του DeepSeek
        String url = "https://api.deepseek.com/chat/completions";

        JsonObject jsonBody = new JsonObject();

        // Αν το toggle είναι ενεργό χρησιμοποιούμε το reasoner, αλλιώς το απλό chat
        String modelName = isThinkingMode ? "deepseek-reasoner" : "deepseek-chat";
        jsonBody.addProperty("model", modelName);

        JsonArray messagesArray = new JsonArray();

        // 1. Το System Prompt (δίνουμε ρόλο στο AI)
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "Είσαι ο UniWingman, ένας φιλικός ψηφιακός βοηθός για τους φοιτητές του Οικονομικού Πανεπιστημίου Αθηνών (ΟΠΑ / AUEB). Απαντάς σύντομα, ευγενικά και στα ελληνικά.");
        messagesArray.add(systemMessage);

        // 2. Το μήνυμα του χρήστη
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", prompt);
        messagesArray.add(userMessage);

        jsonBody.add("messages", messagesArray);

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                // Εδώ μπαίνει το κλειδί σου για αυθεντικοποίηση
                .addHeader("Authorization", "Bearer " + this.apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Σφάλμα δικτύου: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    JsonObject jsonObject = JsonParser.parseString(responseData).getAsJsonObject();

                    // Εξαγωγή του κειμένου της απάντησης
                    String aiText = jsonObject.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();

                    callback.onSuccess(aiText);
                } else {
                    callback.onError("Σφάλμα API (Κωδικός): " + response.code());
                }
            }
        });
    }
}