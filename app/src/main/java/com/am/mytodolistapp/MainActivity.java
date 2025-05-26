package com.am.mytodolistapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.am.mytodolistapp.ui.AnalysisFragment;
import com.am.mytodolistapp.ui.LocationBasedTaskFragment;
import com.am.mytodolistapp.ui.TaskListFragment;
import com.google.android.material.navigation.NavigationView;

// NavigationView 리스너 인터페이스 구현 추가
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    // 권한 요청 코드
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar 찾기 및 액션바로 설정
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // DrawerLayout 및 NavigationView 찾기
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // ActionBarDrawerToggle 설정
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // NavigationView 리스너 설정
        navigationView.setNavigationItemSelectedListener(this);

        // 앱 처음 실행 시 또는 화면 회전 시 프래그먼트 로드
        if (savedInstanceState == null) {
            loadFragment(new TaskListFragment());
            navigationView.setCheckedItem(R.id.nav_task_list);
        }

        // 필요한 권한 요청
        checkAndRequestPermissions();
    }

    // 필요한 권한 확인 및 요청 메서드
    private void checkAndRequestPermissions() {
        // 위치 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // 권한 요청
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION);
        }

        // Android 13 이상에서 알림 권한 확인
        if (Build.VERSION.SDK_INT >= 33) { // API 33 = TIRAMISU
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // 권한 요청
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 위치 권한 승인됨
                Toast.makeText(this, "위치 권한이 승인되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                // 위치 권한 거부됨
                Toast.makeText(this, "위치 기반 알림을 사용하려면 위치 권한이 필요합니다.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 알림 권한 승인됨
                Toast.makeText(this, "알림 권한이 승인되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                // 알림 권한 거부됨
                Toast.makeText(this, "알림을 받으려면 알림 권한이 필요합니다.", Toast.LENGTH_LONG).show();
            }
        }
    }
    // NavigationView 메뉴 아이템 클릭 시 호출될 메서드
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_task_list) {
            selectedFragment = new TaskListFragment();
        } else if (itemId == R.id.nav_location_tasks) {
            selectedFragment = new LocationBasedTaskFragment();
        } else if (itemId == R.id.nav_analysis) {
            selectedFragment = new AnalysisFragment();
        }

        if (selectedFragment != null) {
            loadFragment(selectedFragment);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // 프래그먼트를 교체하는 공통 메서드
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        // fragmentTransaction.addToBackStack(null); // 필요에 따라 백스택 추가
        fragmentTransaction.commit();
    }

    // 툴바의 햄버거 아이콘 클릭 이벤트를 ActionBarDrawerToggle 가 처리하도록 연결
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // 뒤로가기 버튼 처리 (드로어가 열려있으면 닫기, 아니면 기본 동작)
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

}