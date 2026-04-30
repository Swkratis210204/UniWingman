package com.example.uniwingman.ui.home;

import android.app.Application;
import android.content.SharedPreferences;

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

    // ── Mock schedule data ──
    // Μαθήματα: ΑΑΥ, Ανάλυση Δεδ., ΘΥΒ, ΤΚΕ
    // Ανάλυση έχει εργαστήριο, ΑΑΥ & ΘΥΒ έχουν φροντιστήριο
    private static final String[][] SCHEDULE = {
            // {day, course, startHour, endHour, type}  type: D=Διάλεξη, E=Εργαστήριο, F=Φροντιστήριο
            {"Δευ", "ΑΑΥ",       "9",  "11", "D"},
            {"Δευ", "Βάσεις Δεδ.","13", "15", "D"},
            {"Τρι", "Ανάλυση Δεδ.","9", "11", "D"},
            {"Τρι", "Ανάλυση Δεδ.","13","15", "E"},
            {"Τετ", "ΘΥΒ",       "11", "13", "D"},
            {"Τετ", "ΑΑΥ",       "15", "17", "F"},
            {"Πεμ", "ΑΑΥ",       "9",  "11", "D"},
            {"Πεμ", "Βάσεις Δεδ.","13","15", "E"},
            {"Παρ", "ΘΥΒ",       "9",  "11", "D"},
            {"Παρ", "ΘΥΒ",       "13", "15", "F"},
            {"Παρ", "ΤΚΕ",       "11", "13", "D"},
    };

    public HomeViewModel(@NonNull Application application) {
        super(application);
        supabaseHome = new SupabaseHome(application.getApplicationContext());
        buildMockData();
    }

    public void load(String userId) {
        supabaseHome.fetchGpaAndStreak(userId, new SupabaseHome.HomeCallback() {
            @Override
            public void onSuccess(String gpa, int streak) {
                mGpa.postValue(gpa);
                mStreak.postValue(streak);
            }
            @Override
            public void onError(String error) { /* keep defaults */ }
        });
    }

    private void buildMockData() {
        // ── Schedule ──
        String[] days = {"Δευ", "Τρι", "Τετ", "Πεμ", "Παρ"};
        List<ScheduleDay> scheduleDays = new ArrayList<>();
        for (String day : days) {
            List<CourseSlot> slots = new ArrayList<>();
            for (String[] entry : SCHEDULE) {
                if (entry[0].equals(day)) {
                    int start = Integer.parseInt(entry[2]);
                    int end   = Integer.parseInt(entry[3]);
                    String timeStr = start + ":00-" + end + ":00";
                    slots.add(new CourseSlot(entry[1], timeStr, entry[4]));
                }
            }
            scheduleDays.add(new ScheduleDay(day, slots));
        }
        mScheduleDays.setValue(scheduleDays);

        // ── Tasks (mock, βασισμένες στα in_progress μαθήματα) ──
        List<TaskItem> tasks = new ArrayList<>();
        tasks.add(new TaskItem("ΑΑΥ",          "Πρώτο Παραδοτέο",        "2026-04-28", 5));
        tasks.add(new TaskItem("Ανάλυση Δεδ.", "Εργαστηριακή Άσκηση 1",  "2026-04-30", 7));
        tasks.add(new TaskItem("ΘΥΒ",          "Project Phase 1",         "2026-05-05", 12));
        tasks.add(new TaskItem("ΤΚΕ",          "Θεωρητική Άσκηση 2",     "2026-05-08", 15));
        mTasks.setValue(tasks);

        // ── Notifications (mock) ──
        List<NotificationItem> notifs = new ArrayList<>();
        notifs.add(new NotificationItem("Ανακοινώθηκε εργασία στην Ανάλυση Δεδομένων", "πριν 2 ώρες"));
        notifs.add(new NotificationItem("Σε λίγο ξεκινάει το φροντιστήριο στην Αλληλεπίδραση", "σε 15 λεπτά"));
        mNotifications.setValue(notifs);
    }

    public LiveData<String> getGpa()                    { return mGpa; }
    public LiveData<Integer> getStreak()                { return mStreak; }
    public LiveData<List<ScheduleDay>> getScheduleDays(){ return mScheduleDays; }
    public LiveData<List<TaskItem>> getTasks()          { return mTasks; }
    public LiveData<List<NotificationItem>> getNotifications() { return mNotifications; }
}