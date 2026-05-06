package com.example.uniwingman.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.uniwingman.data.SupabaseAdmin;

public class AdminViewModel extends ViewModel {

    // MutableLiveData allows us to safely update UI values from background threads
    private final MutableLiveData<Integer> totalUsers = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Instance of our data layer class
    private final SupabaseAdmin supabaseAdmin;

    public AdminViewModel() {
        supabaseAdmin = new SupabaseAdmin();
    }

    /**
     * Expose immutable LiveData to the Fragment so it can observe changes
     */
    public LiveData<Integer> getTotalUsers() {
        return totalUsers;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Triggers the network request to fetch statistics
     */
    public void loadStatistics() {
        supabaseAdmin.getTotalUserCount(new SupabaseAdmin.StatsCallback() {
            @Override
            public void onSuccess(int count) {
                // Use postValue() because this callback runs on a background thread
                totalUsers.postValue(count);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });
    }
}
