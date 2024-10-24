package com.example.greenway;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private FusedLocationProviderClient fusedLocationClient;
    private FusedLocationSource locationSource;
    private FragmentManager map;
    private DrawerLayout drawerLayout;
    private NaverMap naverMap;
    private EditText searchEditText;
    private Button searchButton;
    private RelativeLayout loadingScreen;
    private LinearLayout buttonGroup;
    private boolean carMarkersVisible = false;
    private boolean bikeMarkersVisible = false;
    private boolean kickboardMarkersVisible = false;
    private List<Marker> carMarkers = new ArrayList<>();
    private List<Marker> bikeMarkers = new ArrayList<>();
    private List<Marker> kickboardMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 사용자의 즐겨찾기 초기화 (로그인한 사용자에 맞게)
        Bookmarker.initialize(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        searchEditText = findViewById(R.id.search_et);
        searchButton = findViewById(R.id.search_btn);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        loadingScreen = findViewById(R.id.loading_screen);
        buttonGroup = findViewById(R.id.button_group);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        map = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) map.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            map.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        searchEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Intent intent = new Intent(MainActivity.this, SearchPlaceActivity.class);
                intent.putExtra("caller", "MainActivity");
                startActivity(intent);
                return true;
            }
            return false;
        });

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String placeName = getIntent().getStringExtra("place_name");
        String address = getIntent().getStringExtra("address");

        if (placeName != null) {
            searchEditText.setText(placeName);  // 장소 이름을 EditText에 설정

        } else if (address != null) {
            searchEditText.setText(address);  // 주소를 설정
        }

        showLoadingScreen();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run(){
                hideLoadingScreen();
            }

        }, 3000);

        FloatingActionButton carButton = findViewById(R.id.car_button);
        carButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                if(carMarkersVisible){
                    removeMarkers(carMarkers);
                    carButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                    carButton.setImageResource(R.drawable.car);
                } else {
                    showMarkers(carMarkers);
                    carButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                    carButton.setImageResource(R.drawable.car);
                }
                carMarkersVisible = !carMarkersVisible;
            }
        });

        FloatingActionButton bikeButton = findViewById(R.id.bike_button);
        bikeButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                if(bikeMarkersVisible){
                    removeMarkers(bikeMarkers);
                    bikeButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                    bikeButton.setImageResource(R.drawable.bike);
                } else {
                    showMarkers(bikeMarkers);
                    bikeButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                    bikeButton.setImageResource(R.drawable.bike);
                }
                bikeMarkersVisible = !bikeMarkersVisible;
            }
        });

        FloatingActionButton kickboardButton =  findViewById(R.id.kickboard_button);
        kickboardButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                if(kickboardMarkersVisible){
                    removeMarkers(kickboardMarkers);
                    kickboardButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                    kickboardButton.setImageResource(R.drawable.kickboard);
                } else {
                    showMarkers(kickboardMarkers);
                    kickboardButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                    kickboardButton.setImageResource(R.drawable.kickboard);
                }
                kickboardMarkersVisible = !kickboardMarkersVisible;
            }
        });
    }

    private void showLoadingScreen(){
        loadingScreen.setVisibility(View.VISIBLE);
        buttonGroup.setVisibility(View.GONE);
    }

    private void hideLoadingScreen(){
        loadingScreen.setVisibility(View.GONE);
        buttonGroup.setVisibility(View.VISIBLE);
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);

        ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);

        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true);

        // 장소 정보가 전달되었는지 확인
        String placeName = getIntent().getStringExtra("place_name");
        double latitude = getIntent().getDoubleExtra("latitude", 0);
        double longitude = getIntent().getDoubleExtra("longitude", 0);

        if (placeName != null && latitude != 0 && longitude != 0) {
            // EditText에 장소 이름을 설정
            searchEditText.setText(placeName);

            // 해당 위치로 마커 추가
            LatLng placeLocation = new LatLng(latitude, longitude);
            Marker marker = new Marker();
            marker.setPosition(placeLocation);
            marker.setCaptionText(placeName);
            marker.setMap(naverMap);

            // 지도 이동
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(placeLocation).animate(CameraAnimation.Easing);
            naverMap.moveCamera(cameraUpdate);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    CameraUpdate cameraUpdate = CameraUpdate.scrollTo(currentLocation).animate(CameraAnimation.Easing);
                    naverMap.moveCamera(cameraUpdate);

                } else {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                }
            });
        }

        try {

            // 자동차 주차장 정보 가져오기 및 마커 추가
            List<ParkingInfo> parkingInfoList = ApiExplorer.getParkingInfo();
            Log.d("MainActivity", "Number of parking info loaded: " + parkingInfoList.size());

            if (parkingInfoList.isEmpty()) {
                // 주차장 정보가 없는 경우
                Toast.makeText(this, "No parking info available", Toast.LENGTH_SHORT).show();
            } else {
                // 주차장 정보가 있는 경우 마커 추가
                for (ParkingInfo parkingInfo : parkingInfoList) {
                    Marker marker = new Marker();
                    marker.setPosition(new LatLng(parkingInfo.getLat(), parkingInfo.getLng()));
                    marker.setCaptionText(parkingInfo.getParkingName());
                    marker.setIcon(OverlayImage.fromResource(R.drawable.car_parking_marker));
                    marker.setOnClickListener(overlay -> {
                        showCarParkingInfo(parkingInfo);
                        return true;
                    });
                    carMarkers.add(marker);
                }
            }

            // 자전거 대여소 정보 가져오기 및 마커 추가
            List<BikeInfo> bikeInfoList = ApiExplorer.getBikeInfo();
            if (bikeInfoList.isEmpty()) {
                Toast.makeText(this, "No bike info available", Toast.LENGTH_SHORT).show();
            } else {
                for (BikeInfo bikeInfo : bikeInfoList) {

                    // 마커 설정
                    Marker marker = new Marker();
                    marker.setPosition(new LatLng(bikeInfo.getLat(), bikeInfo.getLng()));
                    marker.setCaptionText(bikeInfo.getStationName());
                    marker.setIcon(OverlayImage.fromResource(R.drawable.bike_parking_marker));
                    marker.setOnClickListener(overlay -> {
                        showBikeInfo(bikeInfo);  // 클릭 시 BikeInfo 객체 전달
                        return true;
                    });
                    bikeMarkers.add(marker);
                }
            }


            // 킥보드 주차구역 정보 가져오기 및 마커 추가
            List<KickboardInfo> kickboardInfoList = ApiExplorer.getKickboardParkingInfo();
            if (kickboardInfoList.isEmpty()) {
                Toast.makeText(this, "No kickboard parking info available", Toast.LENGTH_SHORT).show();
            } else {
                for (KickboardInfo kickboardInfo : kickboardInfoList) {
                    LatLng position = ApiExplorer.getLatLngFromAddress(kickboardInfo.getLocation());
                    if (position != null) {
                        Marker marker = new Marker();
                        marker.setPosition(position);
                        marker.setCaptionText(kickboardInfo.getLocation());
                        marker.setIcon(OverlayImage.fromResource(R.drawable.kickboard_parking_marker));
                        marker.setOnClickListener(overlay -> {
                            showKickboardInfo(kickboardInfo);
                            return true;
                        });
                        kickboardMarkers.add(marker);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // 주차장 정보 가져오기 실패 시
            Toast.makeText(this, "Error fetching parking info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        String address = getIntent().getStringExtra("address");
        if (address != null) {
            addMarkerToMap(address);
        }

    }

    private void showMarkers(List<Marker> markers) {
        for (Marker marker : markers) {
            marker.setMap(naverMap);
        }
    }

    private void removeMarkers(List<Marker> markers) {
        for (Marker marker : markers) {
            marker.setMap(null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                        if (location != null) {
                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(currentLocation).animate(CameraAnimation.Easing);
                            naverMap.moveCamera(cameraUpdate);

                        } else {
                            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    });
                    naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
                }
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void addMarkerToMap(String address) {
        new GeocodingTask().execute(address);
    }

    private class GeocodingTask extends AsyncTask<String, Void, LatLng> {
        @Override
        protected LatLng doInBackground(String... params) {
            String address = params[0];
            String clientId = "g7qv4saqzt";
            String clientSecret = "1NlHYYs648uGmCnCa5RX664Z7RKQkhi8G26jEBVP";
            String urlString = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=" + address;

            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", clientId);
                connection.setRequestProperty("X-NCP-APIGW-API-KEY", clientSecret);
                connection.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder jsonResult = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonResult.append(line);
                }
                reader.close();

                JSONObject jsonObject = new JSONObject(jsonResult.toString());
                JSONArray addresses = jsonObject.getJSONArray("addresses");
                if (addresses.length() > 0) {
                    JSONObject location = addresses.getJSONObject(0);
                    double lat = location.getDouble("y");
                    double lng = location.getDouble("x");
                    return new LatLng(lat, lng);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(LatLng latLng) {
            if (latLng != null && naverMap != null) {
                Marker marker = new Marker();
                marker.setPosition(latLng);
                marker.setIcon(OverlayImage.fromResource(R.drawable.destination));
                marker.setMap(naverMap);

                CameraUpdate cameraUpdate = CameraUpdate.scrollTo(latLng).animate(CameraAnimation.Easing);
                naverMap.moveCamera(cameraUpdate);
            } else {
                Toast.makeText(MainActivity.this, "주소로부터 좌표를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_navigation) {// Handle navigation
            Intent intent = new Intent(this, NavigationActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_bookmark) {// Handle bookmark
            Intent intent = new Intent(this, BookmarkListActivity.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_near) {// Handle near
            Intent intent = new Intent(this, CurrentLocationNear.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_map_standard) {// Handle near
            Intent intent = new Intent(this, MapStandardList.class);
            startActivity(intent);
        } else if (itemId == R.id.logout) {
            // Handle logout
            logout(); // 로그아웃 함수 호출
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        // SharedPreferences에서 로그인 정보 삭제
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();  // 모든 로그인 관련 데이터를 삭제
        editor.apply();

        // 로그인 화면으로 이동
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);  // 백스택 초기화
        startActivity(intent);

        // 로그아웃 메시지 출력
        Toast.makeText(MainActivity.this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void showCarParkingInfo(ParkingInfo parkingInfo) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.marker_car_parking_info, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        TextView parkingNameTextView = bottomSheetView.findViewById(R.id.car_parking_name);
        TextView parkingAddressTextView = bottomSheetView.findViewById(R.id.car_parking_address);
        ImageButton bookmarkerButton = bottomSheetView.findViewById(R.id.bookmarker_button);

        Button buttonStart = bottomSheetView.findViewById(R.id.button_start);
        Button buttonEnd = bottomSheetView.findViewById(R.id.button_end);

        parkingNameTextView.setText(parkingInfo.getParkingName());
        parkingAddressTextView.setText(parkingInfo.getAddr());

        if (Bookmarker.isParkingBookmarked(this, parkingInfo)) {
            bookmarkerButton.setImageResource(R.drawable.ic_filled_bookmark);
            bookmarkerButton.setTag(R.drawable.ic_filled_bookmark);
        } else {
            bookmarkerButton.setImageResource(R.drawable.ic_empty_bookmarker);
            bookmarkerButton.setTag(R.drawable.ic_empty_bookmarker);
        }

        bookmarkerButton.setOnClickListener(v -> {
            int currentTag = (int) bookmarkerButton.getTag();
            if (currentTag == R.drawable.ic_empty_bookmarker) {
                bookmarkerButton.setImageResource(R.drawable.ic_filled_bookmark);
                bookmarkerButton.setTag(R.drawable.ic_filled_bookmark);
                Bookmarker.addParking(MainActivity.this, parkingInfo); // 주차장 즐겨찾기에 추가
                Toast.makeText(MainActivity.this, "즐겨찾기에 등록되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                bookmarkerButton.setImageResource(R.drawable.ic_empty_bookmarker);
                bookmarkerButton.setTag(R.drawable.ic_empty_bookmarker);
                Bookmarker.removeParking(MainActivity.this, parkingInfo); // 주차장 즐겨찾기에서 제거
                Toast.makeText(MainActivity.this, "즐겨찾기에서 제거되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // Start 버튼 클릭 리스너
        buttonStart.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NavigationActivity.class);
            intent.putExtra("car_parking_name", parkingInfo.getParkingName());
            intent.putExtra("isStart", true);
            intent.putExtra("lat", parkingInfo.getLat());
            intent.putExtra("lng", parkingInfo.getLng());
            startActivity(intent);
            bottomSheetDialog.dismiss();
        });

        // End 버튼 클릭 리스너
        buttonEnd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, NavigationActivity.class);
            intent.putExtra("car_parking_name", parkingInfo.getParkingName());
            intent.putExtra("isStart", false);
            intent.putExtra("lat", parkingInfo.getLat());
            intent.putExtra("lng", parkingInfo.getLng());
            startActivity(intent);
            bottomSheetDialog.dismiss();
        });


        bottomSheetDialog.show();
    }

    private void showBikeInfo(BikeInfo bikeInfo) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.marker_bike_parking_info, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        TextView stationNameTextView = bottomSheetView.findViewById(R.id.bike_parking_name);
        TextView bikeParkingAddressTextView = bottomSheetView.findViewById(R.id.bike_parking_address);
        ImageButton bookmarkerButton = bottomSheetView.findViewById(R.id.bookmarker_button);

        // 자전거 대여소 이름 설정
        stationNameTextView.setText(bikeInfo.getStationName());

        // 가져온 주소를 바로 표시
        bikeParkingAddressTextView.setText(bikeInfo.getAddress() != null ? bikeInfo.getAddress() : "주소를 찾을 수 없습니다.");

        // 즐겨찾기 상태에 따라 초기 아이콘 설정
        if (Bookmarker.isBikeBookmarked(this, bikeInfo)) {
            bookmarkerButton.setImageResource(R.drawable.ic_filled_bookmark);
            bookmarkerButton.setTag(R.drawable.ic_filled_bookmark);
        } else {
            bookmarkerButton.setImageResource(R.drawable.ic_empty_bookmarker);
            bookmarkerButton.setTag(R.drawable.ic_empty_bookmarker);
        }

        // 즐겨찾기 버튼 클릭 시 아이콘 변경 및 즐겨찾기 추가/제거
        bookmarkerButton.setOnClickListener(v -> {
            int currentTag = (int) bookmarkerButton.getTag();
            if (currentTag == R.drawable.ic_empty_bookmarker) {
                bookmarkerButton.setImageResource(R.drawable.ic_filled_bookmark);
                bookmarkerButton.setTag(R.drawable.ic_filled_bookmark);
                Bookmarker.addBike(MainActivity.this, bikeInfo); // 자전거 대여소 즐겨찾기에 추가
                Toast.makeText(MainActivity.this, "즐겨찾기에 등록되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                bookmarkerButton.setImageResource(R.drawable.ic_empty_bookmarker);
                bookmarkerButton.setTag(R.drawable.ic_empty_bookmarker);
                Bookmarker.removeBike(MainActivity.this, bikeInfo); // 자전거 대여소 즐겨찾기에서 제거
                Toast.makeText(MainActivity.this, "즐겨찾기에서 제거되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheetDialog.show();
    }



    private void showKickboardInfo(KickboardInfo kickboardInfo) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.marker_kickboard_parking_info, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        TextView kickboardAddressTextView = bottomSheetView.findViewById(R.id.kickboard_parking_address);
        TextView hasStandTextView = bottomSheetView.findViewById(R.id.has_stand);
        ImageButton bookmarkerButton = bottomSheetView.findViewById(R.id.bookmarker_button);

        kickboardAddressTextView.setText(kickboardInfo.getLocation());
        hasStandTextView.setText(kickboardInfo.isHasStand() ? "스탠드 있음" : "스탠드 없음");

        // 즐겨찾기 상태에 따라 초기 아이콘 설정
        if (Bookmarker.isKickboardBookmarked(this, kickboardInfo)) {
            bookmarkerButton.setImageResource(R.drawable.ic_filled_bookmark);
            bookmarkerButton.setTag(R.drawable.ic_filled_bookmark);
        } else {
            bookmarkerButton.setImageResource(R.drawable.ic_empty_bookmarker);
            bookmarkerButton.setTag(R.drawable.ic_empty_bookmarker);
        }

        // 즐겨찾기 버튼 클릭 시 아이콘 변경 및 즐겨찾기 추가/제거
        bookmarkerButton.setOnClickListener(v -> {
            int currentTag = (int) bookmarkerButton.getTag();
            if (currentTag == R.drawable.ic_empty_bookmarker) {
                bookmarkerButton.setImageResource(R.drawable.ic_filled_bookmark);
                bookmarkerButton.setTag(R.drawable.ic_filled_bookmark);
                Bookmarker.addKickboard(MainActivity.this, kickboardInfo); // 자전거 대여소 즐겨찾기에 추가
                Toast.makeText(MainActivity.this, "즐겨찾기에 등록되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                bookmarkerButton.setImageResource(R.drawable.ic_empty_bookmarker);
                bookmarkerButton.setTag(R.drawable.ic_empty_bookmarker);
                Bookmarker.removeKickboard(MainActivity.this, kickboardInfo); // 자전거 대여소 즐겨찾기에서 제거
                Toast.makeText(MainActivity.this, "즐겨찾기에서 제거되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        bottomSheetDialog.show();
    }

}