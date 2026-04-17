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
    // Προσθήκη LiveData για την κατάσταση πληκτρολόγησης
    private final MutableLiveData<Boolean> isTyping = new MutableLiveData<>(false);

    private final ChatRepository repository;
    private final List<ChatMessage> basicMessages = new ArrayList<>();
    private final List<ChatMessage> thinkingMessages = new ArrayList<>();
    private boolean isThinkingMode = false;

    public static final String BASIC_GREETING = "Λειτουργία Βασικού Μοντέλου: Γρήγορες πληροφορίες για το ΟΠΑ.";
    public static final String THINKING_GREETING = "Λειτουργία Βαθιάς Σκέψης: Έτοιμος για ανάλυση οδηγού σπουδών.";

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

    // Getter για το typing status που χρησιμοποιεί το Fragment
    public LiveData<Boolean> getIsTyping() {
        return isTyping;
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

        // Ενημέρωση ότι το AI ξεκίνησε να επεξεργάζεται (για το Accessibility Announcement)
        isTyping.setValue(true);

        currentList.add(ChatMessage.loading());
        mMessages.setValue(new ArrayList<>(currentList));

        final boolean sentAsThinking = isThinkingMode;

        ChatRepository.AICallback callback = new ChatRepository.AICallback() {
            final long startTime = System.currentTimeMillis();

            @Override
            public void onSuccess(String response) {
                long elapsed = System.currentTimeMillis() - startTime;
                isTyping.postValue(false); // Σταμάτησε να "πληκτρολογεί"
                addAiResponse(response, sentAsThinking, sentAsThinking ? elapsed : -1);
            }

            @Override
            public void onError(String error) {
                isTyping.postValue(false);
                addAiResponse("Σφάλμα επικοινωνίας: " + error, sentAsThinking, -1);
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

        if (!targetList.isEmpty() && targetList.get(targetList.size() - 1).getType() == ChatMessage.Type.LOADING) {
            targetList.remove(targetList.size() - 1);
        }

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