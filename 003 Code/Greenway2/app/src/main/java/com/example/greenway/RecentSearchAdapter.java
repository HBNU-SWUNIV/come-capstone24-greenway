package com.example.greenway;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecentSearchAdapter extends ArrayAdapter<PlaceRecord> {
    private Context context;
    private List<PlaceRecord> recentSearches;
    private String caller;

    // Constructor에 caller를 전달
    public RecentSearchAdapter(Context context, List<PlaceRecord> recentSearches, String caller) {
        super(context, 0, recentSearches);
        this.context = context;
        this.recentSearches = recentSearches;
        this.caller = caller;  // 어느 화면에서 호출했는지 확인하는 정보
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_recent_search, parent, false);
        }

        // 현재 위치의 PlaceRecord 가져오기
        PlaceRecord searchItem = recentSearches.get(position);

        TextView recentSearchTextView = convertView.findViewById(R.id.recentSearchTextView);
        Button deleteButton = convertView.findViewById(R.id.deleteButton);

        // 장소 이름 설정
        recentSearchTextView.setText(searchItem.getPlaceName());

        // convertView 클릭 시
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 검색 기록 저장 메소드 호출
                saveRecentSearch(searchItem.getPlaceName(), searchItem.getAddressName(), searchItem.getLatitude(), searchItem.getLongitude());

                // 적절한 Activity로 이동하는 Intent 생성
                Intent intent;
                if ("MainActivity".equals(caller)) {
                    intent = new Intent(context, MainActivity.class);
                } else if ("mapStandardList".equals(caller)) {
                    intent = new Intent(context, MapStandardList.class);
                } else {
                    return; // 올바르지 않은 호출자 정보 처리
                }

                // 장소 이름과 좌표 정보를 Intent로 전달
                intent.putExtra("place_name", searchItem.getPlaceName());
                intent.putExtra("address", searchItem.getAddressName());
                intent.putExtra("latitude", searchItem.getLatitude());
                intent.putExtra("longitude", searchItem.getLongitude());

                // 새로운 Activity 시작
                context.startActivity(intent);
            }
        });

        // 삭제 버튼 클릭 시 검색 기록 삭제
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeSearchItem(searchItem);
            }
        });

        return convertView;
    }

    // 검색 기록 저장 메소드
    private void saveRecentSearch(String placeName, String addressName, double latitude, double longitude) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("recent_searches", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();

        // 기존 검색 기록 불러오기
        String jsonString = sharedPreferences.getString("searches", "[]");
        List<PlaceRecord> searchesList = gson.fromJson(jsonString, new TypeToken<List<PlaceRecord>>() {}.getType());

        // 중복된 기록 제거
        for (PlaceRecord record : searchesList) {
            if (record.getPlaceName().equals(placeName)) {
                searchesList.remove(record);
                break;
            }
        }

        // 새로운 검색 기록 추가
        searchesList.add(0, new PlaceRecord(placeName, addressName, latitude, longitude));

        // 리스트를 다시 JSON으로 변환하여 저장
        jsonString = gson.toJson(searchesList);
        editor.putString("searches", jsonString);
        editor.apply();
    }

    // 검색 기록 삭제 메소드
    private void removeSearchItem(PlaceRecord searchItem) {
        recentSearches.remove(searchItem);
        notifyDataSetChanged();

        // SharedPreferences에서도 삭제
        SharedPreferences sharedPreferences = context.getSharedPreferences("recent_searches", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String jsonString = gson.toJson(recentSearches);
        editor.putString("searches", jsonString);
        editor.apply();
    }
}
