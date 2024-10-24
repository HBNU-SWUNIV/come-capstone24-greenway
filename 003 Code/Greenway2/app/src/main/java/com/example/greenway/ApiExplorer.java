package com.example.greenway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import com.naver.maps.geometry.LatLng;
import android.util.Log;


public class ApiExplorer {
    // 지오코딩 API 호출을 위한 네이버 지도 API 키
    private static final String NAVER_CLIENT_ID = "3k1jwfilxs";
    private static final String NAVER_CLIENT_SECRET = "KY2iKbdnTVKQrczF8gi7oyG3RAPgFI3X4JwRc0vB";

    // 주소를 위도와 경도로 변환하는 메서드
    public static LatLng getLatLngFromAddress(String address) throws IOException {
        String encodedAddress = URLEncoder.encode(address, "UTF-8");
        String urlString = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=" + encodedAddress;
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-NCP-APIGW-API-KEY-ID", NAVER_CLIENT_ID);
        conn.setRequestProperty("X-NCP-APIGW-API-KEY", NAVER_CLIENT_SECRET);

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
        // 응답 확인
        System.out.println("Geocode Response: " + response);
        return parseGeocodeResponse(response);
    }

    // 지오코딩 응답을 파싱하여 위도와 경도를 추출하는 메서드
    private static LatLng parseGeocodeResponse(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray addresses = jsonObject.getJSONArray("addresses");
            if (addresses.length() > 0) {
                JSONObject address = addresses.getJSONObject(0);
                double lat = address.getDouble("y");
                double lng = address.getDouble("x");
                // 위도와 경도 확인
                System.out.println("Parsed Lat: " + lat + ", Lng: " + lng);
                return new LatLng(lat, lng);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // 주소를 변환하지 못한 경우
    }

    // 주차장 정보를 가져오는 메서드
    public static List<ParkingInfo> getParkingInfo() throws IOException {
        List<ParkingInfo> parkingInfoList = new ArrayList<>();
        Set<String> parkingCodeSet = new HashSet<>(); // 중복을 확인하기 위한 Set
        int start = 1;
        int end = 1000;
        boolean hasMoreData = true;

        while (hasMoreData) {
            StringBuilder urlBuilder = new StringBuilder("http://openapi.seoul.go.kr:8088/56674d7a556d65653835626e57594d/json/GetParkingInfo/")
                    .append(start).append("/").append(end).append("/");
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
            System.out.println("API Response: " + response);  // API 응답 데이터 출력

            List<ParkingInfo> partialList = parseParkingInfo(response);

            // 중복 제거
            for (ParkingInfo info : partialList) {
                if (!parkingCodeSet.contains(info.getParkingCode())) {
                    parkingInfoList.add(info);
                    parkingCodeSet.add(info.getParkingCode());
                }
            }

            // 1000건보다 적게 반환되면 더 이상 데이터가 없다는 의미이므로 반복을 멈춥니다.
            if (partialList.size() < 1000) {
                hasMoreData = false;
            } else {
                start += 1000;
                end += 1000;
            }
        }

        // 총 주차장 정보의 개수 출력
        System.out.println("Total Parking Information Count: " + parkingInfoList.size());

        return parkingInfoList;
    }

    // 공공자전거 대여소 정보를 가져오는 메서드
    public static List<BikeInfo> getBikeInfo() throws IOException {
        StringBuilder urlBuilder = new StringBuilder("http://openapi.seoul.go.kr:8088/6441696e4d6d65653734464e694c70/json/bikeList/1/20/");
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

    // 킥보드 주차구역 정보를 가져오는 메서드
    public static List<KickboardInfo> getKickboardParkingInfo() throws IOException {
        StringBuilder urlBuilder = new StringBuilder("http://openapi.seoul.go.kr:8088/5165634b796d656535395441516b76/json/parkingKickboard/1/20/");
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
        System.out.println("Parking API Response: " + response);

        return parseKickboardParkingInfo(response);
    }

    // 주차장 정보를 파싱하는 메서드
    private static List<ParkingInfo> parseParkingInfo(String response) {
        // 주차장 코드를 키로 하고, 해당 주차장의 총 면수를 저장할 HashMap 생성
        HashMap<String, ParkingInfo> parkingMap = new HashMap<>();
        String[] records = response.split("\\{");

        for (String record : records) {
            if (record.contains("\"PKLT_CD\"") && record.contains("\"PKLT_NM\"") && record.contains("\"ADDR\"")
                    && record.contains("\"NOW_PRK_VHCL_CNT\"") && record.contains("\"NOW_PRK_VHCL_UPDT_TM\"")
                    && record.contains("\"TPKCT\"") && record.contains("\"LAT\"") && record.contains("\"LOT\"")
                    && record.contains("\"BSC_PRK_CRG\"") && record.contains("\"ADD_PRK_HR\"")) {

                String parkingCode = extractValue(record, "PKLT_CD");
                String parkingName = extractValue(record, "PKLT_NM");
                String addr = extractValue(record, "ADDR");
                double lat = Double.parseDouble(extractValue(record, "LAT"));
                double lng = Double.parseDouble(extractValue(record, "LOT"));

                // TPKCT와 NOW_PRK_VHCL_CNT 키 사용
                int capacity = (int) Double.parseDouble(extractValue(record, "TPKCT"));
                int curParking = (int) Double.parseDouble(extractValue(record, "NOW_PRK_VHCL_CNT"));

                String nowPrkVhclUpdtTm = extractValue(record, "NOW_PRK_VHCL_UPDT_TM");
                int bscPrkCrg = (int) Double.parseDouble(extractValue(record, "BSC_PRK_CRG"));
                int addPrkHr = (int) Double.parseDouble(extractValue(record, "ADD_PRK_HR"));

                // 주차장 코드가 이미 존재하는 경우, 총 주차면수와 현재 차량수를 누적 계산
                if (parkingMap.containsKey(parkingCode)) {
                    ParkingInfo existingInfo = parkingMap.get(parkingCode);
                    existingInfo.setCapacity(existingInfo.getCapacity() + capacity);
                    existingInfo.setCurParking(existingInfo.getCurParking() + curParking);
                } else {
                    // 새로운 주차장 코드를 만나면 새로운 객체를 생성하고 맵에 추가
                    parkingMap.put(parkingCode, new ParkingInfo(parkingCode, parkingName, addr, lat, lng, capacity, curParking, nowPrkVhclUpdtTm, bscPrkCrg, addPrkHr));
                }
            }
        }

        // HashMap의 모든 값을 리스트로 변환하여 반환
        return new ArrayList<>(parkingMap.values());
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
                int parkingBikeTotCnt = 0;
                int rackTotCnt = 0;

                // parkingBikeTotCnt 추출
                if (record.contains("\"parkingBikeTotCnt\"")) {
                    parkingBikeTotCnt = Integer.parseInt(extractValue(record, "parkingBikeTotCnt"));
                }

                // rackTotCnt 추출
                if (record.contains("\"rackTotCnt\"")) {
                    rackTotCnt = Integer.parseInt(extractValue(record, "rackTotCnt"));
                }

                bikeInfoList.add(new BikeInfo(stationName, lat, lng, parkingBikeTotCnt, rackTotCnt));
            }
        }
        return bikeInfoList;
    }


    // 킥보드 주차구역 정보를 파싱하는 메서드
    private static List<KickboardInfo> parseKickboardParkingInfo(String response) {
        List<KickboardInfo> kickboardInfoList = new ArrayList<>();
        String[] records = response.split("\\{");

        for (String record : records) {
            if (record.contains("\"PSTN\"") && record.contains("\"STAND_YN\"") && record.contains("\"STAND_SIZE\"")) {
                String location = extractValue(record, "PSTN");
                boolean hasStand = "Y".equals(extractValue(record, "STAND_YN"));
                String standSize = extractValue(record, "STAND_SIZE");

                kickboardInfoList.add(new KickboardInfo(location, hasStand, standSize));
            }
        }
        return kickboardInfoList;
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
