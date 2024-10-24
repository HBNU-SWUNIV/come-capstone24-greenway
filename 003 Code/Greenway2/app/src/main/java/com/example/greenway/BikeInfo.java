package com.example.greenway;

import java.util.Objects;

public class BikeInfo {
    private String stationName;
    private double lat;
    private double lng;
    private String address;
    private int parkingBikeTotCnt;  // 자전거 주차 총 건수
    private int rackTotCnt;          // 거치대 개수

    public BikeInfo(String stationName, double lat, double lng, int parkingBikeTotCnt, int rackTotCnt) {
        this.stationName = stationName;
        this.lat = lat;
        this.lng = lng;
        this.parkingBikeTotCnt = parkingBikeTotCnt;
        this.rackTotCnt = rackTotCnt;
    }

    public String getStationName() {
        // 정규식을 사용하여 숫자와 점 뒤에 오는 부분만 추출
        if (stationName != null && stationName.contains(". ")) {
            return stationName.substring(stationName.indexOf(". ") + 2);
        }
        return stationName;
    }


    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public int getParkingBikeTotCnt() {
        return parkingBikeTotCnt;
    }

    public int getRackTotCnt() {
        return rackTotCnt;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BikeInfo bikeInfo = (BikeInfo) o;
        return Double.compare(bikeInfo.lat, lat) == 0 &&
                Double.compare(bikeInfo.lng, lng) == 0 &&
                Objects.equals(stationName, bikeInfo.stationName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stationName, lat, lng);
    }
}