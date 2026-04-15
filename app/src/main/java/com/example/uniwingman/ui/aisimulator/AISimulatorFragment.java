package com.example.uniwingman.ui.aisimulator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAiSimulatorBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(AISimulatorViewModel.class);

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

        // Listen για το αν ο χρήστης κάνει scroll up χειροκίνητα
        binding.rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy < 0) { // Scroll up
                    isUserScrollingUp = true;
                } else if (!recyclerView.canScrollVertically(1)) { // Έφτασε τέρμα κάτω
                    isUserScrollingUp = false;
                }
            }
        });

        binding.btnSend.setOnClickListener(v -> handleSendMessage());

        // ΔΙΟΡΘΩΣΗ ΓΙΑ ΤΟ ENTER
        binding.etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                handleSendMessage();
                return true;
            }
            return false;
        });

        binding.btnBasicModel.setOnClickListener(v -> switchModel(false));
        binding.btnThinkingModel.setOnClickListener(v -> switchModel(true));
        binding.btnClearChat.setOnClickListener(v -> {
            isUserScrollingUp = false;
            viewModel.clearConversation();
        });
    }

    private void handleSendMessage() {
        String text = binding.etMessage.getText().toString().trim();
        if (!text.isEmpty()) {
            isUserScrollingUp = false; // Επαναφορά για να δει την απάντηση
            viewModel.sendMessage(text);
            binding.etMessage.setText("");
        }
    }

    private void observeViewModel() {
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            if (messages != null && adapter != null) {
                adapter.setMessages(messages);

                // Scroll στο τέλος ΜΟΝΟ αν ο χρήστης δεν διαβάζει τα παλιά (scroll up)
                if (!messages.isEmpty() && !isUserScrollingUp) {
                    binding.rvMessages.post(() ->
                            binding.rvMessages.scrollToPosition(messages.size() - 1));
                }
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