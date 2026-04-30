package com.example.uniwingman.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.uniwingman.data.SupabaseProfile;
public class ProfileViewModel extends AndroidViewModel {

    private final MutableLiveData<String>  mGpa           = new MutableLiveData<>("—");
    private final MutableLiveData<Integer> mStreak        = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> mTotalCourses  = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> mDeclaredCount = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> mPassedCount   = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> mFailedCount   = new MutableLiveData<>(0);

    private final SupabaseProfile supabaseProfile;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        supabaseProfile = new SupabaseProfile(application.getApplicationContext());
    }

    public void loadProfileStats(String userId) {
        supabaseProfile.fetchStats(userId, new SupabaseProfile.ProfileCallback() {
            @Override
            public void onSuccess(SupabaseProfile.ProfileStats stats) {
                mGpa.postValue(stats.gpa);
                mStreak.postValue(stats.streak);
                mTotalCourses.postValue(stats.totalCourses);
                mDeclaredCount.postValue(stats.declaredCount);
                mPassedCount.postValue(stats.passedCount);
                mFailedCount.postValue(stats.failedCount);
            }

            @Override
            public void onError(String error) {
                // Κράτα τις default τιμές, μπορείς να κάνεις log αν θες
            }
        });
    }

    public LiveData<String>  getGpa()           { return mGpa; }
    public LiveData<Integer> getStreak()         { return mStreak; }
    public LiveData<Integer> getTotalCourses()   { return mTotalCourses; }
    public LiveData<Integer> getDeclaredCount()  { return mDeclaredCount; }
    public LiveData<Integer> getPassedCount()    { return mPassedCount; }
    public LiveData<Integer> getFailedCount()    { return mFailedCount; }
}