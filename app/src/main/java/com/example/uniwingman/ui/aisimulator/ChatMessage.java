package com.example.uniwingman.ui.aisimulator;

public class ChatMessage {
    public enum Type { USER, AI, LOADING }

    private String text;
    private boolean isUser;
    private Type type;
    private long responseTimeMs = -1;

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
        this.type = isUser ? Type.USER : Type.AI;
    }

    public static ChatMessage loading() {
        ChatMessage msg = new ChatMessage("", false);
        msg.type = Type.LOADING;
        return msg;
    }

    public static ChatMessage aiWithTime(String text, long responseTimeMs) {
        ChatMessage msg = new ChatMessage(text, false);
        msg.responseTimeMs = responseTimeMs;
        return msg;
    }

    public String getText() { return text; }

    /**
     * Επιστρέφει το κείμενο βελτιστοποιημένο για Screen Readers.
     * Αντικαθιστά σύμβολα όπως bullets με λέξεις που βγάζουν νόημα ακουστικά.
     */
    public String getAccessibilityFriendlyText() {
        if (text == null) return "";
        return text.replace("•", "Σημείο: ")
                .replace("\n", " . ") // Μικρή παύση στην αλλαγή γραμμής
                .replace("*", "");     // Αφαίρεση Markdown bold/italic
    }

    public boolean isUser() { return isUser; }
    public Type getType() { return type; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public boolean hasResponseTime() { return responseTimeMs >= 0; }
}