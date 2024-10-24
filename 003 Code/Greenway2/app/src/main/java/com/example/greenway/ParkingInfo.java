package com.example.greenway;

import java.util.Objects;

public class ParkingInfo {
    private String parkingCode;
    private String parkingName;
    private String addr;
    private double lat;
    private double lng;
    private int capacity; // 총주차면
    private int curParking; // 현재 주차 차량수
    private String nowPrkVhclUpdtTm; // 현재 주차 차량수 업데이트 시간
    private int bscPrkCrg; // 기본 주차 요금
    private int addPrkHr; // 추가 단위시간 (분단위)

    public ParkingInfo(String parkingCode, String parkingName, String addr, double lat, double lng, int capacity, int curParking, String nowPrkVhclUpdtTm, int bscPrkCrg, int addPrkHr) {
        this.parkingCode = parkingCode;
        this.parkingName = parkingName;
        this.addr = addr;
        this.lat = lat;
        this.lng = lng;
        this.capacity = capacity;
        this.curParking = curParking;
        this.nowPrkVhclUpdtTm = nowPrkVhclUpdtTm;
        this.bscPrkCrg = bscPrkCrg;
        this.addPrkHr = addPrkHr;
    }

    public String getParkingCode() {
        return parkingCode;
    }

    public String getParkingName() {
        //주차장명에서 (시) 삭제하여 표시
        if (parkingName.endsWith("(시)")) {
            return parkingName.substring(0, parkingName.length() - 3);
        }
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

    public String getNowPrkVhclUpdtTm() { // 여기를 추가합니다.
        return nowPrkVhclUpdtTm;
    }

    public int getBscPrkCrg() {
        return bscPrkCrg;
    }

    public int getAddPrkHr() {
        return addPrkHr;
    }

    public void setCapacity(int capacity) {this.capacity = capacity;}

    public void setCurParking(int curParking) {this.curParking = curParking;}


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParkingInfo that = (ParkingInfo) o;
        return Objects.equals(parkingCode, that.parkingCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parkingCode);
    }
}