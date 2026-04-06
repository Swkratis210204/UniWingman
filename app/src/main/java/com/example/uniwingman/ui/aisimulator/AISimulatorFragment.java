package com.example.uniwingman.ui.aisimulator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.uniwingman.R;
import com.example.uniwingman.databinding.FragmentAiSimulatorBinding;
import java.util.ArrayList;

public class AISimulatorFragment extends Fragment {
    private FragmentAiSimulatorBinding binding;
    private AISimulatorViewModel viewModel;
    private ChatAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAiSimulatorBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(AISimulatorViewModel.class);

        setupUI();
        observeViewModel();

        return binding.getRoot();
    }

    private void setupUI() {
        // 1. Initialize RecyclerView and Adapter
        adapter = new ChatAdapter(new ArrayList<>());
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvMessages.setAdapter(adapter);

        // 2. Button Click Listener
        binding.btnSend.setOnClickListener(v -> handleSendMessage());

        // 3. Keyboard "Enter/Send" Listener
        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            // Triggers on the keyboard's "Send" action
            handleSendMessage();
            return true;
        });

        // 4. Model Selection Listeners
        binding.btnBasicModel.setOnClickListener(v -> switchModel(false));
        binding.btnThinkingModel.setOnClickListener(v -> switchModel(true));

        // 5. Clear conversation button
        binding.btnClearChat.setOnClickListener(v -> viewModel.clearConversation());
    }

    private void handleSendMessage() {
        String text = binding.etMessage.getText().toString().trim();
        if (!text.isEmpty()) {
            viewModel.sendMessage(text);
            binding.etMessage.setText("");
        }
    }

    private void observeViewModel() {
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null) {
                // Update the adapter with the fresh list
                adapter = new ChatAdapter(messages);
                binding.rvMessages.setAdapter(adapter);

                // Smooth scroll to bottom
                binding.rvMessages.post(() ->
                        binding.rvMessages.scrollToPosition(messages.size() - 1));
            }
        });
    }

    private void switchModel(boolean isThinking) {
        viewModel.setModelMode(isThinking);

        if (isThinking) {
            binding.btnThinkingModel.setBackgroundResource(R.drawable.bg_model_selected);
            binding.btnBasicModel.setBackgroundResource(android.R.color.transparent);
            binding.btnThinkingModel.setTextColor(0xFFFFFFFF);
            binding.btnBasicModel.setTextColor(0xFFB0C8E8);
            binding.tvModelDescription.setText("Online · Βαθιά σκέψη");
        } else {
            binding.btnBasicModel.setBackgroundResource(R.drawable.bg_model_selected);
            binding.btnThinkingModel.setBackgroundResource(android.R.color.transparent);
            binding.btnBasicModel.setTextColor(0xFFFFFFFF);
            binding.btnThinkingModel.setTextColor(0xFFB0C8E8);
            binding.tvModelDescription.setText("Offline · Γρήγορες απαντήσεις");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}