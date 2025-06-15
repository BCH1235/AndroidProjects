package com.am.mytodolistapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

import com.am.mytodolistapp.data.firebase.FirebaseRepository;
import com.am.mytodolistapp.ui.CategoryManagementFragment;
import com.am.mytodolistapp.ui.ImprovedCalendarFragment;
import com.am.mytodolistapp.ui.ImprovedTaskListFragment;
import com.am.mytodolistapp.ui.LocationBasedTaskFragment;
import com.am.mytodolistapp.ui.StatisticsFragment;
import com.am.mytodolistapp.ui.auth.AuthFragment;
import com.am.mytodolistapp.ui.collaboration.CollaborationFragment;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1002;
    private static final int REQUEST_BACKGROUND_LOCATION_PERMISSION = 1003;

    private FirebaseAuth firebaseAuth;
    private FirebaseRepository firebaseRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseRepository = FirebaseRepository.getInstance();

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            loadFragment(new ImprovedTaskListFragment());
            navigationView.setCheckedItem(R.id.nav_task_list);
        }

        checkAndRequestPermissions();
        updateMenuVisibility();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMenuVisibility();
    }

    private void updateMenuVisibility() {
        if (navigationView != null) {
            boolean isLoggedIn = isUserLoggedIn();

            navigationView.getMenu().findItem(R.id.nav_collaboration).setVisible(isLoggedIn);
            navigationView.getMenu().findItem(R.id.nav_auth).setVisible(!isLoggedIn);
            navigationView.getMenu().findItem(R.id.nav_logout).setVisible(isLoggedIn);
        }
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
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        return currentUser != null;
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
            if (isUserLoggedIn()) {
                selectedFragment = new CollaborationFragment();
            } else {
                selectedFragment = new AuthFragment();
            }
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
        firebaseRepository.signOut(new FirebaseRepository.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(MainActivity.this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();
                updateMenuVisibility();
                loadFragment(new AuthFragment());
                navigationView.setCheckedItem(R.id.nav_auth);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(MainActivity.this, "로그아웃 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
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

    public void onUserLoggedIn() {
        updateMenuVisibility();
    }
}