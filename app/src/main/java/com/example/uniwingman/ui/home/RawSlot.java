package com.example.uniwingman.ui.home;

public class RawSlot {
    public String name;
    public String day;
    public int startHour;
    public int endHour;
    public String type;
    public String room;

    public RawSlot(String name, String day, int startHour,
                   int endHour, String type, String room) {
        this.name      = name;
        this.day       = day;
        this.startHour = startHour;
        this.endHour   = endHour;
        this.type      = type;
        this.room      = room;
    }
}
