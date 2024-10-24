package com.example.demo.payload.response;

public class FavoriteDTO {

    private Long id;
    private String parkingLotCode;  // 새로운 필드 추가
    private String parkingLotName;
    private String parkingLotAddress;

    // Getters and Setters for all fields

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getParkingLotCode() {  // getter 추가
        return parkingLotCode;
    }

    public void setParkingLotCode(String parkingLotCode) {  // setter 추가
        this.parkingLotCode = parkingLotCode;
    }

    public String getParkingLotName() {
        return parkingLotName;
    }

    public void setParkingLotName(String parkingLotName) {
        this.parkingLotName = parkingLotName;
    }

    public String getParkingLotAddress() {
        return parkingLotAddress;
    }

    public void setParkingLotAddress(String parkingLotAddress) {
        this.parkingLotAddress = parkingLotAddress;
    }
}