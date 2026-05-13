package com.example.uniwingman.ui.home;

import java.util.List;

public class SlotRow {
    public int startHour;
    public int endHour;
    public List<SlotCard> cards;

    public SlotRow(int startHour, int endHour, List<SlotCard> cards) {
        this.startHour = startHour;
        this.endHour   = endHour;
        this.cards     = cards;
    }
}
