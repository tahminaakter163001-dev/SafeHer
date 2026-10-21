package com.example.safeher;

public class EmergencyContact {

    private final int id;
    private final int userId;
    private final String name;
    private final String phoneNumber;

    public EmergencyContact(int id, int userId,
                            String name, String phoneNumber) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
