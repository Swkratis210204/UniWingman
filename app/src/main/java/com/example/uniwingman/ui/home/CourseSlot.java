package com.example.uniwingman.ui.home;

public class CourseSlot {
    public String name;
    public String time;
    public String type; // D, E, F
    public CourseSlot(String name, String time, String type) {
        this.name = name; this.time = time; this.type = type;
    }
    public String getTypeLabel() {
        switch (type) {
            case "E": return "Εργαστήριο";
            case "F": return "Φροντιστήριο";
            default:  return "Διάλεξη";
        }
    }
    public int getTypeColor() {
        switch (type) {
            case "E": return 0xFF00BCD4;  // cyan
            case "F": return 0xFF9C27B0;  // purple
            default:  return 0xFF3D5AFE;  // blue
        }
    }
}
