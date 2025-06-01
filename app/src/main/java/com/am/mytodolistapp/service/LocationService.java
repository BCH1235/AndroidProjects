package com.am.mytodolistapp.service;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.am.mytodolistapp.data.TodoItem;
import com.am.mytodolistapp.receiver.GeofenceBroadcastReceiver;

import java.util.ArrayList;
import java.util.List;

public class LocationService {
    private static final String TAG = "LocationService";

    private final Context context;
    private final GeofencingClient geofencingClient;

    public LocationService(Context context) {
        this.context = context;
        this.geofencingClient = LocationServices.getGeofencingClient(context);
    }

    // 위치 권한 확인 메서드
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }


    public void registerGeofence(TodoItem todoItem) {
        // 위치 기능이 활성화되어 있지 않거나, 위치 정보가 없는 경우 무시
        if (!todoItem.isLocationEnabled() ||
                (todoItem.getLocationLatitude() == 0 && todoItem.getLocationLongitude() == 0)) {
            return;
        }

        // 권한 확인
        if (!checkLocationPermission()) {
            Log.e(TAG, "위치 권한이 없습니다.");
            return;
        }

        // Geofence 생성
        Geofence geofence = new Geofence.Builder()
                .setRequestId(String.valueOf(todoItem.getId())) // TodoItem의 ID를 Geofence ID로 사용
                .setCircularRegion(
                        todoItem.getLocationLatitude(),
                        todoItem.getLocationLongitude(),
                        todoItem.getLocationRadius()
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE) // 만료 시간 없음
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER) // 영역 진입 시 알림
                .build();

        // GeofencingRequest 생성
        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        // PendingIntent 생성
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        intent.putExtra("TASK_ID", todoItem.getId());
        intent.putExtra("TASK_TITLE", todoItem.getTitle());
        intent.putExtra("LOCATION_NAME", todoItem.getLocationName());

        // API 레벨 호환성 고려한 플래그 설정
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                todoItem.getId(), // 각 할 일마다 고유한 requestCode 사용
                intent,
                flags
        );

        // Geofence 등록 (try-catch 블록으로 감싸기)
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "위치 권한이 없습니다.");
                return;
            }

            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Geofence added for task: " + todoItem.getTitle()))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to add geofence: " + e.getMessage()));
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage());
        }
    }

    public void registerGeofences(List<TodoItem> todoItems) {
        // 권한 확인
        if (!checkLocationPermission()) {
            Log.e(TAG, "위치 권한이 없습니다.");
            return;
        }

        List<Geofence> geofences = new ArrayList<>();

        // 위치 기능이 활성화된 할 일만 필터링
        for (TodoItem todoItem : todoItems) {
            if (todoItem.isLocationEnabled() &&
                    !(todoItem.getLocationLatitude() == 0 && todoItem.getLocationLongitude() == 0)) {

                Geofence geofence = new Geofence.Builder()
                        .setRequestId(String.valueOf(todoItem.getId()))
                        .setCircularRegion(
                                todoItem.getLocationLatitude(),
                                todoItem.getLocationLongitude(),
                                todoItem.getLocationRadius()
                        )
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .build();

                geofences.add(geofence);
            }
        }

        if (geofences.isEmpty()) {
            return;
        }

        // GeofencingRequest 생성
        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build();

        // PendingIntent 생성
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);

        // API 레벨 호환성 고려한 플래그 설정
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0, // 모든 Geofence에 대해 같은 PendingIntent 사용
                intent,
                flags
        );

        // Geofence 등록 (try-catch 블록으로 감싸기)
        try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "위치 권한이 없습니다.");
                return;
            }

            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Added " + geofences.size() + " geofences"))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to add geofences: " + e.getMessage()));
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage());
        }
    }
    public void removeGeofence(TodoItem todoItem) {
        List<String> geofenceIds = new ArrayList<>();
        geofenceIds.add(String.valueOf(todoItem.getId()));

        geofencingClient.removeGeofences(geofenceIds)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Geofence removed for task: " + todoItem.getTitle()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to remove geofence: " + e.getMessage()));
    }


    public void removeAllGeofences() {
        geofencingClient.removeGeofences(getPendingIntent())
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "All geofences removed"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to remove all geofences: " + e.getMessage()));
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);

        // API 레벨 호환성 고려한 플래그 설정
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }

        return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                flags
        );
    }
}