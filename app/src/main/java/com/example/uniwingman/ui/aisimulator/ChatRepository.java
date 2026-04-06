package com.example.uniwingman.ui.aisimulator;

public class ChatRepository {
    private boolean isThinkingMode = false;

    public void setThinkingMode(boolean enabled) {
        this.isThinkingMode = enabled;
    }

    // CHECK THIS LINE: Must be public, must be String, must have (String prompt)
    public String getAIResponse(String prompt) {
        if (isThinkingMode) {
            return "[Online] Βαθιά ανάλυση: " + prompt;
        } else {
            return "[Offline] Γρήγορη απάντηση στο: " + prompt;
        }
    }

    public String getInitialGreeting() {
        return isThinkingMode
                ? "Γεια! Είμαι το Thinking Model. Πώς μπορώ να βοηθήσω;"
                : "Γεια! Είμαι το Basic Model. Πώς μπορώ να βοηθήσω;";
    }
}