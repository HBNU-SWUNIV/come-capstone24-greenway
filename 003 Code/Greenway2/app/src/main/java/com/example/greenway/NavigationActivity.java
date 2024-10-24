package com.example.greenway;

import android.Manifest;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

public class NavigationActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int REQUEST_CODE_START = 1;
    private static final int REQUEST_CODE_END = 2;

    private static final String[] PERMISSIONS = {
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private EditText editText_start;
    private TextView textView_arrival_time;
    private EditText editText_end;
    private LinearLayout routeInfoLayout;
    private TextView textView_distance;
    private TextView textView_duration;
    private TextView parkingNameTextView;
    private TextView congestionTextView;
    private Button button_start_navigation;
    private FragmentManager fm;
    private NaverMap naverMap;

    private LatLng startLatLng;
    private LatLng endLatLng;

    private Marker startMarker;
    private Marker endMarker;
    private PathOverlay pathOverlay;
    private List<LatLng> pathCoords;
    private ImageButton button_swap;

    // ONNX Runtime 관련 변수
    private OrtEnvironment env;
    private OrtSession session;
    private Map<String, Integer> parkingNameEncoding = new HashMap<>();
    private Map<String, Integer> weekdayEncoding = new HashMap<>();
    private float[] minValues;
    private float[] maxValues;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        editText_start = findViewById(R.id.editText_start);
        editText_end = findViewById(R.id.editText_end);
        routeInfoLayout = findViewById(R.id.route_info_layout);
        textView_distance = findViewById(R.id.textView_distance);
        textView_duration = findViewById(R.id.textView_duration);
        textView_arrival_time = findViewById(R.id.textView_arrival_time);
        button_start_navigation = findViewById(R.id.button_start_navigation);
        button_swap = findViewById(R.id.button_swap);
        parkingNameTextView = findViewById(R.id.parkingNameTextView);
        ImageButton buttonRefresh = findViewById(R.id.button_refresh);
        congestionTextView = findViewById(R.id.congestionTextView);

        // JSON 파일에서 OneHotEncoder와 MinMaxScaler 정보 로드
        try {
            loadEncoderAndScalerInfo();
        } catch (Exception e) {
            congestionTextView.setText("인코더/스케일러 로드 중 오류 발생: " + e.getMessage());
            return;
        }

        // ONNX 모델 로드
        try {
            loadOnnxModel();
        } catch (Exception e) {
            congestionTextView.setText("모델 로드 중 오류 발생: " + e.getMessage());
            return;
        }

        // 입력 데이터 준비 (예시)
        String parkingName_Predict = parkingNameTextView.getText().toString();
        String arrivalTime = textView_arrival_time.getText().toString(); // 도착 예정 시간: 19시 34분 형태
        String weekday = getCurrentWeekday();  // 요일 불러오기 함수
        int[] time = extractTime(arrivalTime);

        // 원핫 인코딩 및 정규화 적용
        float[] inputData = prepareInputData(parkingName_Predict, weekday, time[0], time[1]);

        // 혼잡도 예측
        try {
            String congestionLevel = predictCongestion(inputData);
            congestionTextView.setText("예상 주차장 혼잡도: " + congestionLevel);
        } catch (Exception e) {
            congestionTextView.setText("예측 중 오류 발생: " + e.getMessage());
        }


        buttonRefresh.setOnClickListener(v -> {
            if (startLatLng != null && endLatLng != null) {
                // 새로고침 토스트 메시지 표시
                Toast.makeText(NavigationActivity.this, "새로고침합니다.", Toast.LENGTH_SHORT).show();

                // 경로 다시 계산
                calculateRouteLatLng(startLatLng, endLatLng);
            } else {
                Toast.makeText(NavigationActivity.this, "출발지와 도착지를 먼저 설정하세요.", Toast.LENGTH_SHORT).show();
            }
        });



        routeInfoLayout.setVisibility(View.GONE);

        fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);

        // 출발지, 도착지 설정 시 경로 자동 계산
        editText_start.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String startText = editText_start.getText().toString();
                if (!startText.isEmpty()) {
                    addMarkerToMap(startText, true);
                }
            }
        });

        editText_end.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String endText = editText_end.getText().toString();
                if (!endText.isEmpty()) {
                    addMarkerToMap(endText, false);
                }
            }
        });

        // editText_end 텍스트 변경 시 parkingNameTextView 업데이트
        editText_end.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 텍스트 변경 전 처리 (필요 없다면 생략 가능)
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 텍스트가 변경될 때 parkingNameTextView에 반영
                parkingNameTextView.setText(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 텍스트 변경 후 처리 (필요 없다면 생략 가능)
            }
        });

        // 수신된 인텐트 처리
        Intent receivedIntent = getIntent();
        if (receivedIntent != null && receivedIntent.hasExtra("car_parking_name")) {
            String carParkingName = receivedIntent.getStringExtra("car_parking_name");
            boolean isStart = receivedIntent.getBooleanExtra("isStart", true);
            double lat = receivedIntent.getDoubleExtra("lat", 0);
            double lng = receivedIntent.getDoubleExtra("lng", 0);

            LatLng position = new LatLng(lat, lng);

            if (isStart) {
                editText_start.setText(carParkingName);
                startLatLng = position;
            } else {
                editText_end.setText(carParkingName);
                endLatLng = position;


            }
        }

        editText_start.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Intent intent = new Intent(NavigationActivity.this, SearchPlaceActivity_Navigation.class);
                    intent.putExtra("caller", "NavigationActivity");
                    startActivityForResult(intent, REQUEST_CODE_START);
                    return true;
                }
                return false;
            }
        });

        editText_end.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    Intent intent = new Intent(NavigationActivity.this, SearchPlaceActivity_Navigation.class);
                    intent.putExtra("caller", "NavigationActivity");
                    startActivityForResult(intent, REQUEST_CODE_END);
                    return true;
                }
                return false;
            }
        });

        // 출발지와 도착지 교환
        button_swap.setOnClickListener(v -> swapStartAndEnd());

        button_start_navigation.setOnClickListener(v -> {
            if (startLatLng != null && endLatLng != null) {
                Intent intent = new Intent(NavigationActivity.this, NavigationGuideActivity.class);

                // 경로 좌표 리스트 전달 (배열 형태로)
                ArrayList<double[]> pathList = new ArrayList<>();
                for (LatLng coord : pathCoords) {
                    pathList.add(new double[]{coord.latitude, coord.longitude});
                }
                intent.putExtra("pathCoords", pathList);

                // 출발지, 도착지 정보도 전달
                intent.putExtra("start_lat", startLatLng.latitude);
                intent.putExtra("start_lng", startLatLng.longitude);
                intent.putExtra("end_lat", endLatLng.latitude);
                intent.putExtra("end_lng", endLatLng.longitude);

                // 도착 예정 시간 텍스트 전달
                String arrivalTimeText = textView_arrival_time.getText().toString();
                intent.putExtra("arrival_time_text", arrivalTimeText);

                // NavigationGuideActivity로 이동
                startActivity(intent);
            } else {
                Toast.makeText(NavigationActivity.this, "경로를 먼저 계산하세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;

        // NaverMap이 준비된 후에 마커를 추가
        if (startLatLng != null) {
            addMarker(startLatLng, "출발", true);
        }
        if (endLatLng != null) {
            addMarker(endLatLng, "도착", false);
        }

        if (startLatLng != null && endLatLng != null) {
            calculateRouteLatLng(startLatLng, endLatLng);
        }
    }

    private void calculateRouteLatLng(LatLng startLatLng, LatLng endLatLng) {
        this.startLatLng = startLatLng;
        this.endLatLng = endLatLng;

        if (startLatLng != null && endLatLng != null) {
            new CalculateRouteTask().execute();
        }
    }

    private void calculateRoute(String start, String end) {
        new GeocodingTask(true).execute(start);
        new GeocodingTask(false).execute(end);
    }

    private void addMarker(LatLng position, String caption, boolean isStart) {
        Marker marker = new Marker();
        marker.setPosition(position);
        marker.setCaptionText(caption);
        if (isStart) {
            if (startMarker != null) {
                startMarker.setMap(null); // 기존 마커 제거
            }
            startMarker = marker;
            marker.setIcon(OverlayImage.fromResource(R.drawable.ic_start_marker)); // 출발지 아이콘
        } else {
            if (endMarker != null) {
                endMarker.setMap(null); // 기존 마커 제거
            }
            endMarker = marker;
            marker.setIcon(OverlayImage.fromResource(R.drawable.ic_end_marker)); // 도착지 아이콘
        }
        marker.setMap(naverMap);

        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(position).animate(CameraAnimation.Easing);
        naverMap.moveCamera(cameraUpdate);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            String placeName = data.getStringExtra("place_name");
            String address = data.getStringExtra("address");

            if (requestCode == REQUEST_CODE_START) {
                editText_start.setText(placeName);
                addMarkerToMap(address, true);
            } else if (requestCode == REQUEST_CODE_END) {
                editText_end.setText(placeName);
                addMarkerToMap(address, false);

                // parkingNameTextView에 도착지 장소 이름 설정
                parkingNameTextView.setText(placeName);
            }
        }
    }

    private void addMarkerToMap(String address, boolean isStart) {
        new GeocodingTask(isStart).execute(address);
    }

    private class GeocodingTask extends AsyncTask<String, Void, LatLng> {
        private boolean isStart;

        public GeocodingTask(boolean isStart) {
            this.isStart = isStart;
        }

        @Override
        protected LatLng doInBackground(String... params) {
            String address = params[0];
            String clientId = "g7qv4saqzt";
            String clientSecret = "1NlHYYs648uGmCnCa5RX664Z7RKQkhi8G26jEBVP";
            String urlString = "https://naveropenapi.apigw.ntruss.com/map-geocode/v2/geocode?query=" + address;

            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", clientId);
                connection.setRequestProperty("X-NCP-APIGW-API-KEY", clientSecret);
                connection.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder jsonResult = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonResult.append(line);
                }
                reader.close();

                JSONObject jsonObject = new JSONObject(jsonResult.toString());
                JSONArray addresses = jsonObject.getJSONArray("addresses");
                if (addresses.length() > 0) {
                    JSONObject location = addresses.getJSONObject(0);
                    double lat = location.getDouble("y");
                    double lng = location.getDouble("x");
                    return new LatLng(lat, lng);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(LatLng latLng) {
            if (latLng != null) {
                if (isStart) {
                    startLatLng = latLng;
                    addMarker(latLng, "출발", true);
                } else {
                    endLatLng = latLng;
                    addMarker(latLng, "도착", false);
                }

                if (startLatLng != null && endLatLng != null) {
                    new CalculateRouteTask().execute();
                }
            } else {
                Toast.makeText(NavigationActivity.this, "주소로부터 좌표를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class CalculateRouteTask extends AsyncTask<Void, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(Void... voids) {
            String clientId = "g7qv4saqzt";
            String clientSecret = "1NlHYYs648uGmCnCa5RX664Z7RKQkhi8G26jEBVP";
            String urlString = "https://naveropenapi.apigw.ntruss.com/map-direction/v1/driving?" +
                    "start=" + startLatLng.longitude + "," + startLatLng.latitude +
                    "&goal=" + endLatLng.longitude + "," + endLatLng.latitude +
                    "&option=trafast";

            Log.d("CalculateRouteTask", "Request URL: " + urlString);

            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("X-NCP-APIGW-API-KEY-ID", clientId);
                connection.setRequestProperty("X-NCP-APIGW-API-KEY", clientSecret);
                connection.connect();

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder jsonResult = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonResult.append(line);
                }
                reader.close();

                Log.d("CalculateRouteTask", "Response: " + jsonResult.toString());

                return new JSONObject(jsonResult.toString());
            } catch (Exception e) {
                Log.e("CalculateRouteTask", "Error in API request", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null) {
                try {
                    if (result.has("route") && !result.isNull("route")) {
                        JSONObject route = result.getJSONObject("route");
                        if (route.has("trafast") && !route.isNull("trafast")) {
                            JSONArray routes = route.getJSONArray("trafast");
                            if (routes.length() > 0) {
                                JSONObject traFastRoute = routes.getJSONObject(0);
                                JSONObject summary = traFastRoute.getJSONObject("summary");

                                // 거리 계산 (미터 단위)
                                double distance = summary.getDouble("distance");
                                textView_distance.setText(String.format("거리: %.1f km", distance / 1000));

                                // 소요 시간 계산 (밀리초 단위)
                                double duration = summary.getDouble("duration");
                                int totalMinutes = (int) (duration / 1000 / 60); // 소요 시간 (분 단위)
                                int hours = totalMinutes / 60;
                                int minutes = totalMinutes % 60;

                                if (hours > 0) {
                                    textView_duration.setText(String.format("소요시간: %d시간 %d분", hours, minutes));
                                } else {
                                    textView_duration.setText(String.format("소요시간: %d분", minutes));
                                }

                                // 도착 예정 시간 계산
                                long currentTimeMillis = System.currentTimeMillis();  // 현재 시간
                                long arrivalTimeMillis = currentTimeMillis + (long) duration;  // 도착 시간
                                Calendar arrivalCalendar = Calendar.getInstance();
                                arrivalCalendar.setTimeInMillis(arrivalTimeMillis);

                                int arrivalHour = arrivalCalendar.get(Calendar.HOUR_OF_DAY); // 24시간 형식의 시간
                                int arrivalMinute = arrivalCalendar.get(Calendar.MINUTE);

                                // 도착 예정 시간 텍스트 ("몇 시 몇 분" 형식)
                                String arrivalTimeText = String.format("도착 예정 시간: %d시 %d분", arrivalHour, arrivalMinute);
                                textView_arrival_time.setText(arrivalTimeText);

                                // routeInfoLayout 표시
                                routeInfoLayout.setVisibility(View.VISIBLE);

                                // 경로 좌표 설정 (생략 가능)
                                if (pathOverlay != null) {
                                    pathOverlay.setMap(null);  // 기존 경로 제거
                                }

                                JSONArray path = traFastRoute.getJSONArray("path");
                                pathCoords = new ArrayList<>();
                                for (int i = 0; i < path.length(); i++) {
                                    JSONArray point = path.getJSONArray(i);
                                    pathCoords.add(new LatLng(point.getDouble(1), point.getDouble(0)));
                                }

                                pathOverlay = new PathOverlay();
                                pathOverlay.setCoords(pathCoords);
                                pathOverlay.setMap(naverMap);
                            } else {
                                Toast.makeText(NavigationActivity.this, "경로 정보가 없습니다.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(NavigationActivity.this, "경로 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(NavigationActivity.this, "경로 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(NavigationActivity.this, "JSON 파싱 오류.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(NavigationActivity.this, "서버 응답이 없거나 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        }


    }
    private void swapStartAndEnd() {
        // 텍스트 교환
        String startText = editText_start.getText().toString();
        String endText = editText_end.getText().toString();
        editText_start.setText(endText);
        editText_end.setText(startText);

        // 위치 교환
        LatLng tempLatLng = startLatLng;
        startLatLng = endLatLng;
        endLatLng = tempLatLng;

        // 마커 위치 업데이트
        if (startLatLng != null) {
            addMarker(startLatLng, "출발", true);
        }
        if (endLatLng != null) {
            addMarker(endLatLng, "도착", false);
        }

        // 경로 다시 계산
        if (startLatLng != null && endLatLng != null) {
            calculateRouteLatLng(startLatLng, endLatLng);
        }
    }

    // ONNX 모델을 로드하는 함수
    private void loadOnnxModel() throws OrtException, IOException {
        env = OrtEnvironment.getEnvironment();  // ONNX 환경 생성

        // ONNX 모델을 assets 폴더에서 로드
        try (InputStream modelInputStream = getAssets().open("xgboost_model_with_onehot.onnx")) {
            byte[] modelBytes = new byte[modelInputStream.available()];
            modelInputStream.read(modelBytes);

            session = env.createSession(modelBytes);  // ONNX 세션 생성
        }
    }

    // JSON 파일에서 OneHotEncoder와 MinMaxScaler 정보 로드하는 함수
    private void loadEncoderAndScalerInfo() throws IOException {
        try {
            // OneHotEncoder 정보 로드
            String encoderJson = new Scanner(getAssets().open("onehot_encoder_info.json")).useDelimiter("\\A").next();
            JSONObject encoderObj = new JSONObject(encoderJson);
            JSONArray parkingNames = encoderObj.getJSONArray("categories").getJSONArray(0);
            JSONArray weekdays = encoderObj.getJSONArray("categories").getJSONArray(1);

            for (int i = 0; i < parkingNames.length(); i++) {
                parkingNameEncoding.put(parkingNames.getString(i), i);
            }

            for (int i = 0; i < weekdays.length(); i++) {
                weekdayEncoding.put(weekdays.getString(i), i);
            }

            // MinMaxScaler 정보 로드
            String scalerJson = new Scanner(getAssets().open("scaler_info.json")).useDelimiter("\\A").next();
            JSONObject scalerObj = new JSONObject(scalerJson);
            JSONArray minArray = scalerObj.getJSONArray("min");
            JSONArray maxArray = scalerObj.getJSONArray("max");

            minValues = new float[minArray.length()];
            maxValues = new float[maxArray.length()];

            for (int i = 0; i < minArray.length(); i++) {
                minValues[i] = (float) minArray.getDouble(i);
                maxValues[i] = (float) maxArray.getDouble(i);
            }

        } catch (JSONException e) {
            // JSONException이 발생하면 이를 처리
            e.printStackTrace();
            throw new IOException("JSON 데이터 파싱 중 오류 발생: " + e.getMessage());
        }
    }

    // 입력 데이터를 준비하는 함수 (모델이 기대하는 4개의 입력 특성)
    private float[] prepareInputData(String parkingName_Predict, String weekday, int hour, int minute) {
        // 원핫 인코딩 전 입력값을 출력
        Log.d("Debug", "원핫 인코딩 전 입력값 - 주차장명: " + parkingName_Predict + ", 요일: " + weekday + ", 시간: " + hour + ", 분: " + minute);

        // MinMaxScaler 값 확인
        Log.d("Debug", "MinMaxScaler 값 - 시간(min): " + minValues[0] + ", 시간(max): " + maxValues[0] + ", 분(min): " + minValues[1] + ", 분(max): " + maxValues[1]);

        // 원핫 인코딩 적용
        float[] inputData = new float[parkingNameEncoding.size() + weekdayEncoding.size() + 2];  // 주차장명 + 요일 + 시간/분

        // 주차장명 인코딩
        int parkingIdx = parkingNameEncoding.getOrDefault(parkingName_Predict, -1);
        if (parkingIdx != -1) {
            inputData[parkingIdx] = 1.0f;
        }

        // 요일 인코딩
        int weekdayIdx = weekdayEncoding.getOrDefault(weekday, -1);
        if (weekdayIdx != -1) {
            inputData[parkingNameEncoding.size() + weekdayIdx] = 1.0f;
        }

        // 시간 정규화 전 출력
        Log.d("Debug", "정규화 전 시간 값 - 시간: " + hour + ", 분: " + minute);

        // 시간과 분 정규화
        inputData[inputData.length - 2] = normalize(hour, minValues[0], maxValues[0]);
        inputData[inputData.length - 1] = normalize(minute, minValues[1], maxValues[1]);

        // 정규화 후 시간 값을 출력
        Log.d("Debug", "정규화 후 시간 값 - 시간: " + inputData[inputData.length - 2] + ", 분: " + inputData[inputData.length - 1]);

        // 원핫 인코딩 후 입력값을 출력
        StringBuilder encodedData = new StringBuilder("원핫 인코딩 후 입력값: ");
        for (float val : inputData) {
            encodedData.append(val).append(", ");
        }
        Log.d("Debug", encodedData.toString());

        return inputData;
    }

    // 정규화 함수
    private float normalize(int value, float min, float max) {
        return (value - min) / (max - min);
    }

    // ONNX 모델을 사용해 혼잡도를 예측하는 함수
    private String predictCongestion(float[] inputData) throws OrtException {
        float[][] reshapedInput = new float[1][inputData.length];  // 2D 배열로 변환
        reshapedInput[0] = inputData;

        OnnxTensor inputTensor = OnnxTensor.createTensor(env, reshapedInput);

        // 세션 실행
        OrtSession.Result result = session.run(Collections.singletonMap("float_input", inputTensor));

        long[] output = (long[]) result.get(0).getValue();
        int predictedClass = Math.round(output[0]);

        Map<Integer, String> congestionMapping = new HashMap<>();
        congestionMapping.put(0, "여유");
        congestionMapping.put(1, "보통");
        congestionMapping.put(2, "혼잡");

        return congestionMapping.getOrDefault(predictedClass, "알 수 없음");
    }

    // 도착 시간에서 시간과 분을 추출하는 함수
    private int[] extractTime(String arrivalTime) {
        try {
            String[] parts = arrivalTime.split(" ");
            String[] h = parts[3].split("시");
            String[] m = parts[4].split("분");

            int hour = Integer.parseInt(h[0].trim());
            int minute = Integer.parseInt(m[0].trim());
            return new int[]{hour, minute};
        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }

    // 현재 요일을 가져오는 함수
    private String getCurrentWeekday() {
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return "Sunday";
            case Calendar.MONDAY:
                return "Monday";
            case Calendar.TUESDAY:
                return "Tuesday";
            case Calendar.WEDNESDAY:
                return "Wednesday";
            case Calendar.THURSDAY:
                return "Thursday";
            case Calendar.FRIDAY:
                return "Friday";
            case Calendar.SATURDAY:
                return "Saturday";
            default:
                return "Unknown";
        }
    }


}

