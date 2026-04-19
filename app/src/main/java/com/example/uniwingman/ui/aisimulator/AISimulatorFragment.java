package com.example.uniwingman.ui.aisimulator;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.uniwingman.R;
import com.example.uniwingman.databinding.FragmentAiSimulatorBinding;

import java.util.ArrayList;

public class AISimulatorFragment extends Fragment {

    private FragmentAiSimulatorBinding binding;
    private AISimulatorViewModel viewModel;
    private ChatAdapter adapter;
    private boolean isUserScrollingUp = false;
    private ActivityResultLauncher<Intent> speechLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAiSimulatorBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(AISimulatorViewModel.class);

        // Πρέπει να γίνει register ΠΡΙΝ το setupUI
        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        ArrayList<String> results = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (results != null && !results.isEmpty()) {
                            binding.etMessage.setText(results.get(0));
                            binding.etMessage.setSelection(
                                    binding.etMessage.getText().length());
                            announceForAccessibility("Αναγνωρίστηκε: " + results.get(0) +
                                    ". Πατήστε αποστολή για να στείλετε ή διαγράψτε για να ακυρώσετε.");
                        }
                    }
                }
        );

        setupUI();
        observeViewModel();

        return binding.getRoot();
    }

    private void setupUI() {
        adapter = new ChatAdapter(new ArrayList<>());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(false);

        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);

        binding.rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy < 0) {
                    isUserScrollingUp = true;
                } else if (!recyclerView.canScrollVertically(1)) {
                    isUserScrollingUp = false;
                }
            }
        });

        binding.btnSend.setOnClickListener(v -> handleSendMessage());

        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                handleSendMessage();
                return true;
            }
            return false;
        });

        binding.btnMic.setOnClickListener(v -> startSpeechInput());

        binding.btnBasicModel.setOnClickListener(v -> switchModel(false));
        binding.btnThinkingModel.setOnClickListener(v -> switchModel(true));

        binding.btnClearChat.setOnClickListener(v -> {
            isUserScrollingUp = false;
            viewModel.clearConversation();
            announceForAccessibility("Η συνομιλία διαγράφηκε επιτυχώς");
        });
    }

    private void startSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "el-GR");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "el-GR");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Μίλησε τώρα...");
        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "Η φωνητική εισαγωγή δεν υποστηρίζεται.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSendMessage() {
        String text = binding.etMessage.getText().toString().trim();
        if (!text.isEmpty()) {
            isUserScrollingUp = false;
            viewModel.sendMessage(text);
            binding.etMessage.setText("");
            announceForAccessibility("Το μήνυμα εστάλη");
        }
    }

    private void observeViewModel() {
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null && adapter != null) {
                adapter.setMessages(messages);
                if (!messages.isEmpty() && !isUserScrollingUp) {
                    binding.rvMessages.post(() ->
                            binding.rvMessages.scrollToPosition(messages.size() - 1));
                }
            }
        });

        viewModel.getIsTyping().observe(getViewLifecycleOwner(), isTyping -> {
            if (isTyping != null) {
                binding.layoutTyping.setVisibility(isTyping ? View.VISIBLE : View.GONE);
                if (isTyping) {
                    announceForAccessibility("Ο UniWingman πληκτρολογεί");
                }
            }
        });
    }

    private void switchModel(boolean isThinking) {
        viewModel.setModelMode(isThinking);
        String modelMsg;
        if (isThinking) {
            binding.btnThinkingModel.setBackgroundResource(R.drawable.bg_model_selected);
            binding.btnBasicModel.setBackgroundResource(android.R.color.transparent);
            binding.btnThinkingModel.setTextColor(0xFFFFFFFF);
            binding.btnBasicModel.setTextColor(0xFFB0C8E8);
            binding.tvModelDescription.setText("Online · Βαθιά σκέψη");
            modelMsg = "Ενεργοποιήθηκε το μοντέλο βαθιάς σκέψης. Απαιτείται σύνδεση στο διαδίκτυο.";
        } else {
            binding.btnBasicModel.setBackgroundResource(R.drawable.bg_model_selected);
            binding.btnThinkingModel.setBackgroundResource(android.R.color.transparent);
            binding.btnBasicModel.setTextColor(0xFFFFFFFF);
            binding.btnThinkingModel.setTextColor(0xFFB0C8E8);
            binding.tvModelDescription.setText("Offline · Γρήγορες απαντήσεις");
            modelMsg = "Ενεργοποιήθηκε το βασικό μοντέλο. Λειτουργία χωρίς σύνδεση.";
        }
        announceForAccessibility(modelMsg);
    }

    private void announceForAccessibility(String message) {
        if (getView() != null) {
            getView().announceForAccessibility(message);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}