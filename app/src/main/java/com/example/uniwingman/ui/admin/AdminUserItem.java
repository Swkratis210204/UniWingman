package com.example.uniwingman.ui.admin;

public class AdminUserItem {
    private String username;
    private String email;

    public AdminUserItem(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}