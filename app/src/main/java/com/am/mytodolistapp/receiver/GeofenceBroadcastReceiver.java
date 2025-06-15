package com.am.mytodolistapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.am.mytodolistapp.service.NotificationHelper;
import com.am.mytodolistapp.data.AppDatabase;
import com.am.mytodolistapp.data.TodoDao;
import com.am.mytodolistapp.data.TodoItem;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Geofence broadcast received");

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent == null) {
            Log.w(TAG, "Intent did not contain a geofence event.");
            return;
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: " + geofencingEvent.getErrorCode());
            return;
        }

        // 어떤 지오펜스 전환 이벤트가 발생했는지 확인
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        Log.d(TAG, "Geofence transition: " + geofenceTransition);

        // 진입 이벤트만 처리
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // 이벤트를 트리거한 Geofence 목록 가져오기
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            if (triggeringGeofences != null && !triggeringGeofences.isEmpty()) {
                for (Geofence geofence : triggeringGeofences) {
                    String requestId = geofence.getRequestId();
                    Log.d(TAG, "Triggered geofence ID: " + requestId);

                    try {
                        int taskId = Integer.parseInt(requestId);

                        // 데이터베이스에서 할 일 정보 조회
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            try {
                                TodoDao todoDao = AppDatabase.getDatabase(context).todoDao();
                                TodoItem todoItem = todoDao.getTodoByIdSync(taskId);

                                if (todoItem != null && !todoItem.isCompleted()) {
                                    Log.d(TAG, "Showing notification for task: " + todoItem.getTitle());

                                    // 메인 스레드에서 알림 표시
                                    android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                                    mainHandler.post(() -> {
                                        NotificationHelper notificationHelper = new NotificationHelper(context);
                                        notificationHelper.showLocationBasedTaskNotification(
                                                taskId,
                                                todoItem.getTitle(),
                                                todoItem.getLocationName()
                                        );
                                    });
                                } else {
                                    Log.w(TAG, "Todo item not found or already completed for ID: " + taskId);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing geofence trigger", e);
                            }
                        });

                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid task ID format: " + requestId, e);
                    }
                }
            } else {
                Log.w(TAG, "No triggering geofences found");
            }
        } else {
            Log.d(TAG, "Ignoring geofence transition: " + geofenceTransition);
        }
    }
}