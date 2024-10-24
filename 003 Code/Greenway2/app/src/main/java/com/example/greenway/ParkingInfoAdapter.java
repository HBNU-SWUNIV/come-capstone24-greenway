package com.example.greenway;

// ParkingInfoAdapter.java 파일에 위치
import android.content.Context;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

public class ParkingInfoAdapter extends BaseAdapter {
    private List<ParkingInfo> parkingInfoList;
    private Location currentLocation;

    public ParkingInfoAdapter(List<ParkingInfo> parkingInfoList, Location currentLocation) {
        this.parkingInfoList = parkingInfoList;
        this.currentLocation = currentLocation;
    }

    @Override
    public int getCount() {
        return parkingInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return parkingInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        ParkingInfo parkingInfo = parkingInfoList.get(position);

        TextView textView = convertView.findViewById(android.R.id.text1);
        float[] distanceResult = new float[1];
        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                parkingInfo.getLat(), parkingInfo.getLng(), distanceResult);
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

        textView.setText(parkingInfoItem);

        return convertView;
    }
}
