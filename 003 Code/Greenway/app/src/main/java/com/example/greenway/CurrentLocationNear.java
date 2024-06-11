package com.example.greenway;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

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
import java.util.Currency;
import java.util.List;

// 필요한 임포트 추가
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.current_location_near_list);

        fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        locationSource = new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        parkingListView = findViewById(R.id.parking_list_view);
        showParkingListButton = findViewById(R.id.show_parking_list_button);

        showParkingListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSortedParkingList();
                parkingListView.setVisibility(View.VISIBLE);
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
                    marker.setIcon(OverlayImage.fromResource(R.drawable.parking_marker));
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

    private void showSortedParkingList() {
        if (currentLocation == null || parkingInfoList.isEmpty()) {
            Toast.makeText(this, "Current location or parking info not available", Toast.LENGTH_SHORT).show();
            return;
        }

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

        List<String> parkingNames = new ArrayList<>();
        for (ParkingInfo parkingInfo : parkingInfoList) {
            float[] distanceResult = new float[1];
            Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), parkingInfo.getLat(), parkingInfo.getLng(), distanceResult);
            float distance = distanceResult[0];
            parkingNames.add(parkingInfo.getParkingName() + " (" + parkingInfo.getAddr() + ") - " + String.format("%.2f", distance / 1000) + " km");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, parkingNames);
        parkingListView.setAdapter(adapter);
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
}
