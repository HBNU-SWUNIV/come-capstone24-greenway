package com.example.greenway;

import java.util.List;

public class KakaoSearchResponse {
    public List<Place> documents;

    public class Place {
        public String place_name;
        public String address_name;
        public double latitude;  // 위도
        public double longitude; // 경도
    }
}
