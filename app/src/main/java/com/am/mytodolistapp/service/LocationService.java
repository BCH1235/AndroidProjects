package com.am.mytodolistapp.service;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.am.mytodolistapp.data.TodoItem;
import com.am.mytodolistapp.receiver.GeofenceBroadcastReceiver;

import java.util.ArrayList;
import java.util.List;

 // 위치 관련 서비스 로직을 처리하는 클래스
// Geofence 등록, 해제 및 위치 업데이트 요청을 담당한다.
public class LocationService {
    private static final String TAG = "LocationService";
    private static final int GEOFENCE_RADIUS_IN_METERS = 100;
    private static final long LOCATION_UPDATE_INTERVAL = 30000; // 30초
    private static final long LOCATION_UPDATE_FASTEST_INTERVAL = 10000; // 10초

    private final Context context;
    private final GeofencingClient geofencingClient; // Geofence API와 상호작용하는 클라이언트
    private final FusedLocationProviderClient fusedLocationClient; // 위치 정보를 얻기 위한 클라이언트
    private LocationCallback locationCallback;

    public LocationService(Context context) {
        this.context = context;
        this.geofencingClient = LocationServices.getGeofencingClient(context);
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        // 위치 업데이트 콜백 설정
        initializeLocationCallback();
    }

    private void initializeLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                Log.d(TAG, "Location update received: " +
                        locationResult.getLastLocation().getLatitude() + ", " +
                        locationResult.getLastLocation().getLongitude());
                // 위치 업데이트가 있을 때마다 로그 출력
            }
        };
    }

    private boolean checkLocationPermission() {
        boolean fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        return fineLocation && coarseLocation;
    } // 위치 관련 권한이 부여되었는지 확인

    private boolean isLocationServicesEnabled() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null &&
                (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    } // 기기의 위치 서비스가 활성화되어 있는지 확인


    public void startLocationUpdates() {
        if (!checkLocationPermission()) {
            Log.e(TAG, "위치 권한 없음");
            return;
        }

        if (!isLocationServicesEnabled()) {
            Log.w(TAG, "위치 서비스가 비활성화됨");
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(LOCATION_UPDATE_FASTEST_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "위치 업데이트 요청 시작됨");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "위치 업데이트 요청 실패", e);
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException during location updates", e);
        }
    } //주기적인 위치 업데이트를 요청

    public void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "위치 업데이트 중지됨");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "위치 업데이트 중지 실패", e);
                    });
        }
    } // 위치 업데이트 요청을 중지

    public void requestSingleLocationUpdate() {
        if (!checkLocationPermission()) {
            Log.e(TAG, "위치 권한 없음");
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            Log.d(TAG, "현재 위치: " + location.getLatitude() + ", " + location.getLongitude());
                        } else {
                            Log.w(TAG, "현재 위치를 가져올 수 없음");
                            // 새로운 위치 요청
                            requestFreshLocation();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "위치 요청 실패", e);
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException", e);
        }
    }

    private void requestFreshLocation() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(5000)
                .setFastestInterval(2000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1); // 한 번만 업데이트

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null && locationResult.getLastLocation() != null) {
                        Log.d(TAG, "새로운 위치 수신: " +
                                locationResult.getLastLocation().getLatitude() + ", " +
                                locationResult.getLastLocation().getLongitude());
                    }
                }
            }, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException during fresh location request", e);
        }
    }


     // 특정 할 일에 대한 Geofence를 등록
    public void registerGeofence(TodoItem todoItem) {
        Log.d(TAG, "Geofence 등록 시도: " + todoItem.getTitle());

        if (!todoItem.isLocationEnabled()) {
            Log.w(TAG, "위치 기능이 비활성화됨: " + todoItem.getTitle());
            return;
        }

        if (todoItem.getLocationLatitude() == 0 && todoItem.getLocationLongitude() == 0) {
            Log.w(TAG, "잘못된 좌표: " + todoItem.getTitle());
            return;
        }

        if (!checkLocationPermission()) {
            Log.e(TAG, "위치 권한 없음");
            return;
        }

        float radius = todoItem.getLocationRadius() > 0 ? todoItem.getLocationRadius() : GEOFENCE_RADIUS_IN_METERS;

        // Geofence 객체 생성
        Geofence geofence = new Geofence.Builder()
                .setRequestId(String.valueOf(todoItem.getId())) // 할 일 ID를 고유 식별자로 사용
                .setCircularRegion(
                        todoItem.getLocationLatitude(),
                        todoItem.getLocationLongitude(),
                        radius
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER) // 진입 이벤트만 감지
                .setNotificationResponsiveness(0) // 즉시 응답
                .setLoiteringDelay(1000)
                .build();

        // Geofencing 요청 생성
        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER) // 등록 시 이미 영역 안에 있으면 즉시 발동
                .addGeofence(geofence)
                .build();

        PendingIntent pendingIntent = getGeofencePendingIntent();

        try {
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Geofence 등록 성공: " + todoItem.getTitle());
                        // Geofence 등록 후 위치 업데이트 시작
                        startLocationUpdates(); // Geofence 등록 후 위치 업데이트 시작
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Geofence 등록 실패: " + todoItem.getTitle(), e);
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException", e);
        }
    }


    // 여러 할 일에 대한 Geofence를 한 번에 등록
    public void registerGeofences(List<TodoItem> todoItems) {
        Log.d(TAG, "배치 Geofence 등록: " + todoItems.size() + "개");

        if (!checkLocationPermission()) {
            Log.e(TAG, "위치 권한 없음");
            return;
        }

        List<Geofence> geofences = new ArrayList<>();

        for (TodoItem todoItem : todoItems) {
            if (todoItem.isLocationEnabled() &&
                    !(todoItem.getLocationLatitude() == 0 && todoItem.getLocationLongitude() == 0)) {

                float radius = todoItem.getLocationRadius() > 0 ? todoItem.getLocationRadius() : GEOFENCE_RADIUS_IN_METERS;

                Geofence geofence = new Geofence.Builder()
                        .setRequestId(String.valueOf(todoItem.getId()))
                        .setCircularRegion(
                                todoItem.getLocationLatitude(),
                                todoItem.getLocationLongitude(),
                                radius
                        )
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .setNotificationResponsiveness(0) // 즉시 응답 (0ms)
                        .setLoiteringDelay(1000) // 1초 후 트리거
                        .build();

                geofences.add(geofence);
                Log.d(TAG, "Geofence 추가: " + todoItem.getTitle());
            }
        }

        if (geofences.isEmpty()) {
            Log.w(TAG, "등록할 유효한 Geofence가 없음");
            return;
        }

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build();

        PendingIntent pendingIntent = getGeofencePendingIntent();

        try {
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "배치 Geofence 등록 성공: " + geofences.size() + "개");
                        // 배치 Geofence 등록 후 위치 업데이트 시작
                        startLocationUpdates();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "배치 Geofence 등록 실패", e);
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException", e);
        }
    }


     //특정 할 일에 대한 Geofence를 제거
    public void removeGeofence(TodoItem todoItem) {
        Log.d(TAG, "Geofence 제거: " + todoItem.getTitle());

        List<String> geofenceIds = new ArrayList<>();
        geofenceIds.add(String.valueOf(todoItem.getId()));

        geofencingClient.removeGeofences(geofenceIds)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Geofence 제거 성공: " + todoItem.getTitle());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Geofence 제거 실패: " + todoItem.getTitle(), e);
                });
    }

    //등록된 모든 Geofence를 제거
    public void removeAllGeofences() {
        Log.d(TAG, "모든 Geofence 제거");

        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "모든 Geofence 제거 성공");
                    // 모든 Geofence 제거 시 위치 업데이트도 중지
                    stopLocationUpdates();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "모든 Geofence 제거 실패", e);
                });
    }

    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }

        return PendingIntent.getBroadcast(context, 0, intent, flags);
    }
}