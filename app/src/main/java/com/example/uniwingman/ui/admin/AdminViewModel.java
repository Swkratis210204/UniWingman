package com.example.uniwingman.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.uniwingman.data.SupabaseAdmin;
import java.util.List;

public class AdminViewModel extends ViewModel {

    private final MutableLiveData<Integer> totalUsers = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalCourses = new MutableLiveData<>(); // ΝΕΟ: Για τον αριθμό μαθημάτων
    private final MutableLiveData<List<AdminUserItem>> recentUsers = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final SupabaseAdmin supabaseAdmin;

    public AdminViewModel() {
        supabaseAdmin = new SupabaseAdmin();
    }

    // --- GETTERS ΓΙΑ ΤΟ UI ---

    public LiveData<Integer> getTotalUsers() {
        return totalUsers;
    }

    public LiveData<Integer> getTotalCourses() {
        return totalCourses;
    }

    public LiveData<List<AdminUserItem>> getRecentUsers() {
        return recentUsers;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // --- ΜΕΘΟΔΟΙ ΔΙΑΧΕΙΡΙΣΗΣ ΔΕΔΟΜΕΝΩΝ ---

    /**
     * Φορτώνει ΟΛΑ τα στατιστικά (αριθμούς και λίστες χρηστών) από τη βάση δεδομένων
     */
    public void loadStatistics() {
        // 1. Φέρνει τον πραγματικό συνολικό αριθμό χρηστών
        supabaseAdmin.getTotalUserCount(new SupabaseAdmin.StatsCallback() {
            @Override
            public void onSuccess(int count) {
                totalUsers.postValue(count);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue("Σφάλμα καταμέτρησης χρηστών: " + error);
            }
        });

        // 2. ΝΕΟ: Φέρνει τον συνολικό αριθμό των μαθημάτων
        supabaseAdmin.getTotalCoursesCount(new SupabaseAdmin.StatsCallback() {
            @Override
            public void onSuccess(int count) {
                totalCourses.postValue(count);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue("Σφάλμα καταμέτρησης μαθημάτων: " + error);
            }
        });

        // 3. Φέρνει τη λίστα με τους 3 πιο πρόσφατους χρήστες
        supabaseAdmin.getRecentUsers(new SupabaseAdmin.UserListCallback() {
            @Override
            public void onSuccess(List<AdminUserItem> users) {
                recentUsers.postValue(users);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue("Σφάλμα φόρτωσης χρηστών: " + error);
            }
        });
    }

    /**
     * Αναζητά χρήστες στη βάση δεδομένων με βάση το username
     * και ενημερώνει τη λίστα της οθόνης (recentUsers)
     */
    public void searchUser(String query) {
        supabaseAdmin.searchUsers(query, new SupabaseAdmin.UserListCallback() {
            @Override
            public void onSuccess(List<AdminUserItem> users) {
                recentUsers.postValue(users);
            }

            @Override
            public void onError(String error) {
                errorMessage.postValue("Σφάλμα αναζήτησης: " + error);
            }
        });
    }
}