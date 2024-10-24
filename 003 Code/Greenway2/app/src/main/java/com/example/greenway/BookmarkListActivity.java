package com.example.greenway;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BookmarkListActivity extends AppCompatActivity implements BookmarkAdapter.OnBookmarkClickListener {

    private RecyclerView recyclerView;
    private BookmarkAdapter adapter;
    private List<Object> allBookmarks = new ArrayList<>();
    private List<Object> filteredBookmarks = new ArrayList<>();

    private Spinner filterSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarker_list);

        filterSpinner = findViewById(R.id.filter_spinner);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 모든 즐겨찾기 항목을 로드
        allBookmarks.addAll(Bookmarker.getParkingBookmarkList(this));
        allBookmarks.addAll(Bookmarker.getBikeBookmarkList(this));
        allBookmarks.addAll(Bookmarker.getKickboardBookmarkList(this));

        filteredBookmarks.addAll(allBookmarks); // 초기에는 모든 항목을 표시
        adapter = new BookmarkAdapter(this, filteredBookmarks, this);
        recyclerView.setAdapter(adapter);

        // 스피너의 선택 이벤트 처리
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 아무것도 선택되지 않았을 때는 모든 항목을 표시
                filteredBookmarks.clear();
                filteredBookmarks.addAll(allBookmarks);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void applyFilter(int filterOption) {
        filteredBookmarks.clear();

        switch (filterOption) {
            case 1: // 자동차 주차장만 보기
                for (Object item : allBookmarks) {
                    if (item instanceof ParkingInfo) {
                        filteredBookmarks.add(item);
                    }
                }
                break;
            case 2: // 자전거 주차장만 보기
                for (Object item : allBookmarks) {
                    if (item instanceof BikeInfo) {
                        filteredBookmarks.add(item);
                    }
                }
                break;
            case 3: // 킥보드 주차장만 보기
                for (Object item : allBookmarks) {
                    if (item instanceof KickboardInfo) {
                        filteredBookmarks.add(item);
                    }
                }
                break;
            default: // 모든 주차장 보기
                filteredBookmarks.addAll(allBookmarks);
                break;
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBookmarkClick(Object item) {
        if (item instanceof ParkingInfo) {
            showParkingInfoDialog((ParkingInfo) item);
        } else if (item instanceof BikeInfo) {
            showBikeInfoDialog((BikeInfo) item);
        } else if (item instanceof KickboardInfo) {
            showKickboardInfoDialog((KickboardInfo) item);
        }
    }

    public void onParkingInfoClick(ParkingInfo parkingInfo){
        showParkingInfoDialog(parkingInfo);
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

    private void showBikeInfoDialog(BikeInfo bikeInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_bike_parking_info, null);
        builder.setView(dialogView);

        TextView bikeParkingNameTextView = dialogView.findViewById(R.id.bike_parking_name);
        TextView bikeParkingAddressTextView = dialogView.findViewById(R.id.bike_parking_address);

        bikeParkingNameTextView.setText(bikeInfo.getStationName());
        bikeParkingAddressTextView.setText(bikeInfo.getAddress());

        builder.setPositiveButton("닫기", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showKickboardInfoDialog(KickboardInfo kickboardInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_kickboard_parking_info, null);
        builder.setView(dialogView);

        TextView kickboardAddressTextView = dialogView.findViewById(R.id.kickboard_parking_address);
        TextView hasStandTextView = dialogView.findViewById(R.id.kickboard_hasStand);

        kickboardAddressTextView.setText(kickboardInfo.getLocation());
        hasStandTextView.setText(kickboardInfo.isHasStand() ? "스탠드 있음" : "스탠드 없음");

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
