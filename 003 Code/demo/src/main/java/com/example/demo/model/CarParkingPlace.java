package com.example.demo.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "CarParkingPlaces") // 테이블 이름 변경
public class CarParkingPlace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String parkingName; // 주차장명

    @Column(nullable = false, length = 255)
    private String address; // 주소

    @Column(nullable = false, length = 50)
    private String operationType; // 운영구분명

    @Column(length = 20)
    private String phoneNumber; // 전화번호

    @Column(nullable = false)
    private String operationStartTime; // 운영시작시간

    @Column(nullable = false)
    private String operationEndTime; // 운영종료시간

    @Column(nullable = false)
    private Boolean isRealtimeAvailable; // 실시간 주차현황 제공 여부

    @Column(nullable = false)
    private Integer totalParkingSlots; // 총주차면수

    @Column(nullable = false)
    private Integer currentParkedCount; // 현재 주차 차량수

    @Column(nullable = false)
    private Double parkingFee; // 주차요금

    @Column(nullable = false)
    private Integer baseParkingTime; // 기본주차시간

    @Column(nullable = false)
    private Double additionalUnitFee; // 추가 단위요금

    @Column(nullable = false)
    private Integer additionalUnitTime; // 추가 단위시간

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Favorite> favorites = new HashSet<>();
}