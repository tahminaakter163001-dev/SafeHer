
package com.example.safeher;

public class EmergencyPlace {

    private final String name;
    private final String type;
    private final double latitude;
    private final double longitude;
    private final String address;

    public EmergencyPlace(
            String name,
            String type,
            double latitude,
            double longitude,
            String address) {

        this.name = name;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getAddress() {
        return address;
    }
}
