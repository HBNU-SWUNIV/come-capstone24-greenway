package com.example.greenway;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Bookmarker {
    private static final String PREFS_NAME = "bookmarks";
    private static final String BOOKMARKS_KEY = "bookmarks";
    private static final String KEY_PARKING_BOOKMARKS = "parking_bookmark_list";
    private static final String KEY_BIKE_BOOKMARKS = "bike_bookmark_list";
    private static final String KEY_KICKBOARD_BOOKMARKS = "kickboard_bookmark_list";
    private static List<ParkingInfo> bookmarkList = new ArrayList<>();

    // ParkingInfo 관련 메서드 (기존 유지)
    public static void addParking(Context context, ParkingInfo parkingInfo) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        List<ParkingInfo> bookmarkList = getParkingBookmarkList(context);
        bookmarkList.add(parkingInfo);
        String json = gson.toJson(bookmarkList);
        editor.putString(KEY_PARKING_BOOKMARKS, json);
        editor.apply();
    }

    public static void removeParking(Context context, ParkingInfo parkingInfo) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        List<ParkingInfo> bookmarkList = getParkingBookmarkList(context);
        bookmarkList.remove(parkingInfo);
        String json = gson.toJson(bookmarkList);
        editor.putString(KEY_PARKING_BOOKMARKS, json);
        editor.apply();
    }

    public static boolean isParkingBookmarked(Context context, ParkingInfo parkingInfo) {
        List<ParkingInfo> bookmarkList = getParkingBookmarkList(context);
        return bookmarkList.contains(parkingInfo);
    }

    public static List<ParkingInfo> getParkingBookmarkList(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(KEY_PARKING_BOOKMARKS, null);
        Type type = new TypeToken<List<ParkingInfo>>() {}.getType();
        List<ParkingInfo> bookmarkList = gson.fromJson(json, type);
        return bookmarkList != null ? bookmarkList : new ArrayList<>();
    }

    // BikeInfo 관련 메서드 추가
    public static void addBike(Context context, BikeInfo bikeInfo) {
        List<BikeInfo> bookmarkList = getBikeBookmarkList(context);
        if (!bookmarkList.contains(bikeInfo)) {
            bookmarkList.add(bikeInfo);
            saveBikeBookmarkList(context, bookmarkList);
        }
    }

    public static void removeBike(Context context, BikeInfo bikeInfo) {
        List<BikeInfo> bookmarkList = getBikeBookmarkList(context);
        bookmarkList.remove(bikeInfo);
        saveBikeBookmarkList(context, bookmarkList);
    }

    public static boolean isBikeBookmarked(Context context, BikeInfo bikeInfo) {
        List<BikeInfo> bookmarkList = getBikeBookmarkList(context);
        return bookmarkList.contains(bikeInfo);
    }

    private static void saveBikeBookmarkList(Context context, List<BikeInfo> bookmarkList) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(bookmarkList);
        editor.putString(KEY_BIKE_BOOKMARKS, json);
        editor.apply();
    }

    public static List<BikeInfo> getBikeBookmarkList(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(KEY_BIKE_BOOKMARKS, null);
        Type type = new TypeToken<List<BikeInfo>>() {}.getType();
        List<BikeInfo> bookmarkList = gson.fromJson(json, type);
        return bookmarkList != null ? bookmarkList : new ArrayList<>();
    }

    // KickboardInfo 관련 메서드 추가
    public static void addKickboard(Context context, KickboardInfo kickboardInfo) {
        List<KickboardInfo> bookmarkList = getKickboardBookmarkList(context);
        if (!bookmarkList.contains(kickboardInfo)) {
            bookmarkList.add(kickboardInfo);
            saveKickboardBookmarkList(context, bookmarkList);
        }
    }

    public static void removeKickboard(Context context, KickboardInfo kickboardInfo) {
        List<KickboardInfo> bookmarkList = getKickboardBookmarkList(context);
        bookmarkList.remove(kickboardInfo);
        saveKickboardBookmarkList(context, bookmarkList);
    }

    public static boolean isKickboardBookmarked(Context context, KickboardInfo kickboardInfo) {
        List<KickboardInfo> bookmarkList = getKickboardBookmarkList(context);
        return bookmarkList.contains(kickboardInfo);
    }

    private static void saveKickboardBookmarkList(Context context, List<KickboardInfo> bookmarkList) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(bookmarkList);
        editor.putString(KEY_KICKBOARD_BOOKMARKS, json);
        editor.apply();
    }

    public static List<KickboardInfo> getKickboardBookmarkList(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString(KEY_KICKBOARD_BOOKMARKS, null);
        Type type = new TypeToken<List<KickboardInfo>>() {}.getType();
        List<KickboardInfo> bookmarkList = gson.fromJson(json, type);
        return bookmarkList != null ? bookmarkList : new ArrayList<>();
    }

    public static void initialize(Context context) {
        SharedPreferences userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String loggedInUser = userPrefs.getString("loggedInUser", null);

        if (loggedInUser != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Gson gson = new Gson();
            String json = prefs.getString(BOOKMARKS_KEY + "_" + loggedInUser, null);  // 사용자별로 데이터 불러오기
            Type type = new TypeToken<ArrayList<ParkingInfo>>() {}.getType();
            if (json != null) {
                bookmarkList = gson.fromJson(json, type);
            }
            Log.d("Bookmarker", "Initialized with " + bookmarkList.size() + " items for user: " + loggedInUser);
        }
    }

    // 즐겨찾기 목록을 저장/불러올 때 각 사용자의 username에 맞는 데이터를 불러오도록 수정
    private static void saveBookmarks(Context context) {
        SharedPreferences userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE);
        String loggedInUser = userPrefs.getString("loggedInUser", null);

        if (loggedInUser != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            Gson gson = new Gson();
            String json = gson.toJson(bookmarkList);
            editor.putString(BOOKMARKS_KEY + "_" + loggedInUser, json);  // 사용자별로 데이터 저장
            editor.apply();
            Log.d("Bookmarker", "Bookmarks saved for user: " + loggedInUser);
        }
    }

    public static List<ParkingInfo> getBookmarks() {
        return new ArrayList<>(bookmarkList);
    }

}
