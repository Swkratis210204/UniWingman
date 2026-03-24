package com.example.uniwingman.ui.aisimulator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.uniwingman.databinding.FragmentAiSimulatorBinding;

public class AISimulatorFragment extends Fragment {

    private FragmentAiSimulatorBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AISimulatorViewModel aiSimulatorViewModel =
                new ViewModelProvider(this).get(AISimulatorViewModel.class);

        binding = FragmentAiSimulatorBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textAiSimulator;
        aiSimulatorViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}