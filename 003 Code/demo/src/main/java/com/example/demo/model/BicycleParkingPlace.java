package com.example.demo.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@Table(name = "bicycleParkingPlaces")
public class BicycleParkingPlace {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String rentalStationName; // 대여소명

    @Column(nullable = false, length = 255)
    private String address; // 주소

    @Column(nullable = false)
    private Integer dockCount; // 거치대개수

    @Column(nullable = false)
    private Integer totalBicycleCount; // 자전거주차 총 건수

    // 생성자, 게터, 세터는 Lombok @Getter, @Setter 어노테이션으로 자동 생성됩니다.
}