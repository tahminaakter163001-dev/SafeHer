package com.example.safeher;

public class SafetyTip {

    private final String title;
    private final String description;

    public SafetyTip(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
