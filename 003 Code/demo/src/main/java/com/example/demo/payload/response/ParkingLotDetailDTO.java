package com.example.demo.payload.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParkingLotDetailDTO {
    private String 주차장명;
    private String 주소;
    private String 전화번호;
    private String 총주차면;
    private String 현재주차차량수;
    private String 유무료구분명;
    private String 토요일유무료구분명;
    private String 공휴일유무료구분명;
    private String 월정기권금액;
    private String 기본주차요금;
    private String 기본주차시간분단위;
    private String 추가단위요금;
    private String 추가단위시간분단위;
    private String 일최대요금;
    private String 평일운영시작시각HHMM;
    private String 평일운영종료시각HHMM;
    private String 주말운영시작시각HHMM;
    private String 주말운영종료시각HHMM;
    private String 공휴일운영시작시각HHMM;
    private String 공휴일운영종료시각HHMM;
    private String 주차장위치좌표위도;
    private String 주차장위치좌표경도;
}
