package com.example.uniwingman.ui.aisimulator;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AISimulatorViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public AISimulatorViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is notifications fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}