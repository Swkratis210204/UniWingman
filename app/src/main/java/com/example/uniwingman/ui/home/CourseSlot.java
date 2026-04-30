package com.example.uniwingman.ui.home;

public class CourseSlot {
    public String name;
    public String time;
    public String type; // "Διάλεξη", "Εργαστήριο", "Φροντιστήριο"

    public CourseSlot(String name, String time, String type) {
        this.name = name; this.time = time; this.type = type;
    }

    public String getTypeLabel() { return type; }

    public int getTypeColor() {
        if (type == null) return 0xFF185FA5;
        switch (type) {
            case "Εργαστήριο":   return 0xFF00BCD4;  // cyan
            case "Φροντιστήριο": return 0xFF9C27B0;  // purple
            default:             return 0xFF185FA5;  // μπλε
        }
    }
}