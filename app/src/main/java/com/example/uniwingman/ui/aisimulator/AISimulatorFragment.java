package com.example.uniwingman.ui.aisimulator;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.uniwingman.R; // Ensure you import your R file
import com.example.uniwingman.databinding.FragmentAiSimulatorBinding;
import java.util.ArrayList;
import java.util.List;

public class AISimulatorFragment extends Fragment {
    private FragmentAiSimulatorBinding binding;
    private ChatAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAiSimulatorBinding.inflate(inflater, container, false);

        // 1. Setup RecyclerView
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatAdapter(messageList);
        binding.rvMessages.setAdapter(adapter);

        // 2. Clear test data and add greeting
        messageList.clear();
        addNewMessage("Γεια! Είμαι ο AI βοηθός σου. Πώς μπορώ να βοηθήσω;", false);

        // 3. Setup Send Button
        binding.btnSend.setOnClickListener(v -> {
            String text = binding.etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendMessage(text);
            }
        });

        // 4. Setup Model Selection Toggles
        setupToggles();

        return binding.getRoot();
    }

    private void sendMessage(String text) {
        addNewMessage(text, true);
        binding.etMessage.setText("");

        // Simulated AI response (Replace with real AI logic later)
        binding.rvMessages.postDelayed(() ->
                addNewMessage("Έλαβα το μήνυμά σου! (Εδώ θα απαντήσει το μοντέλο)", false), 1000);
    }

    private void setupToggles() {
        binding.btnBasicModel.setOnClickListener(v -> {
            binding.btnBasicModel.setBackgroundResource(R.drawable.bg_model_selected);
            binding.btnBasicModel.setTextColor(0xFFFFFFFF);
            binding.btnThinkingModel.setBackgroundResource(android.R.color.transparent);
            binding.btnThinkingModel.setTextColor(0xFFB0C8E8);
            binding.tvModelDescription.setText("Offline · Γρήγορες απαντήσεις");
        });

        binding.btnThinkingModel.setOnClickListener(v -> {
            binding.btnThinkingModel.setBackgroundResource(R.drawable.bg_model_selected);
            binding.btnThinkingModel.setTextColor(0xFFFFFFFF);
            binding.btnBasicModel.setBackgroundResource(android.R.color.transparent);
            binding.btnBasicModel.setTextColor(0xFFB0C8E8);
            binding.tvModelDescription.setText("Online · Βαθιά σκέψη & Ανάλυση");
        });
    }

    private void addNewMessage(String text, boolean isUser) {
        messageList.add(new ChatMessage(text, isUser));
        adapter.notifyItemInserted(messageList.size() - 1);
        binding.rvMessages.scrollToPosition(messageList.size() - 1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}