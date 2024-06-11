package com.example.greenway;

public class BikeInfo {
    private String stationName;
    private double lat;
    private double lng;

    public BikeInfo(String stationName, double lat, double lng) {
        this.stationName = stationName;
        this.lat = lat;
        this.lng = lng;
    }

    public String getStationName() {
        return stationName;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}
