package com.example.greenway;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SearchPlaceActivity_Navigation extends AppCompatActivity {

    private EditText searchField;
    private Button searchButton;
    private Button button_my_location;  // 내 위치 버튼
    private ListView resultsListView;
    private LocationManager locationManager;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_navigation);

        searchField = findViewById(R.id.searchField);
        searchButton = findViewById(R.id.searchButton);
        button_my_location = findViewById(R.id.button_my_location); // 내 위치 버튼 초기화
        resultsListView = findViewById(R.id.resultsListView);

        loadRecentSearches();

        // 위치 관리자 초기화
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // 내 위치 버튼 클릭 시 현재 위치 가져오기
        button_my_location.setOnClickListener(v -> {
            // 위치 권한 확인 및 요청
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            } else {
                getCurrentLocation();
            }
        });

        // 검색 버튼 클릭 시 검색 실행
        searchButton.setOnClickListener(v -> {
            String query = searchField.getText().toString();
            if (!query.isEmpty()) {
                performSearch(query); // 검색어를 전달하여 메소드 호출
            } else {
                Toast.makeText(SearchPlaceActivity_Navigation.this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        // 검색 결과 클릭 시 해당 장소 정보를 전달
        resultsListView.setOnItemClickListener((parent, view, position, id) -> {
            KakaoSearchResponse.Place place = (KakaoSearchResponse.Place) parent.getItemAtPosition(position);
            String caller = getIntent().getStringExtra("caller");
            Intent intent;

            if ("NavigationActivity".equals(caller)) {
                intent = new Intent(SearchPlaceActivity_Navigation.this, NavigationActivity.class);
            } else {
                return;
            }

            // 장소 이름과 주소, 위도, 경도를 Intent에 추가
            intent.putExtra("place_name", place.place_name);
            intent.putExtra("address", place.address_name);
            intent.putExtra("latitude", place.latitude);
            intent.putExtra("longitude", place.longitude);

            // 선택한 검색 결과를 저장
            saveRecentSearch(place.place_name, place.address_name, place.latitude, place.longitude);

            setResult(Activity.RESULT_OK, intent);
            finish();
        });
    }

    // 현재 위치를 가져오는 메소드
    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();

                    // 현재 위치 정보를 Intent로 NavigationActivity에 전달
                    Intent intent = new Intent();
                    intent.putExtra("latitude", latitude);
                    intent.putExtra("longitude", longitude);
                    intent.putExtra("place_name", "내 위치");

                    setResult(RESULT_OK, intent);
                    finish(); // Activity 종료 후 결과 전달
                }
            }, null);
        }
    }

    // 위치 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            }
        }
    }

    // 검색 수행 메소드 (카카오 API)
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

                    // 검색이 성공적으로 끝난 후 첫 번째 결과를 저장
                    if (!places.isEmpty()) {
                        KakaoSearchResponse.Place firstPlace = places.get(0);
                        // 'place' 대신 'firstPlace'를 사용하여 검색 기록을 저장
                        saveRecentSearch(firstPlace.place_name, firstPlace.address_name, firstPlace.latitude, firstPlace.longitude);
                        PlaceAdapter adapter = new PlaceAdapter(SearchPlaceActivity_Navigation.this, R.layout.place_item, places);
                        resultsListView.setAdapter(adapter);
                    }
                }
            }

            @Override
            public void onFailure(Call<KakaoSearchResponse> call, Throwable t) {
                Toast.makeText(SearchPlaceActivity_Navigation.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // 최근 검색 저장 메소드
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


    // 최근 검색 로드 메소드
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
        String caller = "NavigationActivity";  // 또는 호출자 정보를 적절히 설정
        RecentSearchAdapter adapter = new RecentSearchAdapter(this, recentSearches, caller);
        resultsListView.setAdapter(adapter);

        // 리스트 항목 클릭 시 동작
        resultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PlaceRecord selectedPlace = recentSearches.get(position);

                // 선택한 장소 정보를 MainActivity로 전달
                Intent intent = new Intent(SearchPlaceActivity_Navigation.this, NavigationActivity.class);
                intent.putExtra("place_name", selectedPlace.getPlaceName());
                intent.putExtra("address", selectedPlace.getAddressName());
                intent.putExtra("latitude", selectedPlace.getLatitude());
                intent.putExtra("longitude", selectedPlace.getLongitude());

                startActivity(intent);
            }
        });
    }


}

