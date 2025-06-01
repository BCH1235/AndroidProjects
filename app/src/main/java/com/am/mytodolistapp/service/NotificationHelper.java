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

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESCRIPTION);

            NotificationManager notificationManager =
                    context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // 알림 권한 확인 메서드 추가
    private boolean checkNotificationPermission() {
        // Android 13 이상에서는 POST_NOTIFICATIONS 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        // Android 13 미만은 별도 권한 필요 없음
        return true;
    }

    public void showLocationBasedTaskNotification(int taskId, String taskTitle, String locationName) {
        // 알림 권한 확인
        if (!checkNotificationPermission()) {
            Log.e(TAG, "알림 권한이 없습니다.");
            return;
        }

        // 메인 액티비티를 열기 위한 인텐트
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("TASK_ID", taskId);

        // API 레벨 호환성 고려한 플래그 설정
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

        // 알림 메시지 구성
        String notificationTitle = "할 일 알림";
        String notificationText = locationName + "에 도착했습니다. 할 일: " + taskTitle;

        // 알림 생성
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // 적절한 아이콘으로 변경
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // 알림 표시 (try-catch로 안전하게 처리)
        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.notify(taskId, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "알림 표시 권한 오류: " + e.getMessage());
        }
    }
}