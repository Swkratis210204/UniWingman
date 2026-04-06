package com.example.uniwingman.ui.aisimulator;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.List;

public class AISimulatorViewModel extends ViewModel {
    private final MutableLiveData<List<ChatMessage>> mMessages = new MutableLiveData<>(new ArrayList<>());
    private final ChatRepository repository = new ChatRepository();

    public AISimulatorViewModel() {
        refreshGreeting();
    }

    public LiveData<List<ChatMessage>> getMessages() {
        return mMessages;
    }

    public void setModelMode(boolean isThinking) {
        repository.setThinkingMode(isThinking);
    }

    public void sendMessage(String text) {
        List<ChatMessage> currentMessages = mMessages.getValue();
        if (currentMessages == null) currentMessages = new ArrayList<>();

        List<ChatMessage> newList = new ArrayList<>(currentMessages);
        newList.add(new ChatMessage(text, true));
        mMessages.setValue(newList);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String response = repository.getAIResponse(text);
            List<ChatMessage> updatedList = new ArrayList<>(mMessages.getValue());
            updatedList.add(new ChatMessage(response, false));
            mMessages.setValue(updatedList);
        }, 1000);
    }

    public void refreshGreeting() {
        List<ChatMessage> newList = new ArrayList<>();
        // This ensures the greeting matches the current state of the Repository
        newList.add(new ChatMessage(repository.getInitialGreeting(), false));
        mMessages.setValue(newList);
    }
}