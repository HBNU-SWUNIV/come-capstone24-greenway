package com.example.greenway;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

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
        // activity_searchplace.xml 레이아웃을 로드합니다.
        setContentView(R.layout.activity_search);

        // 레이아웃에서 EditText, Button, ListView 요소를 찾습니다.
        searchField = findViewById(R.id.searchField);
        searchButton = findViewById(R.id.searchButton);
        resultsListView = findViewById(R.id.resultsListView);

        // 검색 버튼에 클릭 이벤트 리스너를 추가할 수 있습니다.
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
                    intent = new Intent(SearchPlaceActivity.this, mapStandardList.class);
                } else {
                    return; // 올바르지 않은 호출자 정보 처리
                }

                intent.putExtra("address", place.address_name);
                intent.putExtra("latitude", place.latitude); // 위도
                intent.putExtra("longitude", place.longitude); // 경도
                startActivity(intent);
            }
        });


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
                    PlaceAdapter adapter = new PlaceAdapter(SearchPlaceActivity.this, R.layout.place_item, places);
                    resultsListView.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<KakaoSearchResponse> call, Throwable t) {
                Toast.makeText(SearchPlaceActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


}