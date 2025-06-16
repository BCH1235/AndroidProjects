package com.am.mytodolistapp.service;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.am.mytodolistapp.MainActivity;
import com.am.mytodolistapp.R;

// 앱의 알림 생성을 담당하는 클래스
// 알림 생성 및 위치 기반 알림 표시 기능을 제공
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "location_task_channel";
    private static final String CHANNEL_NAME = "위치 기반 할 일";
    private static final String CHANNEL_DESCRIPTION = "위치에 도착했을 때 할 일 알림";

    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannel();
    }


    // 알림을 표시하기 위해 필요한 알림 채널을 생성
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableVibration(true);
            channel.enableLights(true);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private boolean checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    } // 알림 권한이 부여되었는지 확인


    //위치 기반 할 일에 대한 알림을 생성하고 표시
    public void showLocationBasedTaskNotification(int taskId, String taskTitle, String locationName) {
        Log.d(TAG, "위치 기반 알림 표시: " + taskTitle + " at " + locationName);

        if (!checkNotificationPermission()) {
            Log.e(TAG, "알림 권한 없음");
            return;
        }

        Intent intent = new Intent(context, MainActivity.class); // 알림 클릭 시 MainActivity를 열기 위한 Intent 생성
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("TASK_ID", taskId); // 클릭된 할 일 ID를 전달

        // Intent를 감싸는 PendingIntent 생성
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                taskId,
                intent,
                flags
        );

        String notificationTitle = "📍 위치 기반 할 일 알림";
        String notificationText = locationName + "에 도착했습니다!\n할 일: " + taskTitle;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // 작은 아이콘
                .setContentTitle(notificationTitle) // 알림 제목
                .setContentText(notificationText) // 알림 내용
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true) // 클릭 시 알림 자동 삭제
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            if (notificationManager.areNotificationsEnabled()) {
                // 알림 표시
                notificationManager.notify(taskId, builder.build());
                Log.d(TAG, "알림 표시 성공: " + taskTitle);
            } else {
                Log.w(TAG, "알림이 비활성화됨");
            }
        } catch (Exception e) {
            Log.e(TAG, "알림 표시 오류", e);
        }
    }
}