package com.example.uniwingman.ui.home;

import java.util.UUID;

public class UserSlot {
    public String id;
    public String name;
    public String day;
    public int startHour;
    public int endHour;
    public String type;
    public String roomOrComment;
    public boolean isOther; // true = εξωσχολική

    public UserSlot(String name, String day, int startHour, int endHour,
                    String type, String roomOrComment, boolean isOther) {
        this.id            = UUID.randomUUID().toString();
        this.name          = name;
        this.day           = day;
        this.startHour     = startHour;
        this.endHour       = endHour;
        this.type          = type;
        this.roomOrComment = roomOrComment;
        this.isOther       = isOther;
    }
}
