package com.example.greenway;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchPlaceActivity extends AppCompatActivity {

    private EditText searchField;
    private Button searchButton;
    private ListView resultsListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchField = findViewById(R.id.searchField);
        searchButton = findViewById(R.id.searchButton);
        resultsListView = findViewById(R.id.resultsListView);

        loadRecentSearches();

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = searchField.getText().toString();
                if (!query.isEmpty()) {
                    performSearch(query); // 검색어를 전달하여 메소드 호출
                } else {
                    Toast.makeText(SearchPlaceActivity.this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        resultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                KakaoSearchResponse.Place place = (KakaoSearchResponse.Place) parent.getItemAtPosition(position);
                String caller = getIntent().getStringExtra("caller");
                Intent intent;


                if ("MainActivity".equals(caller)) {
                    intent = new Intent(SearchPlaceActivity.this, MainActivity.class);
                } else if ("mapStandardList".equals(caller)) {
                    intent = new Intent(SearchPlaceActivity.this, MapStandardList.class);
                } else {
                    return; // 올바르지 않은 호출자 정보 처리
                }

                intent.putExtra("place_name", place.place_name);
                intent.putExtra("address", place.address_name);
                intent.putExtra("latitude", place.latitude); // 위도
                intent.putExtra("longitude", place.longitude); // 경도

                // 선택한 검색 결과를 저장
                saveRecentSearch(place.place_name, place.address_name, place.latitude, place.longitude);

                startActivity(intent);
            }
        });


    }

    // a. 검색어 저장 메소드
    private void saveRecentSearch(String placeName, String addressName, double latitude, double longitude) {
        SharedPreferences sharedPreferences = getSharedPreferences("recent_searches", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Gson gson = new Gson();

        // 기존 검색 기록 불러오기
        String jsonString = sharedPreferences.getString("searches", "[]");
        List<PlaceRecord> searchesList = gson.fromJson(jsonString, new TypeToken<List<PlaceRecord>>(){}.getType());

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





    // b. 최근 검색어 로드 메소드
    private void loadRecentSearches() {
        SharedPreferences sharedPreferences = getSharedPreferences("recent_searches", MODE_PRIVATE);

        // 기존에 Set<String>으로 저장된 데이터를 처리하는 코드
        try {
            Set<String> oldSearchesSet = sharedPreferences.getStringSet("searches", null);
            if (oldSearchesSet != null) {
                // 기존 데이터 삭제
                sharedPreferences.edit().remove("searches").apply(); // 기존 Set<String> 데이터를 삭제
            }
        } catch (ClassCastException e) {
            // ClassCastException 발생 시 안전하게 처리
            e.printStackTrace();
        }

        // 이후 JSON 형식으로 저장된 데이터를 불러오기
        String jsonString = sharedPreferences.getString("searches", "[]");
        Gson gson = new Gson();
        List<PlaceRecord> recentSearches = gson.fromJson(jsonString, new TypeToken<List<PlaceRecord>>(){}.getType());

        // 어댑터 설정
        String caller = getIntent().getStringExtra("caller");  // "MainActivity" 또는 다른 Activity 명
        RecentSearchAdapter adapter = new RecentSearchAdapter(this, recentSearches, caller);
        resultsListView.setAdapter(adapter);

        // 리스트 항목 클릭 시 동작
        resultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PlaceRecord selectedPlace = recentSearches.get(position);

                // 선택한 장소 정보를 MainActivity로 전달
                Intent intent = new Intent(SearchPlaceActivity.this, MainActivity.class);
                intent.putExtra("place_name", selectedPlace.getPlaceName());
                intent.putExtra("address", selectedPlace.getAddressName());
                intent.putExtra("latitude", selectedPlace.getLatitude());
                intent.putExtra("longitude", selectedPlace.getLongitude());

                startActivity(intent);
            }
        });
    }



    public void onResponse(Call<KakaoSearchResponse> call, Response<KakaoSearchResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
            List<KakaoSearchResponse.Place> places = response.body().documents;
            PlaceAdapter adapter = new PlaceAdapter(SearchPlaceActivity.this, R.layout.place_item, places);
            resultsListView.setAdapter(adapter);

            // 검색이 성공적으로 끝난 후 첫 번째 결과를 저장
            if (!places.isEmpty()) {
                KakaoSearchResponse.Place firstPlace = places.get(0);
                // 'place' 대신 'firstPlace'를 사용하여 검색 기록을 저장
                saveRecentSearch(firstPlace.place_name, firstPlace.address_name, firstPlace.latitude, firstPlace.longitude);
            }
        }
    }


    private void performSearch(String keyword) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://dapi.kakao.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        KakaoAPI kakaoAPI = retrofit.create(KakaoAPI.class);
        Call<KakaoSearchResponse> call = kakaoAPI.searchPlaces("KakaoAK 4d8a3ee46d375be23e0b6279f3fe86f3", keyword);

        call.enqueue(new Callback<KakaoSearchResponse>() {
            @Override
            public void onResponse(Call<KakaoSearchResponse> call, Response<KakaoSearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<KakaoSearchResponse.Place> places = response.body().documents;

                    if (!places.isEmpty()) {
                        KakaoSearchResponse.Place firstPlace = places.get(0);

                        // 검색 결과 저장 시 위도와 경도도 함께 저장
                        saveRecentSearch(firstPlace.place_name, firstPlace.address_name, firstPlace.latitude, firstPlace.longitude);

                        // 검색 결과를 UI에 반영
                        PlaceAdapter adapter = new PlaceAdapter(SearchPlaceActivity.this, R.layout.place_item, places);
                        resultsListView.setAdapter(adapter);
                    }
                }
            }

            @Override
            public void onFailure(Call<KakaoSearchResponse> call, Throwable t) {
                Toast.makeText(SearchPlaceActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


}