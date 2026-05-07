package com.example.uniwingman.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.uniwingman.data.SupabaseAdmin;
import java.util.List;

public class AdminViewModel extends ViewModel {

    // Κρατάει τον συνολικό αριθμό
    private final MutableLiveData<Integer> totalUsers = new MutableLiveData<>();
    // ΝΕΟ: Κρατάει τη λίστα με τους πρόσφατους χρήστες
    private final MutableLiveData<List<AdminUserItem>> recentUsers = new MutableLiveData<>();
    // Κρατάει τα μηνύματα λάθους
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final SupabaseAdmin supabaseAdmin;

    public AdminViewModel() {
        supabaseAdmin = new SupabaseAdmin();
    }

    public LiveData<Integer> getTotalUsers() {
        return totalUsers;
    }

    // ΝΕΟ: Επιστρέφει τη λίστα στο Fragment
    public LiveData<List<AdminUserItem>> getRecentUsers() {
        return recentUsers;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Φορτώνει ΟΛΑ τα στατιστικά (αριθμούς και λίστες)
     */
    public void loadStatistics() {
        // 1. Φέρνει το νούμερο (το Mock που έχουμε αφήσει)
        supabaseAdmin.getTotalUserCount(new SupabaseAdmin.StatsCallback() {
            @Override
            public void onSuccess(int count) {
                totalUsers.postValue(count);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue(error);
            }
        });

        // 2. ΝΕΟ: Φέρνει την πραγματική λίστα χρηστών
        supabaseAdmin.getRecentUsers(new SupabaseAdmin.UserListCallback() {
            @Override
            public void onSuccess(List<AdminUserItem> users) {
                recentUsers.postValue(users); // Στέλνει τη λίστα στο UI
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue("Σφάλμα φόρτωσης χρηστών: " + error);
            }
        });
    }
}