package com.example.uniwingman.ui.aisimulator;

import android.content.Context;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.*;

public class ChatRepository {
    private final Context context;
    private JsonObject basicData;
    private final OkHttpClient client = new OkHttpClient();
    private String apiKey;
    private final List<JsonObject> thinkingHistory = new ArrayList<>();

    public interface AICallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public ChatRepository(Context context) {
        this.context = context;
        loadApiKey();
        loadBasicInfo();
        initThinkingSystemMessage();
    }

    private void loadApiKey() {
        try {
            Dotenv dotenv = Dotenv.configure().directory("./assets").filename("env").load();
            this.apiKey = dotenv.get("DEEPSEEK_API_KEY");
        } catch (Exception e) { this.apiKey = ""; }
    }

    private void loadBasicInfo() {
        try (InputStream is = context.getAssets().open("basic_info.json")) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            basicData = JsonParser.parseString(new String(buffer, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase().trim()
                .replace("ά", "α").replace("έ", "ε").replace("ή", "η")
                .replace("ί", "ι").replace("ό", "ο").replace("ύ", "υ")
                .replace("ώ", "ω").replace("ϊ", "ι").replace("ϋ", "υ")
                .replace("ΐ", "ι").replace("ΰ", "υ")
                .replaceAll("[^a-z0-9α-ω\\s]", "");
    }

    public void fetchBasicResponse(String prompt, AICallback callback) {
        if (basicData == null) { callback.onError("Σφάλμα φόρτωσης JSON"); return; }

        JsonObject knowledgeBase = basicData.getAsJsonObject("knowledge_base");
        JsonObject bestEntry = null;
        double bestScore = 0;
        String q = normalize(prompt);

        for (Map.Entry<String, JsonElement> categoryEntry : knowledgeBase.entrySet()) {
            if (categoryEntry.getValue().isJsonArray()) {
                JsonArray items = categoryEntry.getValue().getAsJsonArray();
                for (JsonElement el : items) {
                    JsonObject entry = el.getAsJsonObject();
                    double score = calculateScore(entry, q);
                    if (score > bestScore) {
                        bestScore = score;
                        bestEntry = entry;
                    }
                }
            }
        }

        if (bestScore >= 55 && bestEntry != null) {
            callback.onSuccess(formatAnswer(bestEntry, prompt));
        } else {
            callback.onSuccess("Δεν βρήκα κάτι συγκεκριμένο. (Score: " + (int)bestScore + ")");
        }
    }

    private double calculateScore(JsonObject entry, String q) {
        double score = 0;
        String entryTopic = entry.has("topic") ? entry.get("topic").getAsString() : "Unknown";

        if (entry.has("keywords")) {
            for (JsonElement k : entry.getAsJsonArray("keywords")) {
                String key = normalize(k.getAsString());
                if (q.contains(key)) {
                    double weight = 100.0;
                    if (key.equals("οπα") || key.equals("email") || key.equals("πληροφορικη") || key.equals("πατησιων")) {
                        weight = 25.0;
                    }
                    if (key.equals("γραμματεια") || key.equals("φαγητο") || key.equals("βιβλια") ||
                            key.equals("δηλωση μαθηματων") || key.equals("δηλωση βιβλιων") || key.equals("ευδοξος")) {
                        weight = 500.0;
                    }
                    score += weight + (key.length() * 15);
                }
            }
        }

        // Professor Nuclear Boost με Clean Diagnostic Prints
        if (entry.has("professors")) {
            for (JsonElement p : entry.getAsJsonArray("professors")) {
                String fullName = normalize(p.getAsJsonObject().get("name").getAsString());
                String lastName = fullName.split("\\s+")[0];
                String root = lastName.substring(0, Math.min(lastName.length(), 7));

                if (q.contains(root) && root.length() > 3) {
                    Log.d("UNI_DEBUG", "--------------------------------------------------");
                    Log.d("UNI_DEBUG", "🎯 PROFESSOR MATCH FOUND");
                    Log.d("UNI_DEBUG", "👤 Name: " + lastName);
                    Log.d("UNI_DEBUG", "🔑 Root: " + root);
                    Log.d("UNI_DEBUG", "🚀 Score: 10000.0 (NUCLEAR)");
                    Log.d("UNI_DEBUG", "--------------------------------------------------");
                    return 10000.0;
                }
            }
        }

        if (score > 0) {
            Log.d("UNI_DEBUG", String.format("📊 Entry: %-30s | Score: %6.1f", entryTopic, score));
        }
        return score;
    }

    private String formatAnswer(JsonObject entry, String query) {
        if (entry.has("professors")) {
            StringBuilder sb = new StringBuilder();
            String q = normalize(query);
            boolean foundSpecific = false;

            for (JsonElement el : entry.getAsJsonArray("professors")) {
                JsonObject p = el.getAsJsonObject();
                String name = p.get("name").getAsString();
                String lastName = normalize(name).split("\\s+")[0];
                String root = lastName.substring(0, Math.min(lastName.length(), 7));

                if (q.contains(root)) {
                    if (foundSpecific) sb.append("\n\n---\n\n");
                    sb.append("👤 **").append(name).append("**\n")
                            .append("🎓 ").append(p.get("role").getAsString()).append("\n")
                            .append("📍 ").append(p.get("office").getAsString()).append("\n")
                            .append("✉️ ").append(p.get("email").getAsString()).append("\n")
                            .append("🕒 ").append(p.get("hours").getAsString());
                    foundSpecific = true;
                }
            }
            if (foundSpecific) return sb.toString();
        }

        return entry.has("full_answer") ? entry.get("full_answer").getAsString() : "Σφάλμα μορφοποίησης.";
    }

    private void initThinkingSystemMessage() {
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "system");
        msg.addProperty("content", "Είσαι ο UniWingman, βοηθός φοιτητών ΟΠΑ.");
        thinkingHistory.add(msg);
    }

    public void fetchThinkingResponse(String prompt, AICallback callback) {
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", prompt);
        thinkingHistory.add(userMsg);

        JsonObject bodyJson = new JsonObject();
        bodyJson.addProperty("model", "deepseek-reasoner");
        JsonArray msgs = new JsonArray();
        for (JsonObject m : thinkingHistory) msgs.add(m);
        bodyJson.add("messages", msgs);

        Request request = new Request.Builder()
                .url("https://api.deepseek.com/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(bodyJson.toString(), MediaType.get("application/json")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { callback.onError(e.getMessage()); }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String body = response.body().string();
                        String aiText = JsonParser.parseString(body)
                                .getAsJsonObject().getAsJsonArray("choices").get(0).getAsJsonObject()
                                .getAsJsonObject("message").get("content").getAsString();
                        callback.onSuccess(aiText);
                    } catch (Exception e) { callback.onError("Parsing error"); }
                } else { callback.onError("Error: " + response.code()); }
            }
        });
    }

    public void clearThinkingHistory() {
        thinkingHistory.clear();
        initThinkingSystemMessage();
    }
}