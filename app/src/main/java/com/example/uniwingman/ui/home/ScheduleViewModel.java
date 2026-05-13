package com.example.uniwingman.ui.home;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.uniwingman.data.SupabaseSchedule;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.*;

public class ScheduleViewModel extends AndroidViewModel {

    // Σειρά slots: 9→11→1→3→5→7
    private static final int[] SLOT_STARTS = {9, 11, 1, 3, 5, 7};
    private static final int[] SLOT_ENDS   = {11, 1, 3, 5, 7, 9};

    // Χρώματα ανά τύπο
    private static final int COLOR_LECTURE  = 0xFF185FA5;
    private static final int COLOR_LAB      = 0xFF00897B;
    private static final int COLOR_TUTORIAL = 0xFF7B1FA2;
    private static final int COLOR_OTHER    = 0xFF546E7A;

    private final MutableLiveData<List<SlotRow>> mCurrentDaySlots = new MutableLiveData<>();
    private final MutableLiveData<List<CourseItem>> mAllCourses = new MutableLiveData<>();

    // Raw data
    private final List<RawSlot> supabaseSlots = new ArrayList<>();
    private List<UserSlot> userSlots = new ArrayList<>();
    private String currentDay = "Δευτέρα";

    private final SupabaseSchedule supabaseSchedule;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public ScheduleViewModel(@NonNull Application application) {
        super(application);
        supabaseSchedule = new SupabaseSchedule(application.getApplicationContext());
        prefs = application.getSharedPreferences("UniWingmanPrefs", android.content.Context.MODE_PRIVATE);
        loadUserSlotsFromPrefs();
    }

    public void load(String userId) {
        // Φόρτωσε schedule από Supabase
        supabaseSchedule.fetchSchedule(userId, new SupabaseSchedule.ScheduleCallback() {
            @Override
            public void onSuccess(List<RawSlot> slots) {
                supabaseSlots.clear();
                supabaseSlots.addAll(slots);
                rebuildCurrentDay();
            }
            @Override public void onError(String e) {}
        });

        // Φόρτωσε όλα τα μαθήματα για προσθήκη
        supabaseSchedule.fetchAllCourses(new SupabaseSchedule.CoursesCallback() {
            @Override
            public void onSuccess(List<CourseItem> courses) {
                mAllCourses.postValue(courses);
            }
            @Override public void onError(String e) {}
        });
    }

    public void selectDay(String day) {
        currentDay = day;
        rebuildCurrentDay();
    }

    public void addUserSlot(UserSlot slot) {
        userSlots.add(slot);
        saveUserSlotsToPrefs();
        rebuildCurrentDay();
    }

    public void deleteUserSlot(String slotId) {
        userSlots.removeIf(s -> s.id.equals(slotId));
        saveUserSlotsToPrefs();
        rebuildCurrentDay();
    }

    private void rebuildCurrentDay() {
        // Μάζεψε όλα τα slots για την τρέχουσα μέρα
        List<RawSlot> daySupabase = new ArrayList<>();
        for (RawSlot s : supabaseSlots) {
            if (currentDay.equals(s.day)) {
                // Σπάσε 4ωρα σε 2ωρα
                daySupabase.addAll(splitSlot(s));
            }
        }

        List<UserSlot> dayUser = new ArrayList<>();
        for (UserSlot s : userSlots) {
            if (currentDay.equals(s.day)) dayUser.add(s);
        }

        // Φτιάξε rows για κάθε 2ωρο slot
        List<SlotRow> rows = new ArrayList<>();
        for (int i = 0; i < SLOT_STARTS.length; i++) {
            int start = SLOT_STARTS[i];
            int end   = SLOT_ENDS[i];

            List<SlotCard> cards = new ArrayList<>();

            // Supabase slots
            for (RawSlot s : daySupabase) {
                if (s.startHour == start) {
                    cards.add(new SlotCard(
                            "sb_" + s.name + start,
                            s.name, s.type, s.room,
                            colorForType(s.type), false));
                }
            }

            // User slots
            for (UserSlot s : dayUser) {
                if (s.startHour == start) {
                    cards.add(new SlotCard(
                            s.id, s.name, s.type,
                            s.roomOrComment,
                            s.isOther ? COLOR_OTHER : colorForType(s.type),
                            true));
                }
            }

            if (!cards.isEmpty()) {
                rows.add(new SlotRow(start, end, cards));
            }
        }

        mCurrentDaySlots.postValue(rows);
    }

    // Σπάει 4ωρο σε δύο 2ωρα
    private List<RawSlot> splitSlot(RawSlot s) {
        List<RawSlot> result = new ArrayList<>();
        int[] order = {9, 11, 1, 3, 5, 7};

        int startIdx = indexOf(order, s.startHour);
        int endIdx   = indexOf(order, s.endHour);

        if (startIdx < 0 || endIdx < 0) { result.add(s); return result; }

        // Αν είναι ακριβώς 2ωρο (συνεχόμενα slots)
        int diff = (endIdx - startIdx + order.length) % order.length;
        if (diff == 1) {
            result.add(s);
            return result;
        }

        // Σπάσε σε 2ωρα
        int idx = startIdx;
        while (idx != endIdx) {
            int next = (idx + 1) % order.length;
            result.add(new RawSlot(s.name, s.day, order[idx], order[next], s.type, s.room));
            idx = next;
        }
        return result;
    }

    private int indexOf(int[] arr, int val) {
        for (int i = 0; i < arr.length; i++) if (arr[i] == val) return i;
        return -1;
    }

    private int colorForType(String type) {
        if (type == null) return COLOR_LECTURE;
        switch (type) {
            case "Εργαστήριο":   return COLOR_LAB;
            case "Φροντιστήριο": return COLOR_TUTORIAL;
            default:             return COLOR_LECTURE;
        }
    }

    private void saveUserSlotsToPrefs() {
        prefs.edit().putString("user_slots", gson.toJson(userSlots)).apply();
    }

    private void loadUserSlotsFromPrefs() {
        String json = prefs.getString("user_slots", null);
        if (json != null) {
            userSlots = gson.fromJson(json,
                    new TypeToken<List<UserSlot>>(){}.getType());
        }
    }

    public LiveData<List<SlotRow>> getCurrentDaySlots() { return mCurrentDaySlots; }
    public LiveData<List<CourseItem>> getAllCourses()    { return mAllCourses; }
}
