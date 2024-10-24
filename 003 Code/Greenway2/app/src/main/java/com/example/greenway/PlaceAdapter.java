package com.example.greenway;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class PlaceAdapter extends ArrayAdapter<KakaoSearchResponse.Place> {
    private List<KakaoSearchResponse.Place> places;

    public PlaceAdapter(Context context, int resource, List<KakaoSearchResponse.Place> objects) {
        super(context, resource, objects);
        places = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.place_item, parent, false);
        }

        TextView placeName = convertView.findViewById(R.id.placeName);
        TextView placeAddress = convertView.findViewById(R.id.placeAddress);

        KakaoSearchResponse.Place place = getItem(position);
        if (place != null) {
            placeName.setText(place.place_name);
            placeAddress.setText(place.address_name);
        }

        return convertView;
    }
}

