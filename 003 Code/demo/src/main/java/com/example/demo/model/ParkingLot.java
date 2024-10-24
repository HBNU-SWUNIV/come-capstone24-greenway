package com.example.demo.model;

import javax.persistence.*;

@Entity
@Table(name = "parking_lots")
public class ParkingLot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "`주차장코드`")
    private String code;

    @Column(name = "`주차장명`")
    private String name;

    @Column(name = "`주소`")
    private String address;

    @Column(name = "`주차장 종류`")
    private String type;

    @Column(name = "`주차장 종류명`")
    private String typeName;

    @Column(name = "`운영구분`")
    private String operationType;

    @Column(name = "`운영구분명`")
    private String operationTypeName;

    @Column(name = "`전화번호`")
    private String phoneNumber;

    @Column(name = "`주차현황 정보 제공여부`")
    private String parkingStatusInfo;

    @Column(name = "`총 주차면`")
    private String totalParkingSpots;

    @Column(name = "`현재 주차 차량수`")
    private String currentParkingVehicles;

    @Column(name = "`유무료구분`")
    private String feeType;

    @Column(name = "`유무료구분명`")
    private String feeTypeName;

    @Column(name = "`야간무료개방여부`")
    private String nightFreeOpen;

    @Column(name = "`평일 운영 시작시각(HHMM)`")
    private String weekdayStartTime;

    @Column(name = "`평일 운영 종료시각(HHMM)`")
    private String weekdayEndTime;

    @Column(name = "`주말 운영 시작시각(HHMM)`")
    private String weekendStartTime;

    @Column(name = "`주말 운영 종료시각(HHMM)`")
    private String weekendEndTime;

    @Column(name = "`공휴일 운영 시작시각(HHMM)`")
    private String holidayStartTime;

    @Column(name = "`공휴일 운영 종료시각(HHMM)`")
    private String holidayEndTime;

    @Column(name = "`토요일 유,무료 구분`")
    private String saturdayFeeType;

    @Column(name = "`토요일 유,무료 구분명`")
    private String saturdayFeeTypeName;

    @Column(name = "`공휴일 유,무료 구분`")
    private String holidayFeeType;

    @Column(name = "`공휴일 유,무료 구분명`")
    private String holidayFeeTypeName;

    @Column(name = "`월 정기권 금액`")
    private String monthlySubscriptionFee;

    @Column(name = "`기본 주차 요금`")
    private String baseParkingFee;

    @Column(name = "`기본 주차 시간(분 단위)`")
    private String baseParkingTime;

    @Column(name = "`추가 단위 요금`")
    private String additionalUnitFee;

    @Column(name = "`추가 단위 시간(분 단위)`")
    private String additionalUnitTime;

    @Column(name = "`일 최대 요금`")
    private String dailyMaxFee;

    @Column(name = "`주차장 위치 좌표 위도`")
    private String latitude;

    @Column(name = "`주차장 위치 좌표 경도`")
    private String longitude;

    // Getters
    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getType() {
        return type;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getOperationTypeName() {
        return operationTypeName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getParkingStatusInfo() {
        return parkingStatusInfo;
    }

    public String getTotalParkingSpots() {
        return totalParkingSpots;
    }

    public String getCurrentParkingVehicles() {
        return currentParkingVehicles;
    }

    public String getFeeType() {
        return feeType;
    }

    public String getFeeTypeName() {
        return feeTypeName;
    }

    public String getNightFreeOpen() {
        return nightFreeOpen;
    }

    public String getWeekdayStartTime() {
        return weekdayStartTime;
    }

    public String getWeekdayEndTime() {
        return weekdayEndTime;
    }

    public String getWeekendStartTime() {
        return weekendStartTime;
    }

    public String getWeekendEndTime() {
        return weekendEndTime;
    }

    public String getHolidayStartTime() {
        return holidayStartTime;
    }

    public String getHolidayEndTime() {
        return holidayEndTime;
    }

    public String getSaturdayFeeType() {
        return saturdayFeeType;
    }

    public String getSaturdayFeeTypeName() {
        return saturdayFeeTypeName;
    }

    public String getHolidayFeeType() {
        return holidayFeeType;
    }

    public String getHolidayFeeTypeName() {
        return holidayFeeTypeName;
    }

    public String getMonthlySubscriptionFee() {
        return monthlySubscriptionFee;
    }

    public String getBaseParkingFee() {
        return baseParkingFee;
    }

    public String getBaseParkingTime() {
        return baseParkingTime;
    }

    public String getAdditionalUnitFee() {
        return additionalUnitFee;
    }

    public String getAdditionalUnitTime() {
        return additionalUnitTime;
    }

    public String getDailyMaxFee() {
        return dailyMaxFee;
    }

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }
}
