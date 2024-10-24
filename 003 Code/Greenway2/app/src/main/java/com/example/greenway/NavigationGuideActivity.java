package com.example.greenway;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.widget.ImageView;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.lang.String;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NavigationGuideActivity extends AppCompatActivity implements OnMapReadyCallback {

    private NaverMap naverMap;
    private Marker currentLocationMarker;
    private TextView textView_distance, textView_speed_limit, textView_next_turn, textView_arrival_time;
    private LatLng startLatLng;
    private LatLng endLatLng;
    private List<LatLng> pathCoords;
    private PathOverlay pathOverlay;

    // 위치 서비스
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private OkHttpClient client = new OkHttpClient();  // API 요청을 위한 OkHttpClient

    // 경로 정보 변수
    private List<RouteSection> routeSections = new ArrayList<>();  // 경로 구간 정보

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation_guide);

        // UI 요소 참조
        textView_distance = findViewById(R.id.textView_distance);
        textView_speed_limit = findViewById(R.id.textView_speed_limit);
        textView_next_turn = findViewById(R.id.textView_next_turn);
        textView_arrival_time = findViewById(R.id.textView_arrival_time);

        // 위치 클라이언트 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 전달받은 출발지, 도착지, 경로 정보 가져오기
        double startLat = getIntent().getDoubleExtra("start_lat", 0);
        double startLng = getIntent().getDoubleExtra("start_lng", 0);
        double endLat = getIntent().getDoubleExtra("end_lat", 0);
        double endLng = getIntent().getDoubleExtra("end_lng", 0);
        // 전달받은 경로 좌표 리스트 가져오기
        ArrayList<double[]> pathList = (ArrayList<double[]>) getIntent().getSerializableExtra("pathCoords");


        // Intent에서 전달받은 도착 예정 시간 가져오기
        Intent intent = getIntent();
        String arrivalTimeText = intent.getStringExtra("arrival_time_text");

        if (arrivalTimeText != null) {
            textView_arrival_time.setText(arrivalTimeText);  // 도착 예정 시간을 표시
        }

// LatLng 객체로 변환
        pathCoords = new ArrayList<>();
        for (double[] coord : pathList) {
            pathCoords.add(new LatLng(coord[0], coord[1]));
        }


        // 출발지 및 도착지 좌표 설정
        startLatLng = new LatLng(startLat, startLng);
        endLatLng = new LatLng(endLat, endLng);

        // LatLng 객체로 변환
        pathCoords = new ArrayList<>();
        for (double[] coord : pathList) {
            pathCoords.add(new LatLng(coord[0], coord[1]));
        }

        // 지도 준비
        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        // 위치 업데이트 요청 설정
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000); // 1초마다 위치 업데이트
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // 위치 업데이트 콜백
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    updateCurrentLocationMarker(location);
                    updateRouteInfo(location);
                }
            }
        };

        // 위치 권한 확인 및 요청
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            // 위치 업데이트 요청 시작
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;

        // 경로가 있으면 지도에 경로 표시
        if (pathCoords != null && !pathCoords.isEmpty()) {
            if (pathOverlay != null) {
                pathOverlay.setMap(null);  // 기존 경로 제거
            }

            pathOverlay = new PathOverlay();
            pathOverlay.setCoords(pathCoords);  // 경로 좌표 설정
            pathOverlay.setMap(naverMap);  // 경로 지도에 표시

            // 카메라를 출발지 위치로 이동
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(startLatLng);
            naverMap.moveCamera(cameraUpdate);
        }


        // 경로 API 호출하여 경로 정보 업데이트
        fetchRouteInfo();

        // 현재 위치 마커 초기화
        currentLocationMarker = new Marker();
        currentLocationMarker.setIcon(OverlayImage.fromResource(R.drawable.ic_navigation_arrow));
        currentLocationMarker.setWidth(80);
        currentLocationMarker.setHeight(80);

        // 출발지로 확대하여 카메라 이동
        if (pathCoords != null && !pathCoords.isEmpty()) {
            CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(pathCoords.get(0), 25).animate(CameraAnimation.Easing);
            naverMap.moveCamera(cameraUpdate);
        }
    }

    private void updateCurrentLocationMarker(Location location) {
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        // 마커 위치 업데이트
        currentLocationMarker.setPosition(currentLatLng);
        currentLocationMarker.setMap(naverMap);

        // 카메라를 현재 위치로 이동
        CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(currentLatLng, 15)
                .pivot(new PointF(0.5f, 0.85f))  // 현재 위치를 지도 하단에 배치
                .animate(CameraAnimation.Easing);
        naverMap.moveCamera(cameraUpdate);
    }

    // 실시간 위치 업데이트 함수
    private void updateRouteInfo(Location currentLocation) {
        // 남은 거리 계산
        float[] distanceResult = new float[1];
        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                pathCoords.get(pathCoords.size() - 1).latitude, pathCoords.get(pathCoords.size() - 1).longitude, distanceResult);

        double distanceToEnd = distanceResult[0];  // 남은 거리 (미터 단위)

        // 남은 거리 UI 업데이트 (double 값을 전달)
        updateTotalDistance(distanceToEnd);
        // 회전 정보 업데이트
        updateNextTurnInfo(currentLocation);
    }


    // 경로 API 호출하여 경로 정보 가져오기
    private void fetchRouteInfo() {
        String clientId = "g7qv4saqzt";  // 네이버 API 클라이언트 ID
        String clientSecret = "1NlHYYs648uGmCnCa5RX664Z7RKQkhi8G26jEBVP";  // 네이버 API 클라이언트 Secret
        String startCoords = startLatLng.longitude + "," + startLatLng.latitude;
        String endCoords = endLatLng.longitude + "," + endLatLng.latitude;
        String url = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving?" +
                "start=" + startCoords + "&goal=" + endCoords + "&option=trafast";  // 가장 빠른 경로 옵션

        Request request = new Request.Builder()
                .url(url)
                .addHeader("X-NCP-APIGW-API-KEY-ID", clientId)
                .addHeader("X-NCP-APIGW-API-KEY", clientSecret)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String jsonResponse = response.body().string();
                    parseRouteData(jsonResponse);
                }
            }
        });
    }

    // 경로 정보 파싱 및 회전 정보와 속도 제한 갱신
    private void parseRouteData(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject route = jsonObject.getJSONObject("route");
            JSONArray trafastArray = route.getJSONArray("trafast");
            if (trafastArray.length() > 0) {
                JSONObject trafast = trafastArray.getJSONObject(0);

                // 회전 정보 및 좌표 정보 추출
                JSONArray sections = trafast.getJSONArray("section");
                for (int i = 0; i < sections.length(); i++) {
                    JSONObject section = sections.getJSONObject(i);
                    JSONArray roads = section.getJSONArray("roads");

                    for (int j = 0; j < roads.length(); j++) {
                        JSONObject road = roads.getJSONObject(j);

                        String roadName = road.getString("name");
                        String turnType = road.optString("turn", "");  // 회전 정보
                        int speedLimit = road.optInt("speed", 0);  // 속도 제한

                        // 좌표 정보
                        double latitude = road.getDouble("latitude");
                        double longitude = road.getDouble("longitude");

                        // 회전 정보와 좌표 정보를 담은 RouteSection 객체 추가
                        routeSections.add(new RouteSection(roadName, turnType, speedLimit, latitude, longitude));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // 실시간 위치와 경로 데이터를 비교하여 다음 회전 정보 및 속도 제한 정보 업데이트
    private void updateNextTurnInfo(Location currentLocation) {
        if (!routeSections.isEmpty()) {
            double currentLat = currentLocation.getLatitude();
            double currentLng = currentLocation.getLongitude();

            float[] result = new float[1];
            RouteSection nearestSection = null;
            double minDistance = Double.MAX_VALUE;

            // 경로 상의 각 회전 지점과 현재 위치를 비교하여 가장 가까운 회전 지점 찾기
            for (RouteSection section : routeSections) {
                if (!section.turnType.isEmpty()) { // 회전 정보가 있는 구간만 탐색
                    Location.distanceBetween(currentLat, currentLng, section.latitude, section.longitude, result);
                    double distance = result[0];
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestSection = section;
                    }
                }
            }

            // 가장 가까운 회전 지점이 있으면 회전 정보 업데이트
            if (nearestSection != null) {
                String turnType = nearestSection.turnType;
                int speedLimit = nearestSection.speedLimit;

                updateSpeedLimitIcon(speedLimit);  // 속도 제한 아이콘 업데이트
                updateNextTurn(turnType, minDistance);  // 다음 회전 정보 업데이트
            }
        }
    }

    // 속도 제한에 따른 아이콘 업데이트
    private void updateSpeedLimitIcon(int speedLimit) {
        ImageView speedLimitIcon = findViewById(R.id.imageView_speed_limit);
        switch (speedLimit) {
            case 30:
                speedLimitIcon.setImageResource(R.drawable.ic_speed_limit_30);
                break;
            case 60:
                speedLimitIcon.setImageResource(R.drawable.ic_speed_limit_60);
                break;
            case 80:
                speedLimitIcon.setImageResource(R.drawable.ic_speed_limit_80);
                break;
            default:
                speedLimitIcon.setImageResource(R.drawable.ic_speed_limit);  // 기본 속도 제한 아이콘
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 위치 업데이트 중지
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    // 경로 구간 정보를 저장하는 클래스
    private static class RouteSection {
        String roadName;
        String turnType;
        int speedLimit;
        double latitude;
        double longitude;

        public RouteSection(String roadName, String turnType, int speedLimit, double latitude, double longitude) {
            this.roadName = roadName;
            this.turnType = turnType;
            this.speedLimit = speedLimit;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private void updateArrivalTime(double durationMillis) {
        // 현재 시간을 가져옵니다.
        long currentTimeMillis = System.currentTimeMillis();

        // 도착 예상 시간을 계산합니다.
        long arrivalTimeMillis = currentTimeMillis + (long) durationMillis;

        // Calendar 객체로 변환하여 시각적 정보를 추출합니다.
        Calendar arrivalCalendar = Calendar.getInstance();
        arrivalCalendar.setTimeInMillis(arrivalTimeMillis);

        // 도착 시간 계산 (시, 분)
        int arrivalHour = arrivalCalendar.get(Calendar.HOUR_OF_DAY);
        int arrivalMinute = arrivalCalendar.get(Calendar.MINUTE);

        // 도착 시간 텍스트로 표시
        String arrivalTimeText = String.format("%02d:%02d", arrivalHour, arrivalMinute);
        textView_arrival_time.setText("도착 예정 시간: " + arrivalTimeText);
    }

    // 남은 거리 업데이트 함수
    private void updateTotalDistance(double distanceInMeters) {
        // distanceInKm 변수를 선언하고 계산
        double distanceInKm = distanceInMeters / 1000.0;  // 미터를 킬로미터로 변환

        // 텍스트뷰에 남은 거리를 업데이트
        textView_distance.setText(String.format("남은 거리: %.2f km", distanceInKm));
    }


    private void updateNextTurn(String turnType, double distanceToTurn) {
        String turnText = "";

        // 회전 타입에 따라 텍스트 설정
        if (turnType.equals("R")) {
            turnText = String.format("%.0f m 앞 우회전", distanceToTurn);
        } else if (turnType.equals("L")) {
            turnText = String.format("%.0f m 앞 좌회전", distanceToTurn);
        } else if (turnType.equals("U")) {
            turnText = String.format("%.0f m 앞 유턴", distanceToTurn);
        } else if (turnType.equals("S")) {
            turnText = String.format("%.0f m 앞 직진", distanceToTurn);
        } else {
            turnText = String.format("%.0f m 앞 도로 이동", distanceToTurn); // 기본값
        }

        // textView_next_turn에 표시
        textView_next_turn.setText(turnText);
    }

}
