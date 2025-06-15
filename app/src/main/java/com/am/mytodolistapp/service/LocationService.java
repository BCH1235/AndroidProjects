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
    private static final int GEOFENCE_RADIUS_IN_METERS = 100;

    private final Context context;
    private final GeofencingClient geofencingClient;

    public LocationService(Context context) {
        this.context = context;
        this.geofencingClient = LocationServices.getGeofencingClient(context);
    }

    private boolean checkLocationPermission() {
        boolean fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        return fineLocation && coarseLocation;
    }

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

        Geofence geofence = new Geofence.Builder()
                .setRequestId(String.valueOf(todoItem.getId()))
                .setCircularRegion(
                        todoItem.getLocationLatitude(),
                        todoItem.getLocationLongitude(),
                        radius
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        PendingIntent pendingIntent = getGeofencePendingIntent();

        try {
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Geofence 등록 성공: " + todoItem.getTitle());
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Geofence 등록 실패: " + todoItem.getTitle(), e);
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException", e);
        }
    }

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
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "배치 Geofence 등록 실패", e);
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException", e);
        }
    }

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

    public void removeAllGeofences() {
        Log.d(TAG, "모든 Geofence 제거");

        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "모든 Geofence 제거 성공");
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