package com.example.greenway;


import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class mapStandardList extends FragmentActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private FusedLocationSource locationSource;
    private Location currentLocation;

    private EditText searchEditText;
    private Button searchButton;
    private FragmentManager fm;
    private NaverMap naverMap;
    private ListView parkingListView;
    private Button showParkingListButton;
    private LatLng currentCenter;
    private List<ParkingInfo> parkingInfoList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_standard_list);

        parkingListView = findViewById(R.id.parking_list_view);
        showParkingListButton = findViewById(R.id.show_parking_list_button);
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);

        fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);


        searchEditText.setOnClickListener(v -> {
            Intent intent = new Intent(mapStandardList.this, SearchPlaceActivity.class);
            intent.putExtra("caller", "mapStandardList");
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


        String address = getIntent().getStringExtra("address");
        if (address != null) {
            addMarkerToMap(address);
        }
    }

    private void addMarkerToMap(String address) {
        new GeocodingTask().execute(address);
    }






    // 주차장 정보를 표시하는 다이얼로그 메서드 추가
    private void showParkingInfoDialog(ParkingInfo parkingInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_parking_info, null);
        builder.setView(dialogView);

        TextView parkingNameTextView = dialogView.findViewById(R.id.parking_name);
        TextView parkingAddressTextView = dialogView.findViewById(R.id.parking_address);
        TextView parkingCapacityTextView = dialogView.findViewById(R.id.parking_capacity);
        TextView parkingCurrentTextView = dialogView.findViewById(R.id.parking_current);

        parkingNameTextView.setText(parkingInfo.getParkingName());
        parkingAddressTextView.setText(parkingInfo.getAddr());
        parkingCapacityTextView.setText("총 주차면: " + parkingInfo.getCapacity());
        parkingCurrentTextView.setText("현재 주차 차량수: " + parkingInfo.getCurParking());

        builder.setPositiveButton("닫기", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
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
                Toast.makeText(mapStandardList.this, "주소로부터 좌표를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
