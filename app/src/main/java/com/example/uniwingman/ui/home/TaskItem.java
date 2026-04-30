package com.example.uniwingman.ui.home;

public class TaskItem {
    public String course;
    public String title;
    public String deadline;
    public int daysLeft;
    public TaskItem(String course, String title, String deadline, int daysLeft) {
        this.course = course; this.title = title;
        this.deadline = deadline; this.daysLeft = daysLeft;
    }
}
