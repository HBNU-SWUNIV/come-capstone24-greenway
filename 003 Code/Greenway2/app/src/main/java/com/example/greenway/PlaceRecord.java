package com.example.greenway;

public class PlaceRecord {
    private String placeName;
    private String addressName;
    private double latitude;
    private double longitude;

    // Constructor
    public PlaceRecord(String placeName, String addressName, double latitude, double longitude) {
        this.placeName = placeName;
        this.addressName = addressName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters
    public String getPlaceName() {
        return placeName;
    }

    public String getAddressName() {
        return addressName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}

