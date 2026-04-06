package com.example.uniwingman.ui.aisimulator;

import android.content.Context;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import io.github.cdimascio.dotenv.Dotenv;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatRepository {
    private final Context context;
    private JsonObject basicData;
    private final OkHttpClient client;
    private final String apiKey;

    private final List<JsonObject> thinkingHistory = new ArrayList<>();

    public static final String BASIC_GREETING    = "Basic Mode: Γρήγορες πληροφορίες για το ΟΠΑ.";
    public static final String THINKING_GREETING = "Critical Thinking: Έτοιμος για ανάλυση.";

    public interface AICallback {
        void onSuccess(String response);
        void onError(String error);
    }

    public ChatRepository(Context context) {
        this.context = context;
        this.client = new OkHttpClient();

        Dotenv dotenv = Dotenv.configure().directory("./assets").filename("env").load();
        this.apiKey = dotenv.get("DEEPSEEK_API_KEY");

        loadBasicInfo();
        initThinkingSystemMessage();
    }

    private void loadBasicInfo() {
        try {
            InputStream is = context.getAssets().open("basic_info.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            basicData = JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initThinkingSystemMessage() {
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "Είσαι ο UniWingman, βοηθός φοιτητών ΟΠΑ. Απαντάς με βάθος και ανάλυση στα ελληνικά.");
        thinkingHistory.add(systemMessage);
    }

    // --- NORMALIZATION & IMPROVED FUZZY LOGIC (Python-like Partial Match) ---

    private String normalize(String text) {
        if (text == null) return "";
        String nfdNormalizedString = Normalizer.normalize(text.toLowerCase().trim(), Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("");
    }

    private double calculateSimilarity(String query, String target) {
        if (query == null || target == null || query.isEmpty() || target.isEmpty()) return 0;

        String q = normalize(query);
        String t = normalize(target);

        // Exact substring match check (Immediate 100%)
        if (q.contains(t) || t.contains(q)) return 100.0;

        // Sliding Window for Partial Ratio simulation
        String shorter = q.length() < t.length() ? q : t;
        String longer = q.length() < t.length() ? t : q;

        double bestMatch = 0;
        int windowSize = shorter.length();

        for (int i = 0; i <= longer.length() - windowSize; i++) {
            String sub = longer.substring(i, i + windowSize);
            double currentSim = (windowSize - editDistance(sub, shorter)) / (double) windowSize * 100;
            if (currentSim > bestMatch) bestMatch = currentSim;
        }

        return bestMatch;
    }

    private int editDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) costs[j] = j;
                else if (j > 0) {
                    int newValue = costs[j - 1];
                    if (s1.charAt(i - 1) != s2.charAt(j - 1))
                        newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                    costs[j - 1] = lastValue;
                    lastValue = newValue;
                }
            }
            if (i > 0) costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    // --- ROUTING ---

    public void fetchBasicResponse(String prompt, AICallback callback) {
        if (basicData == null) {
            callback.onError("Basic info not loaded");
            return;
        }

        String q = normalize(prompt);
        JsonArray entries = basicData.getAsJsonObject("basic_info").getAsJsonArray("entries");
        JsonObject bestEntry = null;
        double bestScore = 0;

        for (JsonElement el : entries) {
            JsonObject entry = el.getAsJsonObject();
            double currentMax = 0;

            // 1. Tags Check
            if (entry.has("tags")) {
                for (JsonElement t : entry.getAsJsonArray("tags")) {
                    currentMax = Math.max(currentMax, calculateSimilarity(q, t.getAsString()));
                }
            }

            // 2. Topic/Title Check (1.2x weight)
            String label = entry.has("topic") ? entry.get("topic").getAsString() :
                    (entry.has("title") ? entry.get("title").getAsString() : "");
            currentMax = Math.max(currentMax, calculateSimilarity(q, label) * 1.2);

            // 3. Professors Check (1.3x weight)
            if (entry.has("category") && entry.get("category").getAsString().equals("καθηγητές")) {
                JsonObject details = entry.getAsJsonObject("details");
                for (JsonElement p : details.getAsJsonArray("professors")) {
                    String name = p.getAsJsonObject().get("name").getAsString();
                    currentMax = Math.max(currentMax, calculateSimilarity(q, name) * 1.3);
                }
            }

            if (currentMax > bestScore) {
                bestScore = currentMax;
                bestEntry = entry;
            }
        }

        if (bestScore >= 55 && bestEntry != null) {
            callback.onSuccess(formatAnswer(bestEntry, prompt));
        } else {
            callback.onSuccess("Δεν βρέθηκε σχετική πληροφορία. Δοκίμασε το Thinking Model!");
        }
    }

    public void fetchThinkingResponse(String prompt, AICallback callback) {
        String url = "https://api.deepseek.com/chat/completions";
        addToThinkingHistory("user", prompt);

        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("model", "deepseek-reasoner");
        JsonArray messagesArray = new JsonArray();
        for (JsonObject msg : thinkingHistory) messagesArray.add(msg);
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
                if (!thinkingHistory.isEmpty()) thinkingHistory.remove(thinkingHistory.size() - 1);
                callback.onError("Σφάλμα δικτύου: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    JsonObject jsonObject = JsonParser.parseString(responseData).getAsJsonObject();
                    String aiText = jsonObject.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
                    addToThinkingHistory("assistant", aiText);
                    callback.onSuccess(aiText);
                } else {
                    if (!thinkingHistory.isEmpty()) thinkingHistory.remove(thinkingHistory.size() - 1);
                    callback.onError("Σφάλμα API: " + response.code());
                }
            }
        });
    }

    private String formatAnswer(JsonObject entry, String query) {
        String category = entry.has("category") ? entry.get("category").getAsString() : "";
        if (entry.has("answer")) return entry.get("answer").getAsString();

        if (category.equals("καθηγητές")) {
            JsonArray professors = entry.getAsJsonObject("details").getAsJsonArray("professors");
            StringBuilder result = new StringBuilder();
            boolean found = false;
            for (JsonElement el : professors) {
                JsonObject p = el.getAsJsonObject();
                if (calculateSimilarity(query, p.get("name").getAsString()) >= 65) {
                    result.append("\n").append(p.get("name").getAsString()).append(" (").append(p.get("role").getAsString()).append(")\n");
                    if (p.has("office")) result.append("   Γραφείο: ").append(p.get("office").getAsString()).append("\n");
                    if (p.has("phone")) result.append("   Τηλ: ").append(p.get("phone").getAsString()).append("\n");
                    if (p.has("email")) result.append("   Email: ").append(p.get("email").getAsString()).append("\n");
                    result.append("   Ώρες: ").append(p.get("hours").getAsString()).append("\n");
                    found = true;
                }
            }
            return found ? result.toString().trim() : "Βρέθηκαν καθηγητές πληροφορικής. Ρώτα για συγκεκριμένο όνομα!";
        }
        return entry.has("info") ? entry.get("info").getAsString() : "Δεν βρέθηκε απάντηση.";
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