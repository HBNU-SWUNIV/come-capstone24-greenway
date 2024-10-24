package com.example.greenway;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;

public class CurrentLocationNear extends FragmentActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private FusedLocationSource locationSource;
    private FragmentManager fm;
    private NaverMap naverMap;
    private Location currentLocation;
    private List<ParkingInfo> parkingInfoList = new ArrayList<>();
    private ListView parkingListView;
    private Button showParkingListButton;
    private Spinner sortingSpinner;  // 스피너 변수 선언

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.current_location_near_list);

        parkingListView = findViewById(R.id.parking_list_view);
        showParkingListButton = findViewById(R.id.show_parking_list_button);
        sortingSpinner = findViewById(R.id.spinner);

        fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        showParkingListButton.setOnClickListener(new View.OnClickListener() {
            private boolean isListVisible = false; // 보이는지 여부를 저장

            @Override
            public void onClick(View v) {
                ConstraintLayout.LayoutParams mapLayoutParams = (ConstraintLayout.LayoutParams) findViewById(R.id.map).getLayoutParams();
                ConstraintLayout.LayoutParams listLayoutParams = (ConstraintLayout.LayoutParams) parkingListView.getLayoutParams();
                ConstraintLayout.LayoutParams spinnerLayoutParams = (ConstraintLayout.LayoutParams) sortingSpinner.getLayoutParams();

                if (!isListVisible) {
                    // 인근 주차장 리스트 보이기
                    showSortedParkingList();
                    parkingListView.setVisibility(View.VISIBLE);
                    sortingSpinner.setVisibility(View.VISIBLE);

                    // MapFragment 크기 조정
                    mapLayoutParams.height = 0;
                    mapLayoutParams.bottomToTop = R.id.show_parking_list_button; // 맵의 하단을 버튼에 제약
                    mapLayoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;

                    // 스피너 보이도록 크기 조정
                    spinnerLayoutParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT;

                    // 리스트뷰 보이도록 크기 조정
                    listLayoutParams.height = 0;
                    listLayoutParams.topToBottom = R.id.spinner;

                    isListVisible = true; // 리스트가 보이는 상태로 전환
                } else {
                    // MapFragment를 처음 상태로 되돌리기
                    mapLayoutParams.height = 0;
                    mapLayoutParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                    mapLayoutParams.bottomToTop = R.id.show_parking_list_button;

                    // 스피너 숨기기
                    sortingSpinner.setVisibility(View.GONE);

                    // 리스트뷰 숨기기
                    parkingListView.setVisibility(View.GONE);

                    isListVisible = false; // 리스트가 숨겨진 상태로 전환
                }

                // 변경된 레이아웃 파라미터를 적용
                findViewById(R.id.map).setLayoutParams(mapLayoutParams);
                parkingListView.setLayoutParams(listLayoutParams);
                sortingSpinner.setLayoutParams(spinnerLayoutParams);
            }
        });

        parkingListView.setOnItemClickListener((parent, view, position, id) -> {
            ParkingInfo selectedParkingInfo = (ParkingInfo) parent.getItemAtPosition(position);
            showParkingInfoDialog(selectedParkingInfo);
        });


        // 스피너에 표시될 항목 설정
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.sorting_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortingSpinner.setAdapter(adapter);

        // 스피너 항목 선택 이벤트 처리
        sortingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedOption = parentView.getItemAtPosition(position).toString();

                if (selectedOption.equals("가격순")) {
                    // 주차장을 가격순으로 정렬
                    Collections.sort(parkingInfoList, new Comparator<ParkingInfo>() {
                        @Override
                        public int compare(ParkingInfo p1, ParkingInfo p2) {
                            return Integer.compare(p1.getBscPrkCrg(), p2.getBscPrkCrg());
                        }
                    });

                    // 리스트를 새로고침하여 정렬된 주차장 정보를 표시
                    updateParkingList();
                } else if (selectedOption.equals("거리순")) {
                    // 기존 거리순 정렬 함수 호출
                    showSortedParkingList();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // 아무 항목도 선택되지 않은 경우 기본 동작을 수행하지 않습니다.
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

        ActivityCompat.requestPermissions(this, PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);

        naverMap.addOnLocationChangeListener(location -> {
            currentLocation = location;
            try {
                new FetchParkingDataTask().execute();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error fetching info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class FetchParkingDataTask extends AsyncTask<Void, Void, List<ParkingInfo>> {
        @Override
        protected List<ParkingInfo> doInBackground(Void... voids) {
            try {
                return ApiExplorer.getParkingInfo();
            } catch (IOException e) {
                e.printStackTrace();
                return Collections.emptyList();
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
                        showParkingInfoDialog(parkingInfo); // 마커 클릭 시 다이얼로그 표시
                        return true;
                    });
                }
            } else {
                Toast.makeText(CurrentLocationNear.this, "No parking info available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 인근 주차장 찾기 리스트
    /*private void showSortedParkingList() {
        if (currentLocation == null || parkingInfoList.isEmpty()) {
            Toast.makeText(this, "Current location or parking info not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedOption = sortingSpinner.getSelectedItem().toString();

        if (selectedOption.equals("거리순")) {
            Collections.sort(parkingInfoList, new Comparator<ParkingInfo>() {
                @Override
                public int compare(ParkingInfo p1, ParkingInfo p2) {
                    float[] result1 = new float[1];
                    float[] result2 = new float[1];
                    Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), p1.getLat(), p1.getLng(), result1);
                    Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), p2.getLat(), p2.getLng(), result2);
                    return Float.compare(result1[0], result2[0]);
                }
            });
        } else if (selectedOption.equals("가격순")) {
            Collections.sort(parkingInfoList, new Comparator<ParkingInfo>() {
                @Override
                public int compare(ParkingInfo p1, ParkingInfo p2) {
                    return Integer.compare(p1.getBscPrkCrg(), p2.getBscPrkCrg());
                }
            });
        }

        List<String> parkingNames = new ArrayList<>();
        for (ParkingInfo parkingInfo : parkingInfoList) {
            float[] distanceResult = new float[1];
            Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), parkingInfo.getLat(), parkingInfo.getLng(), distanceResult);
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

        // 리스트 항목 클릭 시 해당 주차장 정보를 다이얼로그로 표시
        parkingListView.setOnItemClickListener((parent, view, position, id) -> {
            // 클릭된 위치의 parkingInfo를 가져옴
            if (position >= 0 && position < parkingInfoList.size()) { // 유효한 인덱스 확인
                ParkingInfo selectedParkingInfo = parkingInfoList.get(position);
                showParkingInfoDialog(selectedParkingInfo);
            } else {
                Toast.makeText(this, "Invalid parking info selected", Toast.LENGTH_SHORT).show();
            }
        });
    }*/

    // 주차장 정보를 표시하는 다이얼로그 메서드 추가
    private void showParkingInfoDialog(ParkingInfo parkingInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_parking_info, null);
        builder.setView(dialogView);

        // 다이얼로그에 표시할 TextView를 찾습니다.
        TextView parkingNameTextView = dialogView.findViewById(R.id.parking_name);
        TextView parkingAddressTextView = dialogView.findViewById(R.id.parking_address);
        TextView parkingCapacityTextView = dialogView.findViewById(R.id.parking_capacity);
        TextView parkingCurrentTextView = dialogView.findViewById(R.id.parking_current);
        TextView parkingBaseFeeTextView = dialogView.findViewById(R.id.parking_base_fee);
        TextView parkingAdditionalTimeTextView = dialogView.findViewById(R.id.parking_additional_time);

        // ParkingInfo 객체에서 정보를 가져와서 다이얼로그에 설정합니다.
        parkingNameTextView.setText(parkingInfo.getParkingName());
        parkingAddressTextView.setText(parkingInfo.getAddr());
        parkingCurrentTextView.setText(String.valueOf(parkingInfo.getCurParking()));  // int -> String 변환
        parkingCapacityTextView.setText(String.valueOf(parkingInfo.getCapacity()));  // int -> String 변환
        parkingBaseFeeTextView.setText(parkingInfo.getBscPrkCrg() + "원");
        parkingAdditionalTimeTextView.setText(parkingInfo.getAddPrkHr() + "분");

        // '닫기' 버튼을 설정합니다.
        builder.setPositiveButton("닫기", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // 다이얼로그를 생성하고 표시합니다.
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 주차장 리스트를 갱신하는 메서드
    private void updateParkingList() {
        if (currentLocation == null) {
            Toast.makeText(this, "Current location is not available", Toast.LENGTH_SHORT).show();
            return;
        }

        ParkingInfoAdapter adapter = new ParkingInfoAdapter(parkingInfoList, currentLocation);
        parkingListView.setAdapter(adapter);
    }

    // 인근 주차장 찾기 리스트
    private void showSortedParkingList() {
        if (currentLocation == null || parkingInfoList.isEmpty()) {
            Toast.makeText(this, "Current location or parking info not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // 선택된 정렬 옵션을 확인
        String selectedOption = sortingSpinner.getSelectedItem().toString();

        if (selectedOption.equals("거리순")) {
            Collections.sort(parkingInfoList, (p1, p2) -> {
                float[] result1 = new float[1];
                float[] result2 = new float[1];
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), p1.getLat(), p1.getLng(), result1);
                Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), p2.getLat(), p2.getLng(), result2);
                return Float.compare(result1[0], result2[0]);
            });
        } else if (selectedOption.equals("가격순")) {
            Collections.sort(parkingInfoList, (p1, p2) -> Integer.compare(p1.getBscPrkCrg(), p2.getBscPrkCrg()));
        }

        // 데이터 정렬 후 리스트뷰 갱신
        updateParkingList();
    }

}
