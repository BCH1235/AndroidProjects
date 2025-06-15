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
    }

    public void showLocationBasedTaskNotification(int taskId, String taskTitle, String locationName) {
        Log.d(TAG, "위치 기반 알림 표시: " + taskTitle + " at " + locationName);

        if (!checkNotificationPermission()) {
            Log.e(TAG, "알림 권한 없음");
            return;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("TASK_ID", taskId);

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
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(notificationText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            if (notificationManager.areNotificationsEnabled()) {
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