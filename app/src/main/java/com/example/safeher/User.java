
package com.example.safeher;

public class User {

    private final int id;
    private final String fullName;
    private final String email;
    private final String phoneNumber;
    private final String address;
    private final String dateOfBirth;
    private final String bloodGroup;

    // Keep old constructor for existing code compatibility
    public User(int id, String fullName, String email) {
        this(id, fullName, email, "", "", "", "");
    }

    // New constructor with complete profile information
    public User(int id, String fullName, String email,
                String phoneNumber, String address,
                String dateOfBirth, String bloodGroup) {

        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.bloodGroup = bloodGroup;
    }

    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }
}
