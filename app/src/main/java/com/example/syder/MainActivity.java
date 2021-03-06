package com.example.syder;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.syder.databinding.ActivityMainBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
    private ActivityMainBinding binding;
    private static final String TAG =  "activity_main";
    private GoogleMap           mMap;
    private SupportMapFragment  mapFragment;
    private RequestQueue mQueue;
    private DrawerLayout drawerLayout;
    private View drawerView;
    private RequestQueue requestQueue;
    private TextView startPoint;
    private TextView endPoint;
    static ArrayList<RouteModel> routeList = new ArrayList<RouteModel>();
    private ArrayList<MarkerModel> markersInfo = new ArrayList<MarkerModel>();
    private ArrayList<MarkerModel> list = new ArrayList<MarkerModel>();
    static boolean moveNeed;
    static String moveNeedRoute = "0";
    static String startingId;
    static String arrivalId;
    private boolean setVisibilityInfo;
    static int travelTime;
    static int selectedCount = 0;
    static String cartId;
    static String[] selectedTitle = new String[2];
    private ArrayList<CarMarkerModel> carMarkerInfoList = new ArrayList<CarMarkerModel>();
    private ArrayList<CarMarkerModel> carMarkerData     = new ArrayList<CarMarkerModel>();
    private Socket socket;
    private String Socket_URL_USER = "http://13.124.124.67:80/user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        requestQueue =  Volley.newRequestQueue(this);
        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        drawerView = (View) findViewById(R.id.drawer);
        startPoint = findViewById(R.id.startPoint);
        endPoint = findViewById(R.id.endPoint);
        ImageView menu_open = (ImageView)findViewById(R.id.menu_open);


        //Socket Connect
        connectServer();
        menu_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(drawerView);
            }
        });


        mQueue = Volley.newRequestQueue(this);

        drawerLayout.setDrawerListener(listener);
        drawerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        Button menu_close = (Button)findViewById(R.id.menu_close);
        menu_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.closeDrawers();
            }
        });

        Button logout = (Button) findViewById(R.id.button_logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
                Log.i(TAG,"토큰 값" + ActivityLogin.loginResponse);
                ActivityLogin.loginResponse = null;
            }
        });

        binding.send.setOnClickListener(v -> authCheck());

        mapFragment = (SupportMapFragment)getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        socket.close();
        super.onPause();
        finish();
    }

    private void connectServer(){
        try {
            socket = IO.socket(Socket_URL_USER);
            socket.connect();
            socketConnect();
            Log.i(TAG, "지금 연결함!!");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
    // 초기 차량 위치 조회
    public Emitter.Listener cars_info = args ->runOnUiThread(()->{
        JSONArray data = (JSONArray) args[0];
        Log.d(TAG,"초기 차량 위치  : " + data);
    });

    // 차량 위치 변화 받기
    public Emitter.Listener car_location = args ->runOnUiThread(()->{
        JSONArray data = (JSONArray) args[0];
        Log.d(TAG,"차량 위치 변화 받기 : " + data);

        carUpdate();

        Log.d(TAG,carMarkerInfoList.size() + "");

        try {
            JSONObject jsonObject;
            for(int i = 0; i < data.length(); i++){
                jsonObject     = data.getJSONObject(i);
                String carNumber              = jsonObject.getString("carNumber");
                int car_status                = jsonObject.getInt("status");
                double car_lat                = jsonObject.getDouble("lat");
                double car_lng                = jsonObject.getDouble("lng");
                int car_battery               = jsonObject.getInt("battery");
                Log.d(TAG,carNumber +" : " +car_status + " : " + car_lat+ " : "  + car_lng+ " : "  + car_battery);

                carMarkerInfoList.add(new CarMarkerModel(carNumber, car_status, car_lat, car_lng, car_battery));

                Log.d(TAG,carMarkerInfoList.get(i).getCarNumber() + "");
            }

            for(CarMarkerModel carMarkerModel : carMarkerInfoList){
                addCarMarker(carMarkerModel);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    });

    public void carUpdate(){

        for(int iCount = 0; iCount < carMarkerData.size() ; iCount++){
            carMarkerData.get(iCount).getCarMarker().remove();
            Log.d(TAG,carMarkerData.get(iCount).getCarMarker().toString());
        }
        carMarkerInfoList.removeAll(carMarkerInfoList);
        carMarkerData.removeAll(carMarkerData);
        Log.d(TAG,"리스트 값 확인 : "+carMarkerData.size() + carMarkerInfoList.size() + "");
    }

    public  void addCarMarker(CarMarkerModel carMarkerModel){
        LatLng position = new LatLng(carMarkerModel.getCar_lat(), carMarkerModel.getCar_lng());
        String carNumber = carMarkerModel.getCarNumber();
        int carStatus = carMarkerModel.getCar_status();
        int battery = carMarkerModel.getCar_battery();

        Log.d(TAG, "postion : " + position + " carNumber : " + carNumber + "carStatus : " + carStatus + " battery : " + battery);
        //크기를 지정해서 비트맵으로 만들기 자동차
        Bitmap car = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.driving),
                80, 80, false);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title(carNumber + "호차")
                .snippet(battery + "%")
                .icon(BitmapDescriptorFactory.fromBitmap(car))
                .position(position);

        Log.d(TAG, "위치 : " + position + "차량 호수 : "+carNumber + "차량 상태 : " +carStatus + "배터리 : " + battery);

        carMarkerData.add(new CarMarkerModel(mMap.addMarker(markerOptions), carNumber));

    }

    // 서버와 소켓 연결 성공 시 리스너
    private Emitter.Listener onConnect = args ->runOnUiThread(()->{
        Log.d(TAG,"Connect message : 서버와 연결이 성공하였습니다.");
        Toast.makeText(getApplicationContext(),"서버와 연결이 성공하였습니다.",Toast.LENGTH_SHORT).show();
    });

    // 서버연결이 실패 했을 때 리스너
    private Emitter.Listener onConnectError = args ->runOnUiThread(()->{
        Log.e(TAG,"Error message : 서버와 연결이 실패됬습니다.");
        Toast.makeText(getApplicationContext(),"서버와 연결이 실패됬습니다.",Toast.LENGTH_SHORT).show();
    });

    private void socketConnect(){
        socket.on(Socket.EVENT_CONNECT,onConnect);
        socket.on(Socket.EVENT_CONNECT_ERROR,onConnectError);
        socket.on("locationRequest",cars_info);
        socket.on("car_updateLocation",car_location);
    }

    public void authCheck() {
        //ActivityLogin.orderId
        String url = "http://13.124.189.186/api/authCheck?guard=user";
        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            public void onResponse(String response) {
                int authId = 0;
                Log.d(TAG,"auth 체크" + response);
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    authId = jsonResponse.getInt("id");
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "auth 체크 에러 하이고" );
                }
                if(authId == ActivityLogin.userId) {
                    Intent intent = new Intent(MainActivity.this, ActivitySend.class);
                    startActivity(intent);
                }
            }
        },new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG,"auth 체크 에러 -> " + error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError{
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization" , "Bearer " + ActivityLogin.loginResponse);
                return params;
            }
        };

        request.setShouldCache(false);
        requestQueue.add(request);
        Log.d(TAG, "auth 체크 요청보냄");
    }
    public void logout(){
        String url = "http://13.124.189.186/api/logout";

        StringRequest logoutRequest = new StringRequest(Request.Method.POST, url,
                response->{
                    Log.i(TAG, response);
                    Toast.makeText(getApplicationContext(), "로그아웃 받아옴", Toast.LENGTH_SHORT).show();
                },
                error -> Log.i(TAG, error.getMessage())
        ) {
            protected Map<String, String> getParams() throws AuthFailureError{
                Map<String, String> params = new HashMap<String, String>();
                params.put("guard" , "user");
                return params;
            }

            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization", "Bearer " + ActivityLogin.loginResponse);

                return params;
            }

        };

        logoutRequest.setShouldCache(false);
        Log.i(TAG,"request" + logoutRequest);
        requestQueue.add(logoutRequest);
        Log.i(TAG,"요청 보냄.");

        Log.i(TAG,"토큰 값" + ActivityLogin.loginResponse);
        ActivityLogin.loginResponse = null;

        Intent intent = new Intent(MainActivity.this, ActivityLogin.class);
        startActivity(intent);
    }

    public void routeTimeChecking() {
        for(int i = 0; i < routeList.size(); i++) {
            // 정방향 0
            if (startingId.equals(routeList.get(i).getStartingId()) &&
                    arrivalId.equals(routeList.get(i).getArrivalPoint())) {
                travelTime = routeList.get(i).getTravelTime();
                break;
            }
            // 역방향
            else if(startingId.equals(routeList.get(i).getArrivalPoint()) &&
                    arrivalId.equals(routeList.get(i).getStartingId())) {
                travelTime = routeList.get(i).getTravelTime();
                // String 역방향일 때 1
                break;
            }
        }
    }

    // 서버에서 받아온 데이터로 경로 저장
    private void getRouteItems() {
        try {
            for (int i = 0; i < ActivityWaypoint.jsonWaypointArray.length(); i++) {
                JSONObject result = ActivityWaypoint.jsonRouteArray.getJSONObject(i);
                routeList.add(new RouteModel(result.getString("id"), result.getString("starting_point"),
                        result.getString("arrival_point"), result.getInt("travel_time"), result.getInt("travel_distance")));
            }
        }catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "에러 하이고" );
        }

        Log.d(TAG, "경로배열 원래 크기 " + routeList.size());
    }

    public void orderShowRequest() {
        Log.d(TAG, "마커정보 size " + list.size());
        for (int i = 0; i < list.size(); i++) {
            Log.d(TAG, "마커 제목들" + selectedTitle[0] +"  " + list.get(i).getTitle());
            if(selectedTitle[0].equals(list.get(i).getTitle())) {
                startingId = list.get(i).getId();
                Log.d(TAG, "d---" + list.get(i).getId());
            }
        }

        for (int i = 0; i < list.size(); i++) {
            Log.d(TAG, "마커 제목들" + selectedTitle[1] +"  " + list.get(i).getTitle());
            if(selectedTitle[1].equals(list.get(i).getTitle())) {
                arrivalId = list.get(i).getId();
                Log.d(TAG, "d---" + list.get(i).getId());
            }
        }
        Log.d(TAG, "출발지 id값 " + startingId);
        String url = "http://13.124.189.186/api/orders/show?starting_id=" + startingId + "&guard=user";
        StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @SuppressLint("SetTextI18n")
            public void onResponse(String response) {
                int moveNeedTime = 0;
                int remainOrder = 0;
                String getMsg = null;
                Log.d(TAG,"순서대로 " + response);
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    getMsg = jsonResponse.getString("message");//모든경우

                    if(getMsg.equals("Cart is ready for start")) {
                        cartId = jsonResponse.getString("cart_id"); //가용차량 있을때만
                        moveNeed = jsonResponse.getBoolean("cartMove_needs");
                    }
                    if(getMsg.equals("Cart is need to move")) {
                        cartId = jsonResponse.getString("cart_id"); //가용차량 있을때만
                        moveNeed = jsonResponse.getBoolean("cartMove_needs"); //가용차량 있을때만
                        moveNeedTime = jsonResponse.getInt("cartMove_time"); // 가용차량 있고 움직일때만
                        moveNeedRoute = jsonResponse.getString("cartMove_route"); // 가용차량 있고 움직일때만
                    }
                    if(getMsg.equals("There is no available cart")) {
                        remainOrder = jsonResponse.getInt("remain_order");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, "가용에러 하이고" );
                }
                if(getMsg.equals("There is no available cart")) {
                    setVisibilityInfo = true;
                    binding.watingNumber.setText("현재 대기열 : " + remainOrder);
                }else if(moveNeed) {
                    binding.timeAttack.setText("차량도착시간 | " + moveNeedTime + ":00");
                    binding.timeResult.setText("총 소요 시간 | " + (moveNeedTime + travelTime) + ":00");
                }else {
                    binding.timeAttack.setText("차량도착시간 | 00:00");
                    binding.timeResult.setText("총 소요 시간 | " + travelTime + ":00");
                }
            }
        },new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG,"가용에러 -> " + error.getMessage());
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError{
                Map<String, String> params = new HashMap<String, String>();
                params.put("Authorization" , "Bearer " + ActivityLogin.loginResponse);
                return params;
            }
        };

        request.setShouldCache(false);
        requestQueue.add(request);
        Log.d(TAG, "way요청보냄");
    }
    //addMarker 재수정
    public void addMarker(MarkerModel markerModel, boolean isSelectedMarker) {
        LatLng position = new LatLng(markerModel.getLat(), markerModel.getLng());
        String title = markerModel.getTitle();
        String idString = markerModel.getId();
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title(title);
        markerOptions.position(position);

        Bitmap markerDraw = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.baseline_place_black_18dp),
                100, 100, false);
        Bitmap markerDrawSelected_1= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.baseline_pin_drop_white_24dp),
                100, 100, false);
        Bitmap markerDrawSelected_2= Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.baseline_pin_drop_black_24dp),
                100, 100, false);

//         클릭시 아이콘 배치할겨
        if(isSelectedMarker) {
            if(selectedCount != 2) {
                selectedTitle[selectedCount] = title;
                selectedCount++;
                if(selectedCount == 2) {
                    orderShowRequest(); // 출발지 클릭시 실행
                    routeTimeChecking();
                }
                Log.d(TAG, "제목 설정 및 카운트 업");
            }

            if (selectedCount == 1) {
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(markerDrawSelected_1));
            } else if(selectedCount == 2){
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(markerDrawSelected_2));
            }
            Log.d(TAG, "마커 표시addMarker " + title);
        }else {
            if(selectedCount != 0) { selectedCount--; }
            Log.d(TAG, "제목 카운터 다운");
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(markerDraw));
        }

        markersInfo.add(new MarkerModel(mMap.addMarker(markerOptions), title, idString));
        Log.d(TAG, "배열 넣어진 크기 " + markersInfo.size());
    }
    // 서버에서 받아온 데이터로 마커 생성
    private void getMarkerItems() {
        try {
            for (int i = 0; i < ActivityWaypoint.jsonWaypointArray.length(); i++) {
                JSONObject result = ActivityWaypoint.jsonWaypointArray.getJSONObject(i);
                list.add(new MarkerModel(result.getDouble("lat"), result.getDouble("lng"),
                        result.getString("name"), result.getString("id")));
            }
        }catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "에러 하이고" );
        }

        Log.d(TAG, "배열 원래 크기 " + list.size());
        for (MarkerModel markerModel : list) {
            addMarker(markerModel, false);
        }
    }
    // 표시 마커 동작
    private void changeSelectedMarker(Marker marker) {
        Log.d(TAG, "마커 정보 : " + marker);
        Log.d(TAG, "배열 넣어진 크기 changeSelectMarker" + markersInfo.size());
        // 선택했던 마커 다시 선택시 되돌리기 단, 순서대로
        if((selectedTitle[0] != null && selectedTitle[0].equals(marker.getTitle())) && !marker.getTitle().equals("car")
                || (selectedTitle[1] != null && selectedTitle[1].equals(marker.getTitle()) && !marker.getTitle().equals("car"))) {
            if(selectedTitle[1] == null) {
                markersCheck(marker);
                addMarker(new MarkerModel(marker.getPosition().latitude, marker.getPosition().longitude,
                        marker.getTitle(), marker.getId()), false);
                marker.remove();
                selectedTitle[0] = null;
                Log.d(TAG, "제목 초기화");
            }else if(selectedTitle[1].equals(marker.getTitle()) && !marker.getTitle().equals("car")){
                markersCheck(marker);
                addMarker(new MarkerModel(marker.getPosition().latitude, marker.getPosition().longitude,
                        marker.getTitle(), marker.getId()), false);
                marker.remove();
                selectedTitle[1] = null;
                Log.d(TAG, "제목 초기화");
            }
        }else if (marker != null && selectedCount < 2 && !marker.getTitle().equals("car")) { //선택한 마커 표시
            markersCheck(marker);
            addMarker(new MarkerModel(marker.getPosition().latitude, marker.getPosition().longitude,
                    marker.getTitle(), marker.getId()), true);
            marker.remove();
        }
    }
    // 클릭한 마커의 정보가 있는지 체크
    public void markersCheck(Marker marker) {
        Log.d(TAG, "배열 넣어진 크기 markersCheck" + markersInfo.size());
        for(int i = 0; i < markersInfo.size(); i++) {
            Log.d(TAG, "마커배열들 " + markersInfo.get(i).getTitle());
            if(markersInfo.get(i).getTitle().equals(marker.getTitle())) {
                markersInfo.remove(i);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        getMarkerItems();
        getRouteItems();
        //-----------------------------------------------------------------------
        LatLng YJU = new LatLng(35.896274, 128.621827);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(YJU, 17));
        //마커 클릭에 대한 이벤트 처리
        mMap.setOnMarkerClickListener(this);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(YJU));
    }

    DrawerLayout.DrawerListener listener = new DrawerLayout.DrawerListener() {
        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

        }
        @Override
        public void onDrawerOpened(@NonNull View drawerView) {

        }
        @Override
        public void onDrawerClosed(@NonNull View drawerView) {

        }
        @Override
        public void onDrawerStateChanged(int newState) {

        }
    };

        @Override
    public boolean onMarkerClick(Marker marker) {
        changeSelectedMarker(marker);
        changeSetVisible();
        //marker.getId()는 마커생성 순서
        startPoint.setText(String.format("출발지: %s", selectedTitle[0]));
        endPoint.setText(selectedTitle[1] == null ? "" : "도착지: " + selectedTitle[1]);

        Log.d(TAG, "마커 제발 되라 " + selectedCount + " 제목좀 " +selectedTitle[0] + selectedTitle[1]);
        return false;
    }

    public void changeSetVisible() {
        if(selectedTitle[0] ==  null || selectedTitle[1] == null) {
            binding.deliveryInfo.setVisibility(View.GONE);
            binding.deliveryInfoWating.setVisibility(View.GONE);
        }else {
            if(setVisibilityInfo)
                binding.deliveryInfoWating.setVisibility(View.VISIBLE);
            else
                binding.deliveryInfo.setVisibility(View.VISIBLE);
        }

        if(selectedTitle[1] != null) {
            Log.d(TAG, "배열 넣어진 크기 visible" + markersInfo.size());
            for(int i = 0; i < markersInfo.size(); i++) {
                int count = 0;
                Log.d(TAG, "배열크기" + markersInfo.size());
                Log.d(TAG, "마커정보들과 선택된 제목" + markersInfo.get(i).getTitle());
                Log.d(TAG, "마커정보들과 선택된 제목" + selectedTitle[0] + selectedTitle[1]);
                for (String s : selectedTitle) {
                    if (!markersInfo.get(i).getTitle().equals(s)) {
                        count++;
                        if(count == 2) markersInfo.get(i).getMarker().setVisible(false);
                    }
                }
            }
        }else {
            for(int i = 0; i < markersInfo.size(); i++) {
                markersInfo.get(i).getMarker().setVisible(true);
            }
        }
    }
}