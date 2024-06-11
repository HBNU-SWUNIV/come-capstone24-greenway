package com.example.greenway;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

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
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private FusedLocationSource locationSource;
    private FragmentManager map;
    private DrawerLayout drawerLayout;
    private NaverMap naverMap;
    private EditText searchEditText;
    private Button searchButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        searchEditText = findViewById(R.id.search_et);
        searchButton = findViewById(R.id.search_btn);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        map = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)map.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            map.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);


        searchEditText.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchPlaceActivity.class);
            intent.putExtra("caller", "MainActivity");
            startActivity(intent);
        });

        // 네트워크 작업을 메인 스레드에서 실행하기 위해 StrictMode 설정 (권장하지 않음, 임시로 사용)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // 인텐트에서 주소 정보를 가져옵니다.
        String address = getIntent().getStringExtra("address");

        // EditText에 주소를 설정합니다.
        if (address != null) {
            searchEditText.setText(address);
            // 지도에 마커를 추가하는 메소드 호출
            addMarkerToMap(address);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // request code와 권한획득 여부 확인
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);

        // 권한확인. 결과는 onRequestPermissionsResult 콜백 매서드 호출
        ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);

        UiSettings uiSettings = naverMap.getUiSettings();
        uiSettings.setLocationButtonEnabled(true);

        try {
            List<ParkingInfo> parkingInfoList = ApiExplorer.getParkingInfo();
            if (parkingInfoList.isEmpty()) {
                // 주차장 정보가 없는 경우
                Toast.makeText(this, "No parking info available", Toast.LENGTH_SHORT).show();
            } else {
                // 주차장 정보가 있는 경우 마커 추가
                for (ParkingInfo parkingInfo : parkingInfoList) {
                    Marker marker = new Marker();
                    marker.setPosition(new LatLng(parkingInfo.getLat(), parkingInfo.getLng()));
                    marker.setCaptionText(parkingInfo.getParkingName());
                    marker.setMap(naverMap);
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

    private void addMarkerToMap(String address) {
        new GeocodingTask().execute(address);
    }

    private class GeocodingTask extends AsyncTask<String, Void, LatLng> {
        @Override
        protected LatLng doInBackground(String... params) {
            String address = params[0];
            String clientId = "g7qv4saqzt";  // Replace with your actual Naver Client ID
            String clientSecret = "1NlHYYs648uGmCnCa5RX664Z7RKQkhi8G26jEBVP";  // Replace with your actual Naver Client Secret
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
        } else if (itemId == R.id.nav_bookmark) {// Handle bookmark
        } else if (itemId == R.id.nav_near) {// Handle near
            Intent intent = new Intent(this, CurrentLocationNear.class);
            startActivity(intent);
        } else if (itemId == R.id.nav_map_standard) {// Handle near
            Intent intent = new Intent(this, mapStandardList.class);
            startActivity(intent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
