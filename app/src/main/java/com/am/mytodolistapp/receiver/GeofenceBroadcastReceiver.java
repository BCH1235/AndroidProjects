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

  // Geofence 이벤트를 수신하는 BroadcastReceiver 클래스
  // LocationService에 의해 등록된 Geofence 영역에 사용자가 진입하면 시스템이 이 Receiver를 호출한다.
public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";

    //시스템으로부터 Geofence 관련 브로드캐스트를 수신했을 때 호출되는 메서드이다.
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Geofence broadcast received");

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent); //GeofencingEvent 객체를 추출

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
            // 이벤트를 발생시킨 Geofence 목록 가져오기
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            if (triggeringGeofences != null && !triggeringGeofences.isEmpty()) {
                for (Geofence geofence : triggeringGeofences) {
                    String requestId = geofence.getRequestId();
                    Log.d(TAG, "Triggered geofence ID: " + requestId); // Geofence 등록 시 설정했던 할 일의 ID를 가져온다.

                    try {
                        int taskId = Integer.parseInt(requestId);

                        // 데이터베이스에서 할 일 정보 조회(백그라운드)
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            try {
                                TodoDao todoDao = AppDatabase.getDatabase(context).todoDao(); // 데이터베이스 인스턴스와 DAO를 가져온다.
                                TodoItem todoItem = todoDao.getTodoByIdSync(taskId); // 할 일 ID를 사용하여 데이터베이스에서 해당 할 일 정보를 동기적으로 조회한다.

                                // 할 일이 존재하고 아직 완료되지 않은 경우에만 알림을 보냅니다.
                                if (todoItem != null && !todoItem.isCompleted()) {
                                    Log.d(TAG, "Showing notification for task: " + todoItem.getTitle());

                                    // 메인 스레드에서 알림 표시
                                    android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                                    mainHandler.post(() -> {
                                        NotificationHelper notificationHelper = new NotificationHelper(context); // NotificationHelper를 통해 사용자에게 알림을 표시한다.
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
            Log.d(TAG, "Ignoring geofence transition: " + geofenceTransition); //진입 이벤트가 아닌 이탈은 무시
        }
    }
}