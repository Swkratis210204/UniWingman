package com.example.uniwingman.ui.aisimulator;

import android.content.Context;
import android.util.Log;

import com.example.uniwingman.data.StudentProfileRepository;
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
    private static final String TAG = "ChatRepository";

    private final Context context;
    private JsonObject basicData;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    // API Keys
    private String geminiApiKey;
    private String supabaseUrl;
    private String supabaseKey;

    // Student profile
    private final StudentProfileRepository profileRepo;
    private boolean profileReady = false;

    // Conversation history για το online chatbot
    private final List<JsonObject> conversationHistory = new ArrayList<>();

    // Gemini API endpoints
    private static final String GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta";
    private static final String EMBEDDING_MODEL = "models/gemini-embedding-001";
    private static final String CHAT_MODEL = "gemini-2.0-flash";
    private static final int TOP_K = 10;

    // Base system prompt (profile will be prepended at runtime)
    private static final String BASE_SYSTEM_PROMPT =
            "Είσαι βοηθός φοιτητών για το Τμήμα Πληροφορικής του ΟΠΑ (UniWingman).\n" +
                    "Απάντησε στην ερώτηση του φοιτητή χρησιμοποιώντας ΜΟΝΟ τις πληροφορίες από το παρακάτω context.\n" +
                    "Αν δεν έχεις αρκετές πληροφορίες, πες: 'Δεν έχω αρκετές πληροφορίες για να απαντήσω πλήρως.'\n" +
                    "ΜΗΝ εφευρίσκεις πληροφορίες που δεν υπάρχουν στο context.\n" +
                    "Απάντα στα Ελληνικά, σύντομα και ξεκάθαρα.\n\n" +
                    "ΣΗΜΑΝΤΙΚΟ για τα εξάμηνα:\n" +
                    "- Χειμερινά εξάμηνα: 1ο, 3ο, 5ο, 7ο. Εαρινά: 2ο, 4ο, 6ο, 8ο.\n" +
                    "- Χειμερινά μαθήματα προσφέρονται ΜΟΝΟ σε χειμερινά εξάμηνα, εαρινά ΜΟΝΟ σε εαρινά.\n" +
                    "- Μάθημα 5ου μπορεί να παρθεί και στο 7ο, μάθημα 6ου και στο 8ο κ.ο.κ.\n" +
                    "- Όταν αναφέρεις μαθήματα 5ου εξαμήνου πρόσθεσε '(μπορείς να το δηλώσεις και στο 7ο)'.\n" +
                    "- Όταν αναφέρεις μαθήματα 6ου εξαμήνου πρόσθεσε '(μπορείς να το δηλώσεις και στο 8ο)'.\n" +
                    "- Μαθήματα Ζ/Η μπορούν να παρθούν είτε στο 7ο είτε στο 8ο.\n" +
                    "- Αν κοπείς σε χειμερινό μάθημα, το ξαναπαίρνεις τον επόμενο χειμώνα ή Σεπτέμβριο.\n" +
                    "- Όταν απαντάς για μεταπτυχιακά, συμπεριέλαβε πάντα τον ιστότοπο αν υπάρχει στο context.\n" +
                    "Να απαντάς πάντα σύντομα και περιεκτικά.\n" +
                    "Όταν αναφέρεις λίστα μαθημάτων, χρησιμοποίησε απλή λίστα με - χωρίς επιπλέον εξηγήσεις.\n" +
                    "ΜΗΝ προσθέτεις εισαγωγικές προτάσεις ή περιττές επεξηγήσεις — απλά δώσε την απάντηση.\n" +
                    "ΜΗΝ χρησιμοποιείς markdown (**, ##, αριθμημένες λίστες). Μόνο απλό κείμενο και - για λίστες.\n" +
                    "Χρησιμοποίησε το ΠΡΟΦΙΛ ΦΟΙΤΗΤΗ (αν υπάρχει παραπάνω) για εξατομικευμένες απαντήσεις.\n" +
                    "Αν η ερώτηση αφορά τον συγκεκριμένο φοιτητή (π.χ. 'ποιος είναι ο ΜΟ μου', 'τι μαθήματα έχω'), " +
                    "απάντησε με βάση το προφίλ του, όχι γενικά.\n" +
                    "\nΣΗΜΑΝΤΙΚΟ για κύκλους σπουδών:\n" +
                    "- Κάθε κύκλος απαιτεί 5 μαθήματα για να κλείσει.\n" +
                    "- Ένα μάθημα που ανήκει σε ΠΟΛΛΟΥΣ κύκλους μπορεί να δηλωθεί ΜΟΝΟ σε ΕΝΑΝ κύκλο κατά την κατάθεση πτυχίου.\n" +
                    "- Η επιλογή σε ποιον κύκλο θα δηλωθεί ένα κοινό μάθημα είναι ΚΡΙΣΙΜΗ για τη στρατηγική αποφοίτησης.\n" +
                    "- Παράδειγμα: αν έχεις 5 μαθήματα Κύκλου 7 και 2 μαθήματα Κύκλου 1, και 3 ανήκουν και στους δύο, " +
                    "μπορείς να τα μοιράσεις ώστε να κλείσουν και οι δύο με 5+5.\n" +
                    "- Η Ερευνητική Εργασία και η Πτυχιακή Εργασία μετράνε ως μαθήματα κύκλου σε ΟΛΟΥΣ τους κύκλους.\n" +
                    "- ΚΡΙΣΙΜΟ: Χρησιμοποίησε ΑΥΣΤΗΡΑ την ΑΝΑΛΥΣΗ ΚΥΚΛΩΝ ΣΠΟΥΔΩΝ από το προφίλ — μην παραλείπεις κανένα μάθημα.\n" +
                    "- Τα 'Περασμένα' και τα 'Δηλωμένα' είναι ΕΝΤΕΛΩΣ ΔΙΑΦΟΡΕΤΙΚΑ:\n" +
                    "  * Περασμένα = έχουν ήδη βαθμολογηθεί και μετράνε οριστικά.\n" +
                    "  * Δηλωμένα = είναι σε εξέλιξη φέτος, ΔΕΝ έχουν περαστεί ακόμα.\n" +
                    "  * Όταν λες πόσα έχει περάσει, μέτρα ΜΟΝΟ τα Περασμένα.\n" +
                    "  * Όταν προτείνεις στρατηγική, μπορείς να αναφέρεις τα Δηλωμένα ως 'υπό εξέλιξη'.\n" +
                    "- Κάθε μάθημα να αναφέρεται ΜΙΑ ΦΟΡΑ στην απάντηση. Αν ανήκει σε πολλούς κύκλους, " +
                    "σημείωσέ το ως '(Κύκλος X ή Y)' αντί να το επαναλαμβάνεις.\n";

    // Personal question keywords for basic mode
    private static final String[] PERSONAL_KEYWORDS = {
            "μο μου", "μέσος όρος μου", "βαθμολογία μου", "βαθμοί μου",
            "ects μου", "μαθήματα μου", "μαθήματά μου", "εξάμηνο μου",
            "πέρασα", "κόπηκα", "δηλωμένα", "ολοκλήρωσα",
            "πρόοδός μου", "πρόοδος μου", "πτυχίο μου", "τι μου λείπει"
    };

    public interface AICallback {
        void onSuccess(String response);
        void onError(String error);
    }

    // ─────────────────────────────────────────────
    //  Constructor — accepts StudentProfileRepository
    // ─────────────────────────────────────────────
    public ChatRepository(Context context, StudentProfileRepository profileRepo) {
        this.context = context;
        this.profileRepo = profileRepo;
        loadCredentials();
        loadBasicInfo();
        // Load profile once on init
        loadProfileAsync();
    }

    // Backwards-compatible constructor (no profile)
    public ChatRepository(Context context) {
        this(context, new StudentProfileRepository(context));
    }

    private void loadProfileAsync() {
        profileRepo.loadProfile(new StudentProfileRepository.ProfileCallback() {
            @Override
            public void onReady(String profileString) {
                profileReady = true;
                Log.d(TAG, "Student profile ready:\n" + profileString);
            }

            @Override
            public void onError(String error) {
                profileReady = false;
                Log.w(TAG, "Profile load failed: " + error);
            }
        });
    }

    // ─────────────────────────────────────────────
    //  CREDENTIALS & INIT
    // ─────────────────────────────────────────────
    private void loadCredentials() {
        try {
            Dotenv dotenv = Dotenv.configure().directory("./assets").filename("env").load();
            this.geminiApiKey = dotenv.get("GEMINI_API_KEY");
            this.supabaseUrl = dotenv.get("DB_URL");
            this.supabaseKey = dotenv.get("SUPABASE_KEY");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load credentials: " + e.getMessage());
            this.geminiApiKey = "";
            this.supabaseUrl = "";
            this.supabaseKey = "";
        }
    }

    private void loadBasicInfo() {
        try (InputStream is = context.getAssets().open("basic_info.json")) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            basicData = JsonParser.parseString(new String(buffer, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────
    //  OFFLINE (BASIC) MODE
    // ─────────────────────────────────────────────
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
        // Pre-check: is this a personal question?
        String normalizedPrompt = normalize(prompt);
        if (isPersonalQuestion(normalizedPrompt)) {
            String profileAnswer = answerFromProfile(normalizedPrompt);
            if (profileAnswer != null) {
                callback.onSuccess(profileAnswer);
                return;
            }
        }

        if (basicData == null) { callback.onError("Σφάλμα φόρτωσης JSON"); return; }

        JsonObject knowledgeBase = basicData.getAsJsonObject("knowledge_base");
        JsonObject bestEntry = null;
        double bestScore = 0;
        String q = normalizedPrompt;

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

    // Check if the question is about the student's personal data
    private boolean isPersonalQuestion(String normalizedPrompt) {
        for (String keyword : PERSONAL_KEYWORDS) {
            if (normalizedPrompt.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    // Answer personal questions directly from cached profile
    private String answerFromProfile(String q) {
        if (!profileReady || profileRepo.getCachedProfile().isEmpty()) {
            return "Δεν μπόρεσα να φορτώσω το προφίλ σου αυτή τη στιγμή.";
        }

        String profile = profileRepo.getCachedProfile();

        // GPA question
        if (q.contains("μεσος ορος") || q.contains("μο") || q.contains("βαθμολογια")) {
            return extractLineFromProfile(profile, "Μέσος Όρος Βαθμολογίας:");
        }

        // ECTS question
        if (q.contains("ects")) {
            return extractLineFromProfile(profile, "Συνολικά ECTS");
        }

        // Semester question
        if (q.contains("εξαμηνο")) {
            return extractLineFromProfile(profile, "Τρέχον Εξάμηνο:");
        }

        // General profile dump for "μαθήματά μου", "πρόοδός μου" etc.
        return profile;
    }

    private String extractLineFromProfile(String profile, String linePrefix) {
        for (String line : profile.split("\n")) {
            if (line.contains(linePrefix)) {
                return line.trim();
            }
        }
        return profile; // fallback: return full profile
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

        if (entry.has("professors")) {
            for (JsonElement p : entry.getAsJsonArray("professors")) {
                String fullName = normalize(p.getAsJsonObject().get("name").getAsString());
                String lastName = fullName.split("\\s+")[0];
                String root = lastName.substring(0, Math.min(lastName.length(), 7));
                if (q.contains(root) && root.length() > 3) {
                    Log.d(TAG, "Professor match: " + lastName);
                    return 10000.0;
                }
            }
        }

        if (score > 0) {
            Log.d(TAG, String.format("Entry: %-30s | Score: %6.1f", entryTopic, score));
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
                    sb.append("Καθηγητής: ").append(name).append(". ")
                            .append("Τίτλος ").append(p.get("role").getAsString()).append("\n")
                            .append("Γραφείο ").append(p.get("office").getAsString()).append("\n")
                            .append("Εmail ").append(p.get("email").getAsString()).append("\n")
                            .append("ώρες Γραφείου ").append(p.get("hours").getAsString());
                    foundSpecific = true;
                }
            }
            if (foundSpecific) return sb.toString();
        }

        return entry.has("full_answer") ? entry.get("full_answer").getAsString() : "Σφάλμα μορφοποίησης.";
    }

    // ─────────────────────────────────────────────
    //  ONLINE (THINKING) MODE — RAG PIPELINE
    // ─────────────────────────────────────────────
    public void fetchThinkingResponse(String userQuestion, AICallback callback) {
        new Thread(() -> {
            try {
                // Step 1: Embed the question
                Log.d(TAG, "Step 1: Embedding question...");
                float[] queryEmbedding = embedText(userQuestion);
                if (queryEmbedding == null) {
                    callback.onError("Σφάλμα embedding ερώτησης.");
                    return;
                }

                // Step 2: Retrieve relevant chunks from Supabase
                Log.d(TAG, "Step 2: Retrieving chunks from Supabase...");
                String ragContext = retrieveContext(queryEmbedding);
                if (ragContext == null) {
                    callback.onError("Σφάλμα ανάκτησης δεδομένων.");
                    return;
                }

                // Step 3: Build prompt with profile + context
                Log.d(TAG, "Step 3: Building prompt...");
                String fullPrompt = buildPrompt(userQuestion, ragContext);

                // Step 4: Add to conversation history
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", fullPrompt);
                conversationHistory.add(userMsg);

                // Step 5: Call Gemini
                Log.d(TAG, "Step 5: Calling Gemini...");
                String answer = callGemini();

                if (answer != null) {
                    JsonObject assistantMsg = new JsonObject();
                    assistantMsg.addProperty("role", "model");
                    assistantMsg.addProperty("content", answer);
                    conversationHistory.add(assistantMsg);
                    callback.onSuccess(answer);
                } else {
                    callback.onError("Σφάλμα απάντησης Gemini.");
                }

            } catch (Exception e) {
                Log.e(TAG, "RAG pipeline error: " + e.getMessage());
                callback.onError("Σφάλμα: " + e.getMessage());
            }
        }).start();
    }

    private float[] embedText(String text) throws IOException {
        String url = GEMINI_BASE + "/" + EMBEDDING_MODEL + ":embedContent";

        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        parts.add(part);
        content.add("parts", parts);

        JsonObject body = new JsonObject();
        body.addProperty("model", EMBEDDING_MODEL);
        body.add("content", content);
        body.addProperty("outputDimensionality", 768);

        Request request = new Request.Builder()
                .url(url)
                .header("x-goog-api-key", geminiApiKey)
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "Embedding error: " + response.code());
                return null;
            }

            JsonObject data = JsonParser.parseString(response.body().string()).getAsJsonObject();
            JsonArray values = data.getAsJsonObject("embedding").getAsJsonArray("values");

            float[] embedding = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                embedding[i] = values.get(i).getAsFloat();
            }
            return embedding;
        }
    }

    private String retrieveContext(float[] queryEmbedding) throws IOException {
        StringBuilder embStr = new StringBuilder("[");
        for (int i = 0; i < queryEmbedding.length; i++) {
            embStr.append(queryEmbedding[i]);
            if (i < queryEmbedding.length - 1) embStr.append(",");
        }
        embStr.append("]");

        JsonObject body = new JsonObject();
        body.add("query_embedding", JsonParser.parseString(embStr.toString()));
        body.addProperty("match_count", TOP_K);

        String url = supabaseUrl + "/rest/v1/rpc/match_documents";

        Request request = new Request.Builder()
                .url(url)
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer " + supabaseKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "Supabase error: " + response.code());
                return null;
            }

            JsonArray results = JsonParser.parseString(response.body().string()).getAsJsonArray();
            StringBuilder context = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                JsonObject doc = results.get(i).getAsJsonObject();
                context.append("[Source ").append(i + 1).append("]\n");
                context.append(doc.get("text").getAsString()).append("\n\n");
            }

            Log.d(TAG, "Retrieved " + results.size() + " chunks");
            return context.toString();
        }
    }

    // Build prompt: profile block + system prompt + RAG context + question
    private String buildPrompt(String question, String ragContext) {
        StringBuilder prompt = new StringBuilder();

        // Prepend student profile if available
        String profile = profileRepo.getCachedProfile();
        if (!profile.isEmpty()) {
            prompt.append(profile).append("\n");
        }

        prompt.append(BASE_SYSTEM_PROMPT);
        prompt.append("\n\nContext:\n").append(ragContext);
        prompt.append("\nΕρώτηση: ").append(question);
        prompt.append("\n\nΑπάντηση:");

        return prompt.toString();
    }

    private String callGemini() throws IOException {
        String url = GEMINI_BASE + "/models/" + CHAT_MODEL + ":generateContent";

        JsonArray contents = new JsonArray();
        for (JsonObject msg : conversationHistory) {
            JsonObject content = new JsonObject();
            content.addProperty("role", msg.get("role").getAsString());

            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", msg.get("content").getAsString());
            parts.add(part);
            content.add("parts", parts);

            contents.add(content);
        }

        JsonObject generationConfig = new JsonObject();
        generationConfig.addProperty("temperature", 0.3);
        generationConfig.addProperty("maxOutputTokens", 1024);

        JsonObject body = new JsonObject();
        body.add("contents", contents);
        body.add("generationConfig", generationConfig);

        Request request = new Request.Builder()
                .url(url)
                .header("x-goog-api-key", geminiApiKey)
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Log.e(TAG, "Gemini error: " + response.code());
                return null;
            }

            JsonObject data = JsonParser.parseString(response.body().string()).getAsJsonObject();
            return data.getAsJsonArray("candidates")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("content")
                    .getAsJsonArray("parts")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString();
        }
    }

    public void clearThinkingHistory() {
        conversationHistory.clear();
    }
}