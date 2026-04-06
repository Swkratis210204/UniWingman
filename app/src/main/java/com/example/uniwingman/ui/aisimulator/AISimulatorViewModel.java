package com.example.uniwingman.ui.aisimulator;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

public class AISimulatorViewModel extends ViewModel {
    private final MutableLiveData<List<ChatMessage>> mMessages = new MutableLiveData<>();
    private final ChatRepository repository = new ChatRepository();

    // Each mode has its own independent conversation list
    private final List<ChatMessage> basicMessages    = new ArrayList<>();
    private final List<ChatMessage> thinkingMessages = new ArrayList<>();

    private boolean isThinkingMode = false;

    public AISimulatorViewModel() {
        basicMessages.add(new ChatMessage(ChatRepository.BASIC_GREETING, false));
        thinkingMessages.add(new ChatMessage(ChatRepository.THINKING_GREETING, false));
        // Start in basic mode
        mMessages.setValue(new ArrayList<>(basicMessages));
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return mMessages;
    }

    public boolean isThinkingMode() {
        return isThinkingMode;
    }

    public void setModelMode(boolean isThinking) {
        isThinkingMode = isThinking;
        // Switch to the other conversation — no data is lost
        List<ChatMessage> current = isThinking ? thinkingMessages : basicMessages;
        mMessages.setValue(new ArrayList<>(current));
    }

    public void sendMessage(String text) {
        List<ChatMessage> currentList = isThinkingMode ? thinkingMessages : basicMessages;

        currentList.add(new ChatMessage(text, true));
        mMessages.setValue(new ArrayList<>(currentList));

        // Capture mode now so the callback always writes to the correct list,
        // even if the user switches models before the response arrives.
        final boolean sentAsThinking = isThinkingMode;

        ChatRepository.AICallback callback = new ChatRepository.AICallback() {
            @Override
            public void onSuccess(String response) {
                addAiResponse(response, sentAsThinking);
            }

            @Override
            public void onError(String error) {
                addAiResponse("Σφάλμα: " + error, sentAsThinking);
            }
        };

        if (isThinkingMode) {
            repository.fetchThinkingResponse(text, callback);
        } else {
            repository.fetchBasicResponse(text, callback);
        }
    }

    private void addAiResponse(String text, boolean wasThinking) {
        List<ChatMessage> targetList = wasThinking ? thinkingMessages : basicMessages;
        targetList.add(new ChatMessage(text, false));
        // Only update LiveData if we're still on the same mode
        if (wasThinking == isThinkingMode) {
            mMessages.postValue(new ArrayList<>(targetList));
        }
    }

    public void clearConversation() {
        if (isThinkingMode) {
            repository.clearThinkingHistory();
            thinkingMessages.clear();
            thinkingMessages.add(new ChatMessage(ChatRepository.THINKING_GREETING, false));
            mMessages.setValue(new ArrayList<>(thinkingMessages));
        } else {
            basicMessages.clear();
            basicMessages.add(new ChatMessage(ChatRepository.BASIC_GREETING, false));
            mMessages.setValue(new ArrayList<>(basicMessages));
        }
    }
}
