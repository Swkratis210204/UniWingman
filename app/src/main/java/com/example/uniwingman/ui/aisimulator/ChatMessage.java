package com.example.uniwingman.ui.aisimulator;

public class ChatMessage {
    public enum Type { USER, AI, LOADING }

    private String text;
    private boolean isUser;
    private Type type;
    private long responseTimeMs = -1; // -1 = no timing

    // Original constructor (backward compatible)
    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
        this.type = isUser ? Type.USER : Type.AI;
    }

    // Loading message constructor
    public static ChatMessage loading() {
        ChatMessage msg = new ChatMessage("", false);
        msg.type = Type.LOADING;
        return msg;
    }

    // AI message with response time (online mode)
    public static ChatMessage aiWithTime(String text, long responseTimeMs) {
        ChatMessage msg = new ChatMessage(text, false);
        msg.responseTimeMs = responseTimeMs;
        return msg;
    }

    public String getText() { return text; }
    public boolean isUser() { return isUser; }
    public Type getType() { return type; }
    public long getResponseTimeMs() { return responseTimeMs; }
    public boolean hasResponseTime() { return responseTimeMs >= 0; }
}