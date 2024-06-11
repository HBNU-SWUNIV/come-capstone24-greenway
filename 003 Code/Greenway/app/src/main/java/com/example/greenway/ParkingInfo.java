package com.example.greenway;

public class ParkingInfo {
    private String parkingCode;
    private String parkingName;
    private String addr;
    private double lat;
    private double lng;
    private int capacity; // 총주차면
    private int curParking; // 현재 주차 차량수

    public ParkingInfo(String parkingCode, String parkingName, String addr, double lat, double lng, int capacity, int curParking) {
        this.parkingCode = parkingCode;
        this.parkingName = parkingName;
        this.addr = addr;
        this.lat = lat;
        this.lng = lng;
        this.capacity = capacity;
        this.curParking = curParking;
    }

    public String getParkingCode() {
        return parkingCode;
    }

    public String getParkingName() {
        return parkingName;
    }

    public String getAddr() {
        return addr;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getCurParking() {
        return curParking;
    }
}