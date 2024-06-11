package com.example.greenway;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class ApiExplorer {

    public static List<ParkingInfo> getParkingInfo() throws IOException {
        StringBuilder urlBuilder = new StringBuilder("http://openapi.seoul.go.kr:8088/6a66414357776c7333365a4d455058/json/GetParkingInfo/1/1000/");
        urlBuilder.append("/" + URLEncoder.encode("6a66414357776c7333365a4d455058","UTF-8") );
        urlBuilder.append("/" + URLEncoder.encode("json","UTF-8") );
        urlBuilder.append("/" + URLEncoder.encode("GetParkingInfo","UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode("1","UTF-8"));
        urlBuilder.append("/" + URLEncoder.encode("1000","UTF-8"));

        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");

        BufferedReader rd;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();

        String response = sb.toString();
        return parseParkingInfo(response);
    }

    private static List<ParkingInfo> parseParkingInfo(String response) {
        List<ParkingInfo> parkingInfoList = new ArrayList<>();
        Set<String> parkingCodeSet = new HashSet<>();
        String[] records = response.split("\\{");

        for (String record : records) {
            if (record.contains("\"PARKING_CODE\"") && record.contains("\"PARKING_NAME\"") && record.contains("\"ADDR\"") && record.contains("\"LAT\"") && record.contains("\"LNG\"") && record.contains("\"CAPACITY\"") && record.contains("\"CUR_PARKING\"")) {
                String parkingCode = extractValue(record, "PARKING_CODE");
                if (!parkingCodeSet.contains(parkingCode)) {
                    String parkingName = extractValue(record, "PARKING_NAME");
                    String addr = extractValue(record, "ADDR");
                    double lat = Double.parseDouble(extractValue(record, "LAT"));
                    double lng = Double.parseDouble(extractValue(record, "LNG"));
                    int capacity = (int) Double.parseDouble(extractValue(record, "CAPACITY"));
                    int curParking = (int) Double.parseDouble(extractValue(record, "CUR_PARKING"));

                    parkingInfoList.add(new ParkingInfo(parkingCode, parkingName, addr, lat, lng, capacity, curParking));
                    parkingCodeSet.add(parkingCode);
                }
            }
        }
        return parkingInfoList;
    }

    // 공공자전거 대여소 정보를 가져오는 메서드
    public static List<BikeInfo> getBikeInfo() throws IOException {
        StringBuilder urlBuilder = new StringBuilder("http://openapi.seoul.go.kr:8088/517163556b776c7337316742726246/json/bikeList/1/20/");
        URL url = new URL(urlBuilder.toString());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-type", "application/json");

        BufferedReader rd;
        if (conn.getResponseCode() >= 200 && conn.getResponseCode() <= 300) {
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        } else {
            rd = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = rd.readLine()) != null) {
            sb.append(line);
        }
        rd.close();
        conn.disconnect();

        String response = sb.toString();
        return parseBikeInfo(response);
    }

    // 공공자전거 대여소 정보를 파싱하는 메서드
    private static List<BikeInfo> parseBikeInfo(String response) {
        List<BikeInfo> bikeInfoList = new ArrayList<>();
        String[] records = response.split("\\{");

        for (String record : records) {
            if (record.contains("\"stationName\"") && record.contains("\"stationLatitude\"") && record.contains("\"stationLongitude\"")) {
                String stationName = extractValue(record, "stationName");
                double lat = Double.parseDouble(extractValue(record, "stationLatitude"));
                double lng = Double.parseDouble(extractValue(record, "stationLongitude"));

                bikeInfoList.add(new BikeInfo(stationName, lat, lng));
            }
        }
        return bikeInfoList;
    }
    // JSON 레코드에서 키에 해당하는 값을 추출하는 메서드
    private static String extractValue(String record, String key) {
        String keyPattern = "\"" + key + "\":";
        int startIndex = record.indexOf(keyPattern) + keyPattern.length();
        int endIndex = record.indexOf(",", startIndex);
        if (endIndex == -1) {
            endIndex = record.indexOf("}", startIndex);
        }
        return record.substring(startIndex, endIndex).replace("\"", "").trim();
    }
}