package com.am.mytodolistapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.am.mytodolistapp.data.TodoRepository;
import com.am.mytodolistapp.data.firebase.FirebaseRepository;
import com.am.mytodolistapp.service.LocationService;
import com.am.mytodolistapp.ui.category.CategoryManagementFragment;
import com.am.mytodolistapp.ui.calendar.ImprovedCalendarFragment;
import com.am.mytodolistapp.ui.task.ImprovedTaskListFragment;
import com.am.mytodolistapp.ui.location.LocationBasedTaskFragment;
import com.am.mytodolistapp.ui.stats.StatisticsFragment;
import com.am.mytodolistapp.ui.auth.AuthFragment;
import com.am.mytodolistapp.ui.collaboration.CollaborationFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, FragmentManager.OnBackStackChangedListener {
    private static final String TAG = "MainActivity";

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1002;
    private static final int REQUEST_BACKGROUND_LOCATION_PERMISSION = 1003;

    private FirebaseAuth firebaseAuth;
    private FirebaseRepository firebaseRepository;
    private LocationService locationService;

    private TodoRepository todoRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseRepository = FirebaseRepository.getInstance();
        todoRepository = new TodoRepository(getApplication());
        locationService = new LocationService(this);

        initializeViews();
        setupNavigationDrawer();
        checkAndRequestPermissions();
        initializeCollaborationSync();

        getSupportFragmentManager().addOnBackStackChangedListener(this);

        if (savedInstanceState == null) {
            loadFragment(new ImprovedTaskListFragment());
            navigationView.setCheckedItem(R.id.nav_task_list);
        }

        updateMenuVisibility();
        onBackStackChanged();
    }

    @Override
    public void onBackStackChanged() {
        boolean isSubFragment = getSupportFragmentManager().getBackStackEntryCount() > 0;

        if (getSupportActionBar() != null) {
            // 뒤로가기 화살표(Up Button) 표시 여부만 제어
            getSupportActionBar().setDisplayHomeAsUpEnabled(isSubFragment);
            // 햄버거 아이콘과 뒤로가기 화살표 상태 전환
            toggle.setDrawerIndicatorEnabled(!isSubFragment);
            toggle.syncState();
        }
        // 메뉴를 다시 그리도록 요청하여 각 Fragment가 자신의 메뉴를 표시하게 함
        invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == android.R.id.home) {

            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                onBackPressed();
                return true;
            }
        }

        // 네비게이션 드로어 토글
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
    }

    private void setupNavigationDrawer() {
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
    }

    private void initializeCollaborationSync() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User is logged in, starting collaboration sync for: " + currentUser.getEmail());
            todoRepository.startCollaborationSync();
        } else {
            Log.d(TAG, "No user logged in, skipping collaboration sync");
        }
    }

    public void onUserLoggedIn() {
        Log.d(TAG, "User logged in, starting collaboration sync");
        todoRepository.startCollaborationSync();
        updateMenuVisibility();
        loadFragment(new ImprovedTaskListFragment());
        navigationView.setCheckedItem(R.id.nav_task_list);
        Toast.makeText(this, "로그인되었습니다. 협업 할 일을 동기화하는 중...", Toast.LENGTH_SHORT).show();
    }

    public void onUserLoggedOut() {
        Log.d(TAG, "User logged out, stopping collaboration sync");
        todoRepository.stopCollaborationSync();
        todoRepository.deleteAllCollaborationTodos();
        updateMenuVisibility();
        Toast.makeText(this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMenuVisibility();
        if (checkLocationPermissionGranted()) {
            locationService.requestSingleLocationUpdate();
        }
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null && !todoRepository.isCollaborationSyncActive()) {
            Log.d(TAG, "App resumed, restarting collaboration sync");
            todoRepository.startCollaborationSync();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (todoRepository != null) {
            Log.d(TAG, "App destroying, stopping collaboration sync");
            todoRepository.stopCollaborationSync();
        }
    }

    private void updateMenuVisibility() {
        if (navigationView != null) {
            boolean isLoggedIn = isUserLoggedIn();
            navigationView.getMenu().findItem(R.id.nav_collaboration).setVisible(isLoggedIn);
            navigationView.getMenu().findItem(R.id.nav_auth).setVisible(!isLoggedIn);
            navigationView.getMenu().findItem(R.id.nav_logout).setVisible(isLoggedIn);
            Log.d(TAG, "Menu visibility updated, user logged in: " + isLoggedIn);
        }
    }

    private boolean checkLocationPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION);
        } else {
            checkAndRequestBackgroundLocationPermission();
        }

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
        checkBatteryOptimization();
    }

    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                showBatteryOptimizationDialog();
            }
        }
    }

    private void showBatteryOptimizationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("배터리 최적화 제외 필요")
                .setMessage("위치 기반 알림이 정확히 동작하려면 이 앱을 배터리 최적화에서 제외해야 합니다. 설정으로 이동하시겠습니까?")
                .setPositiveButton("설정으로 이동", (dialog, which) -> requestBatteryOptimizationExemption())
                .setNegativeButton("나중에", null)
                .show();
    }

    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
                Toast.makeText(this, "목록에서 '" + getString(R.string.app_name) + "'을 찾아 최적화를 해제해주세요.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkAndRequestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(this)
                        .setTitle("백그라운드 위치 권한 필요")
                        .setMessage("앱이 꺼져 있을 때도 위치 기반 알림을 받으려면, 위치 접근 권한을 '항상 허용'으로 설정해야 합니다.")
                        .setPositiveButton("설정으로 이동", (dialog, which) -> {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                    REQUEST_BACKGROUND_LOCATION_PERMISSION);
                        })
                        .setNegativeButton("취소", null)
                        .show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "위치 권한이 승인되었습니다.", Toast.LENGTH_SHORT).show();
                checkAndRequestBackgroundLocationPermission();
                locationService.requestSingleLocationUpdate();
            } else {
                Toast.makeText(this, "위치 기반 알림을 사용하려면 위치 권한이 필요합니다.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "알림 권한이 승인되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "알림을 받으려면 알림 권한이 필요합니다.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_BACKGROUND_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "백그라운드 위치 권한이 승인되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "백그라운드 위치 권한이 거부되었습니다. 앱 설정에서 직접 '항상 허용'으로 변경해주세요.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean isUserLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_task_list) {
            selectedFragment = new ImprovedTaskListFragment();
        } else if (itemId == R.id.nav_location_tasks) {
            selectedFragment = new LocationBasedTaskFragment();
        } else if (itemId == R.id.nav_calendar) {
            selectedFragment = new ImprovedCalendarFragment();
        } else if (itemId == R.id.nav_categories) {
            selectedFragment = new CategoryManagementFragment();
        } else if (itemId == R.id.nav_statistics) {
            selectedFragment = new StatisticsFragment();
        } else if (itemId == R.id.nav_collaboration) {
            selectedFragment = isUserLoggedIn() ? new CollaborationFragment() : new AuthFragment();
        } else if (itemId == R.id.nav_auth) {
            selectedFragment = new AuthFragment();
        } else if (itemId == R.id.nav_logout) {
            showLogoutConfirmDialog();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if (selectedFragment != null) {
            loadFragment(selectedFragment);
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showLogoutConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃하시겠습니까?")
                .setPositiveButton("로그아웃", (dialog, which) -> performLogout())
                .setNegativeButton("취소", null)
                .show();
    }

    private void performLogout() {
        Log.d(TAG, "Performing logout...");
        firebaseRepository.signOut(new FirebaseRepository.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Firebase logout successful");
                onUserLoggedOut();
                updateMenuVisibility();
                loadFragment(new AuthFragment());
                navigationView.setCheckedItem(R.id.nav_auth);
                Toast.makeText(MainActivity.this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Firebase logout failed", e);
                Toast.makeText(MainActivity.this, "로그아웃 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        boolean isTopLevel = fragment instanceof ImprovedTaskListFragment ||
                fragment instanceof AuthFragment ||
                fragment instanceof CollaborationFragment ||
                fragment instanceof LocationBasedTaskFragment ||
                fragment instanceof ImprovedCalendarFragment ||
                fragment instanceof StatisticsFragment ||
                fragment instanceof CategoryManagementFragment;

        if (isTopLevel) {
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);

        if (!isTopLevel) {
            fragmentTransaction.addToBackStack(null);
        }

        fragmentTransaction.commit();
    }

    public LocationService getLocationService() {
        return locationService;
    }

    public void triggerManualSync() {
        if (todoRepository != null) {
            Log.d(TAG, "Triggering manual sync");
            todoRepository.performManualSync();
            Toast.makeText(this, "동기화 중...", Toast.LENGTH_SHORT).show();
        }
    }

    public void checkCollaborationTodoCount() {
        if (todoRepository != null) {
            todoRepository.getCollaborationTodoCount(count -> {
                Log.d(TAG, "Current collaboration todo count: " + count);
            });
        }
    }

    public void logSyncStatus() {
        if (todoRepository != null) {
            boolean isActive = todoRepository.isCollaborationSyncActive();
            int projectCount = todoRepository.getSyncingProjectCount();
            Log.d(TAG, "Sync status - Active: " + isActive + ", Projects: " + projectCount);
            todoRepository.logCollaborationInfo();
        }
    }
}