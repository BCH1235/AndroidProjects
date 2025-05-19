package com.am.mytodolistapp.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.am.mytodolistapp.service.NotificationHelper;

import java.util.List;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
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

        // 진입 이벤트만 처리
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // 이벤트를 트리거한 Geofence 목록 가져오기
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // 알림에 필요한 정보 가져오기
            int taskId = Integer.parseInt(triggeringGeofences.get(0).getRequestId());
            String taskTitle = intent.getStringExtra("TASK_TITLE");
            String locationName = intent.getStringExtra("LOCATION_NAME");

            // 알림 표시
            NotificationHelper notificationHelper = new NotificationHelper(context);
            notificationHelper.showLocationBasedTaskNotification(taskId, taskTitle, locationName);
        }
    }
}