package com.example.uniwingman.ui.home;
import java.util.List;

public class ScheduleDay {
    public String day;
    public List<CourseSlot> slots;
    public ScheduleDay(String day, List<CourseSlot> slots) {
        this.day = day; this.slots = slots;
    }
}
