package com.example.uniwingman.ui.aisimulator;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.List;

public class AISimulatorViewModel extends AndroidViewModel {
    private final MutableLiveData<List<ChatMessage>> mMessages = new MutableLiveData<>();
    private final ChatRepository repository;

    private final List<ChatMessage> basicMessages = new ArrayList<>();
    private final List<ChatMessage> thinkingMessages = new ArrayList<>();

    private boolean isThinkingMode = false;

    public static final String BASIC_GREETING = "Basic Mode: Γρήγορες πληροφορίες για το ΟΠΑ.";
    public static final String THINKING_GREETING = "Critical Thinking: Έτοιμος για ανάλυση.";

    public AISimulatorViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ChatRepository(application.getApplicationContext());

        basicMessages.add(new ChatMessage(BASIC_GREETING, false));
        thinkingMessages.add(new ChatMessage(THINKING_GREETING, false));

        mMessages.setValue(new ArrayList<>(basicMessages));
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return mMessages;
    }

    public void setModelMode(boolean isThinking) {
        isThinkingMode = isThinking;
        List<ChatMessage> current = isThinking ? thinkingMessages : basicMessages;
        mMessages.setValue(new ArrayList<>(current));
    }

    public void sendMessage(String text) {
        if (text == null || text.trim().isEmpty()) return;

        List<ChatMessage> currentList = isThinkingMode ? thinkingMessages : basicMessages;
        currentList.add(new ChatMessage(text, true));
        currentList.add(ChatMessage.loading());
        mMessages.setValue(new ArrayList<>(currentList));

        final boolean sentAsThinking = isThinkingMode;

        ChatRepository.AICallback callback = new ChatRepository.AICallback() {
            final long startTime = System.currentTimeMillis();

            @Override
            public void onSuccess(String response) {
                long elapsed = System.currentTimeMillis() - startTime;
                addAiResponse(response, sentAsThinking, sentAsThinking ? elapsed : -1);
            }

            @Override
            public void onError(String error) {
                addAiResponse("Σφάλμα: " + error, sentAsThinking, -1);
            }
        };

        if (isThinkingMode) {
            repository.fetchThinkingResponse(text, callback);
        } else {
            repository.fetchBasicResponse(text, callback);
        }
    }

    private void addAiResponse(String text, boolean wasThinking, long responseTimeMs) {
        List<ChatMessage> targetList = wasThinking ? thinkingMessages : basicMessages;

        // Αφαίρεσε το loading
        if (!targetList.isEmpty() && targetList.get(targetList.size() - 1).getType() == ChatMessage.Type.LOADING) {
            targetList.remove(targetList.size() - 1);
        }

        // Πρόσθεσε την απάντηση
        ChatMessage response = wasThinking
                ? ChatMessage.aiWithTime(text, responseTimeMs)
                : new ChatMessage(text, false);
        targetList.add(response);

        if (wasThinking == isThinkingMode) {
            mMessages.postValue(new ArrayList<>(targetList));
        }
    }

    public void clearConversation() {
        if (isThinkingMode) {
            repository.clearThinkingHistory();
            thinkingMessages.clear();
            thinkingMessages.add(new ChatMessage(THINKING_GREETING, false));
            mMessages.setValue(new ArrayList<>(thinkingMessages));
        } else {
            basicMessages.clear();
            basicMessages.add(new ChatMessage(BASIC_GREETING, false));
            mMessages.setValue(new ArrayList<>(basicMessages));
        }
    }
}