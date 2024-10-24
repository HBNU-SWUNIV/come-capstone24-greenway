package com.example.greenway;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface KakaoAPI {
    @GET("v2/local/search/keyword.json")
    Call<KakaoSearchResponse> searchPlaces(
            @Header("Authorization") String key,
            @Query("query") String query
    );
}
