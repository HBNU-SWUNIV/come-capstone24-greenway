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
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
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

public class MapStandardList extends FragmentActivity implements OnMapReadyCallback {
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
    private Spinner sortingSpinner;
    private boolean isListVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_standard_list);

        parkingListView = findViewById(R.id.parking_list_view);
        showParkingListButton = findViewById(R.id.show_parking_list_button);
        searchEditText = findViewById(R.id.search_edit_text);
        searchButton = findViewById(R.id.search_button);
        sortingSpinner = findViewById(R.id.spinner);

        fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);


        searchEditText.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                Intent intent = new Intent(MapStandardList.this, SearchPlaceActivity.class);
                intent.putExtra("caller", "mapStandardList");
                startActivity(intent);
                return true;
            }
            return false;
        });

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String address = getIntent().getStringExtra("address");
        String placeName = getIntent().getStringExtra("place_name");

        if (placeName != null) {
            searchEditText.setText(placeName);
        }

        // 초기 상태를 리스트 창 닫힌 상태로 설정
        parkingListView.setVisibility(View.GONE);
        sortingSpinner.setVisibility(View.GONE);
        isListVisible = false;

        // 리스트 창 토글 버튼 리스너 설정
        showParkingListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleListVisibility();
            }
        });

        parkingListView.setOnItemClickListener((parent, view, position, id) -> {
            ParkingInfo selectedParkingInfo = parkingInfoList.get(position);
            showParkingInfoDialog(selectedParkingInfo);
        });

        // 스피너 설정
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.sorting_options, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortingSpinner.setAdapter(spinnerAdapter);

        // 스피너 항목 선택 리스너 설정
        sortingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // 선택된 항목에 따라 정렬 수행
                showSortedParkingList();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
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

        ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);

        String address = getIntent().getStringExtra("address");
        if (address != null) {
            addMarkerToMap(address);
        }
    }

    private void addMarkerToMap(String address) {
        new GeocodingTask().execute(address);
    }

    // 리스트 창 토글 메서드
    private void toggleListVisibility() {
        if (isListVisible) {
            // 리스트 창 닫기
            parkingListView.setVisibility(View.GONE);
            sortingSpinner.setVisibility(View.GONE);
            isListVisible = false;
        } else {
            // 리스트 창 열기
            showSortedParkingList();
            parkingListView.setVisibility(View.VISIBLE);
            sortingSpinner.setVisibility(View.VISIBLE);
            isListVisible = true;
        }
    }

    // 정렬 기준에 따른 리스트 정렬
    private void showSortedParkingList() {
        if (currentCenter == null || parkingInfoList.isEmpty()) {
            Toast.makeText(this, "Current location or parking info not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // 스피너에서 선택된 옵션 가져오기
        String selectedOption = sortingSpinner.getSelectedItem().toString();

        if (selectedOption.equals("거리순")) {
            // 거리순으로 정렬
            Collections.sort(parkingInfoList, new Comparator<ParkingInfo>() {
                @Override
                public int compare(ParkingInfo p1, ParkingInfo p2) {
                    float[] result1 = new float[1];
                    float[] result2 = new float[1];
                    Location.distanceBetween(currentCenter.latitude, currentCenter.longitude, p1.getLat(), p1.getLng(), result1);
                    Location.distanceBetween(currentCenter.latitude, currentCenter.longitude, p2.getLat(), p2.getLng(), result2);
                    return Float.compare(result1[0], result2[0]);
                }
            });
        } else if (selectedOption.equals("가격순")) {
            // 가격순으로 정렬
            Collections.sort(parkingInfoList, new Comparator<ParkingInfo>() {
                @Override
                public int compare(ParkingInfo p1, ParkingInfo p2) {
                    return Integer.compare(p1.getBscPrkCrg(), p2.getBscPrkCrg());
                }
            });
        }

        // 정렬된 리스트를 업데이트하여 화면에 표시
        updateParkingList();
    }

    // 리스트 업데이트 함수
    private void updateParkingList() {
        List<String> parkingNames = new ArrayList<>();
        for (ParkingInfo parkingInfo : parkingInfoList) {
            float[] distanceResult = new float[1];
            Location.distanceBetween(currentCenter.latitude, currentCenter.longitude, parkingInfo.getLat(), parkingInfo.getLng(), distanceResult);
            float distance = distanceResult[0] / 1000; // km로 변환

            int availableSpots = parkingInfo.getCapacity() - parkingInfo.getCurParking();
            String spotDisplay = availableSpots > 0 ? availableSpots + "자리" : "만석";

            String parkingInfoItem = String.format(
                    "%s\n%.2f km | %d원 | %s",
                    parkingInfo.getParkingName(),
                    distance,
                    parkingInfo.getBscPrkCrg(),
                    spotDisplay
            );
            parkingNames.add(parkingInfoItem);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, parkingNames);
        parkingListView.setAdapter(adapter);

        parkingListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < parkingInfoList.size()) { // 유효한 인덱스 확인
                ParkingInfo selectedParkingInfo = parkingInfoList.get(position);
                showParkingInfoDialog(selectedParkingInfo); // 다이얼로그 표시
            } else {
                Toast.makeText(this, "Invalid parking info selected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAndShowParkingInfo() {
        new AsyncTask<Void, Void, List<ParkingInfo>>() {
            @Override
            protected List<ParkingInfo> doInBackground(Void... voids) {
                try {
                    return ApiExplorer.getParkingInfo();
                } catch (IOException e) {
                    e.printStackTrace();
                    return new ArrayList<>();
                }
            }

            @Override
            protected void onPostExecute(List<ParkingInfo> result) {
                parkingInfoList = result;
                if (!parkingInfoList.isEmpty()) {
                    for (ParkingInfo parkingInfo : parkingInfoList) {
                        Marker marker = new Marker();
                        marker.setPosition(new LatLng(parkingInfo.getLat(), parkingInfo.getLng()));
                        marker.setCaptionText(parkingInfo.getParkingName());
                        marker.setIcon(OverlayImage.fromResource(R.drawable.car_parking_marker));
                        marker.setMap(naverMap);
                        marker.setOnClickListener(overlay -> {
                            showParkingInfoDialog(parkingInfo);
                            return true;
                        });
                    }
                } else {
                    Toast.makeText(MapStandardList.this, "No parking info available", Toast.LENGTH_SHORT).show();
                }
                showSortedParkingList();
            }
        }.execute();
    }

    private void showParkingInfoDialog(ParkingInfo parkingInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_parking_info, null);
        builder.setView(dialogView);

        TextView parkingNameTextView = dialogView.findViewById(R.id.parking_name);
        TextView parkingAddressTextView = dialogView.findViewById(R.id.parking_address);
        TextView parkingCapacityTextView = dialogView.findViewById(R.id.parking_capacity);
        TextView parkingCurrentTextView = dialogView.findViewById(R.id.parking_current);
        TextView parkingBaseFeeTextView = dialogView.findViewById(R.id.parking_base_fee);
        TextView parkingAdditionalTimeTextView = dialogView.findViewById(R.id.parking_additional_time);

        parkingNameTextView.setText(parkingInfo.getParkingName());
        parkingAddressTextView.setText(parkingInfo.getAddr());
        parkingCurrentTextView.setText(String.valueOf(parkingInfo.getCurParking()));
        parkingCapacityTextView.setText(String.valueOf(parkingInfo.getCapacity()));
        parkingBaseFeeTextView.setText(parkingInfo.getBscPrkCrg() + "원");
        parkingAdditionalTimeTextView.setText(parkingInfo.getAddPrkHr() + "분");

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
            if (latLng != null) {
                currentCenter = latLng;
                Marker marker = new Marker();
                marker.setPosition(latLng);
                marker.setMap(naverMap);
                marker.setIcon(OverlayImage.fromResource(R.drawable.destination));

                CameraUpdate cameraUpdate = CameraUpdate.scrollTo(latLng).animate(CameraAnimation.Easing);
                naverMap.moveCamera(cameraUpdate);

                fetchAndShowParkingInfo();
            } else {
                Toast.makeText(MapStandardList.this, "주소로부터 좌표를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
