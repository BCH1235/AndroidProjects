package com.am.mytodolistapp;

import android.app.Application;
import android.util.Log;

import com.am.mytodolistapp.data.sync.CollaborationSyncService;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.jakewharton.threetenabp.AndroidThreeTen;

// 앱이 실행될 때 가장 먼저 호출되어 필요한 라이브러리나 서비스를 초기화한다.
public class MyTodoApplication extends Application {
    private static final String TAG = "MyTodoApplication";

    private static MyTodoApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        Log.d(TAG, "Application starting...");

        // 기본 라이브러리 초기화
        initializeLibraries();

        // Firebase 초기화
        initializeFirebase();

        // 협업 동기화 초기화 (로그인된 사용자가 있는 경우)
        initializeCollaborationSync();

        Log.d(TAG, "Application initialized successfully");
    }


    private void initializeLibraries() {
        try {
            // ThreeTenABP (Joda-Time 백포트) 라이브러리 초기화
            AndroidThreeTen.init(this);
            Log.d(TAG, "ThreeTenABP initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize libraries", e);
        }
    } // 앱에서 사용하는 기본 라이브러리들을 초기화한다.

    private void initializeFirebase() {
        try {
            // Firebase 앱 초기화 (자동으로 처리되지만 명시적으로 확인)
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase", e);
        }
    } // Firebase 서비스를 초기화


    private void initializeCollaborationSync() {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();

            if (currentUser != null) {
                Log.d(TAG, "Found logged in user, initializing collaboration sync: " + currentUser.getEmail());

                // 동기화 서비스 초기화 (실제 시작은 MainActivity에서)
                CollaborationSyncService syncService = CollaborationSyncService.getInstance(this);

                // 여기서는 서비스만 초기화하고, 실제 동기화 시작은 MainActivity에서 처리
                Log.d(TAG, "Collaboration sync service initialized");
            } else {
                Log.d(TAG, "No logged in user found, skipping collaboration sync initialization");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize collaboration sync", e);
        }
    } // 협업 동기화 서비스를 초기화한다.앱 시작 시 로그인된 사용자가 있으면 동기화 서비스를 준비 상태로 만든다.

    public static MyTodoApplication getInstance() {
        return instance;
    } // Application 인스턴스 반환

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.d(TAG, "Application terminating...");

        try {
            // 협업 동기화 중지
            CollaborationSyncService syncService = CollaborationSyncService.getInstance(this);
            syncService.stopAllSync();

            Log.d(TAG, "Application cleanup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error during application cleanup", e);
        }
    } //  앱이 종료될 때 호출

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "Low memory warning received");

        // 필요 시 메모리 정리 작업 수행
        // 예: 캐시 정리, 불필요한 리소스 해제 등
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Log.d(TAG, "Memory trim requested with level: " + level);

        // 메모리 사용량에 따른 정리 작업
        switch (level) {
            case TRIM_MEMORY_RUNNING_MODERATE:
            case TRIM_MEMORY_RUNNING_LOW:
            case TRIM_MEMORY_RUNNING_CRITICAL:
                // 앱이 실행 중일 때의 메모리 정리
                Log.d(TAG, "Performing memory cleanup while running");
                break;
            case TRIM_MEMORY_UI_HIDDEN:
                // UI가 숨겨졌을 때
                Log.d(TAG, "UI hidden, performing background cleanup");
                break;
            case TRIM_MEMORY_BACKGROUND:
            case TRIM_MEMORY_MODERATE:
            case TRIM_MEMORY_COMPLETE:
                // 백그라운드에서의 메모리 정리
                Log.d(TAG, "App in background, performing aggressive cleanup");
                break;
        }
    }

    public boolean isUserLoggedIn() {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = auth.getCurrentUser();
            return currentUser != null;
        } catch (Exception e) {
            Log.e(TAG, "Error checking login status", e);
            return false;
        }
    }

    public FirebaseUser getCurrentUser() {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            return auth.getCurrentUser();
        } catch (Exception e) {
            Log.e(TAG, "Error getting current user", e);
            return null;
        }
    }

    public void logAppInfo() {
        try {
            String packageName = getPackageName();
            String versionName = getPackageManager().getPackageInfo(packageName, 0).versionName;
            int versionCode = getPackageManager().getPackageInfo(packageName, 0).versionCode;

            Log.d(TAG, "=== App Info ===");
            Log.d(TAG, "Package: " + packageName);
            Log.d(TAG, "Version: " + versionName + " (" + versionCode + ")");
            Log.d(TAG, "User logged in: " + isUserLoggedIn());

            FirebaseUser user = getCurrentUser();
            if (user != null) {
                Log.d(TAG, "User email: " + user.getEmail());
            }
            Log.d(TAG, "================");
        } catch (Exception e) {
            Log.e(TAG, "Error logging app info", e);
        }
    }
}