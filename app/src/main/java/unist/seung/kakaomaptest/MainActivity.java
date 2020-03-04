package unist.seung.kakaomaptest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonObject;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public class MainActivity extends AppCompatActivity {

    private GpsTracker gpsTracker;

    MapView mapView;
    MapPOIItem marker;
    FloatingActionButton locationButton;
    Button zoomIn, zoomOut;
    TextView addressView, roadView;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        }else {
            checkRunTimePermission();
        }

        gpsTracker = new GpsTracker(MainActivity.this);
        locationButton = findViewById(R.id.floatingActionButton);
        zoomIn = findViewById(R.id.zoomIn);
        zoomOut = findViewById(R.id.zoomOut);
        addressView = findViewById(R.id.address);
        roadView = findViewById(R.id.road);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                getAddress(35.537190, 129.313530);
            }
        });

        mapView = new MapView(this);
        ViewGroup mapViewContainer = findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);
        mapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(gpsTracker.getLatitude(), gpsTracker.getLongitude()), true);

        marker = new MapPOIItem();
        marker.setItemName("Current Location");
        marker.setTag(0);
        marker.setMarkerType(MapPOIItem.MarkerType.BluePin); // 기본으로 제공하는 BluePin 마커 모양.
        marker.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin); // 마커를 클릭했을때, 기본으로 제공하는 RedPin 마커 모양.

        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double latitude = gpsTracker.getLatitude();
                double longitude = gpsTracker.getLongitude();
                Log.d("location", latitude + ", " + longitude);
                AsyncTask.execute(new Runnable() {
                    @Override
                    public void run() {
                        getAddress(gpsTracker.getLatitude(), gpsTracker.getLongitude());
                    }
                });

                mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(gpsTracker.getLatitude(), gpsTracker.getLongitude()),0, true);
                marker.setMapPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude));
                mapView.addPOIItem(marker);
            }
        });

        zoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.zoomIn(true);
            }
        });
        zoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapView.zoomOut(true);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, @NonNull String[] permissions, @NonNull int[] grandResults) {
        if (permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {
            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면
            boolean check_result = true;

            // 모든 퍼미션을 허용했는지 체크합니다.
            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }

            if (check_result) {
                Log.d("onRequestPermissionResult", "위치 값을 가져올 수 있습니다.");
            }
            else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0]) || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {
                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    void checkRunTimePermission() {
        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED && hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {
            Log.d("checkRunTimePermission", "Permission을 가지고 있습니다. 위치 값을 가져올 수 있습니다.");
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    private void showDialogForLocationServiceSetting() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n" + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GPS_ENABLE_REQUEST_CODE) {
            if (checkLocationServicesStatus()) {
                if (checkLocationServicesStatus()) {
                    Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                    checkRunTimePermission();
                }
            }
        }
    }

    private interface ApiService {
        String base = "http://dapi.kakao.com/";
        String REST_API_CODE = "fa63f8d02a23fd8daae5739d86a7c620";

        @Headers("Authorization: KakaoAK " + REST_API_CODE)
        @GET("/v2/local/geo/coord2address.json")
        Call<JsonObject> getAddress(@Query("x") double lon, @Query("y") double lat, @Query("input_coord") String input);
    }

    private void getAddress(double lat, double lon) {
        JsonParser jsonParser = new JsonParser();
        Retrofit retrofit = new Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()).baseUrl(ApiService.base).build();
        ApiService api = retrofit.create(ApiService.class);
        Call<JsonObject> call = api.getAddress(lon, lat, "WGS84");
        try {
            JsonObject json = call.execute().body();
            assert json != null;
            jsonParser.parse(json);
            addressView.setText(jsonParser.address);
            roadView.setText(jsonParser.roadName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        assert locationManager != null;
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

}
