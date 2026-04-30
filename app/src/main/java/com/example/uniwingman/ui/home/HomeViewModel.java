package com.example.uniwingman.ui.home;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.uniwingman.data.SupabaseHome;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final MutableLiveData<String> mGpa = new MutableLiveData<>("—");
    private final MutableLiveData<Integer> mStreak = new MutableLiveData<>(0);
    private final MutableLiveData<List<ScheduleDay>> mScheduleDays = new MutableLiveData<>();
    private final MutableLiveData<List<TaskItem>> mTasks = new MutableLiveData<>();
    private final MutableLiveData<List<NotificationItem>> mNotifications = new MutableLiveData<>();

    private final SupabaseHome supabaseHome;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        supabaseHome = new SupabaseHome(application.getApplicationContext());
        buildMockData(); // tasks + notifications παραμένουν mock
    }

    public void load(String userId) {
        // Real data: GPA + Streak
        supabaseHome.fetchGpaAndStreak(userId, new SupabaseHome.HomeCallback() {
            @Override
            public void onSuccess(String gpa, int streak) {
                mGpa.postValue(gpa);
                mStreak.postValue(streak);
            }
            @Override public void onError(String error) {}
        });

        // Real data: Schedule από Supabase
        supabaseHome.fetchSchedule(userId, new SupabaseHome.ScheduleCallback() {
            @Override
            public void onSuccess(List<ScheduleDay> days) {
                mScheduleDays.postValue(days);
            }
            @Override public void onError(String error) {}
        });
    }

    private void buildMockData() {
        // Mock tasks — βασισμένες στα in_progress μαθήματα
        List<TaskItem> tasks = new ArrayList<>();
        tasks.add(new TaskItem("ΑΑΥ",          "Πρώτο Παραδοτέο",       "2026-04-28", 5));
        tasks.add(new TaskItem("Ανάλυση Δεδ.", "Εργαστηριακή Άσκηση 1", "2026-04-30", 7));
        tasks.add(new TaskItem("ΘΥΒ",          "Project Phase 1",        "2026-05-05", 12));
        tasks.add(new TaskItem("ΤΚΕ",          "Θεωρητική Άσκηση 2",    "2026-05-08", 15));
        mTasks.setValue(tasks);

        // Mock notifications
        List<NotificationItem> notifs = new ArrayList<>();
        notifs.add(new NotificationItem("Ανακοινώθηκε εργασία στην Ανάλυση Δεδομένων", "πριν 2 ώρες"));
        notifs.add(new NotificationItem("Σε λίγο ξεκινάει το φροντιστήριο στην Αλληλεπίδραση", "σε 15 λεπτά"));
        mNotifications.setValue(notifs);
    }

    public LiveData<String> getGpa()                     { return mGpa; }
    public LiveData<Integer> getStreak()                 { return mStreak; }
    public LiveData<List<ScheduleDay>> getScheduleDays() { return mScheduleDays; }
    public LiveData<List<TaskItem>> getTasks()           { return mTasks; }
    public LiveData<List<NotificationItem>> getNotifications() { return mNotifications; }
}