package com.example.uniwingman.ui.home;

public class SlotCard {
    public String slotId;
    public String name;
    public String type;
    public String room;
    public int color;
    public boolean isDeletable;

    public SlotCard(String slotId, String name, String type,
                    String room, int color, boolean isDeletable) {
        this.slotId      = slotId;
        this.name        = name;
        this.type        = type;
        this.room        = room;
        this.color       = color;
        this.isDeletable = isDeletable;
    }
}
