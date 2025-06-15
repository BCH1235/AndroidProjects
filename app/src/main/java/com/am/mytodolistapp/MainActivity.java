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

// NavigationView 리스너 인터페이스 구현 추가
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;
    // 권한 요청 코드
    private static final int REQUEST_LOCATION_PERMISSION = 1001;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1002;

    private static final int REQUEST_BACKGROUND_LOCATION_PERMISSION = 1003;//백그라운드 위치 권한 요청 코드

    private FirebaseAuth firebaseAuth;
    private FirebaseRepository firebaseRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseRepository = FirebaseRepository.getInstance();

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
            // 개선된 TaskListFragment 사용
            loadFragment(new ImprovedTaskListFragment());
            navigationView.setCheckedItem(R.id.nav_task_list);
        }

        // 필요한 권한 요청
        checkAndRequestPermissions();

        // 메뉴 상태 업데이트
        updateMenuVisibility();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 앱이 다시 활성화될 때마다 메뉴 상태 업데이트
        updateMenuVisibility();
    }

    // 로그인 상태에 따라 메뉴 아이템의 가시성을 업데이트하는 메서드
    private void updateMenuVisibility() {
        if (navigationView != null) {
            boolean isLoggedIn = isUserLoggedIn();

            // 협업 메뉴 - 로그인된 경우에만 표시
            navigationView.getMenu().findItem(R.id.nav_collaboration).setVisible(isLoggedIn);

            // 로그인 메뉴 - 로그인되지 않은 경우에만 표시
            navigationView.getMenu().findItem(R.id.nav_auth).setVisible(!isLoggedIn);

            // 로그아웃 메뉴 - 로그인된 경우에만 표시
            navigationView.getMenu().findItem(R.id.nav_logout).setVisible(isLoggedIn);
        }
    }

    // 필요한 권한 확인 및 요청 메서드
    private void checkAndRequestPermissions() {
        // 포그라운드 위치 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQUEST_LOCATION_PERMISSION);
        } else {
            // 포그라운드 권한이 이미 있다면, 백그라운드 권한을 확인하고 요청
            checkAndRequestBackgroundLocationPermission();
        }

        // 알림 권한 확인
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    //백그라운드 위치 권한을 확인하고 요청하는 메소드를 새로 추가
    private void checkAndRequestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 (API 29) 이상
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {


                new AlertDialog.Builder(this)
                        .setTitle("백그라운드 위치 권한 필요")
                        .setMessage("앱이 꺼져 있을 때도 위치 기반 알림을 받으려면, 위치 접근 권한을 '항상 허용'으로 설정해야 합니다.")
                        .setPositiveButton("설정으로 이동", (dialog, which) -> {
                            // 권한 요청
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                                    REQUEST_BACKGROUND_LOCATION_PERMISSION);
                        })
                        .setNegativeButton("취소", null)
                        .show();
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
                checkAndRequestBackgroundLocationPermission();
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
        else if (requestCode == REQUEST_BACKGROUND_LOCATION_PERMISSION) {
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

    // NavigationView 메뉴 아이템 클릭 시 호출될 메서드 (통합된 버전)
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_task_list) {
            // 개선된 TaskListFragment 사용
            selectedFragment = new ImprovedTaskListFragment();
        } else if (itemId == R.id.nav_location_tasks) {
            selectedFragment = new LocationBasedTaskFragment();
        } else if (itemId == R.id.nav_calendar) {
            // 개선된 CalendarFragment 사용
            selectedFragment = new ImprovedCalendarFragment();
        } else if (itemId == R.id.nav_categories) {
            selectedFragment = new CategoryManagementFragment();
        } else if (itemId == R.id.nav_statistics) {
            selectedFragment = new StatisticsFragment();
        } else if (itemId == R.id.nav_collaboration) {
            // 협업 메뉴 추가
            if (isUserLoggedIn()) {
                selectedFragment = new CollaborationFragment();
            } else {
                // 로그인이 필요한 경우 인증 프래그먼트로 이동
                selectedFragment = new AuthFragment();
            }
        } else if (itemId == R.id.nav_auth) {
            // 로그인 메뉴 클릭
            selectedFragment = new AuthFragment();
        } else if (itemId == R.id.nav_logout) {
            // 로그아웃 메뉴 클릭
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

    // 로그아웃 확인 다이얼로그 표시
    private void showLogoutConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("로그아웃")
                .setMessage("정말 로그아웃하시겠습니까?")
                .setPositiveButton("로그아웃", (dialog, which) -> performLogout())
                .setNegativeButton("취소", null)
                .show();
    }

    // 로그아웃 실행
    private void performLogout() {
        // Firebase에서 로그아웃
        firebaseRepository.signOut(new FirebaseRepository.OnCompleteListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                // 로그아웃 성공
                Toast.makeText(MainActivity.this, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show();

                // 메뉴 상태 업데이트
                updateMenuVisibility();

                // AuthFragment로 이동
                loadFragment(new AuthFragment());
                navigationView.setCheckedItem(R.id.nav_auth);
            }

            @Override
            public void onFailure(Exception e) {
                // 로그아웃 실패 (일반적으로 발생하지 않음)
                Toast.makeText(MainActivity.this, "로그아웃 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 프래그먼트를 교체하는 공통 메서드
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

    // 뒤로가기 버튼 처리 (드로어가 열려있으면 닫기, 아니면 기본 동작)
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // AuthFragment에서 로그인 성공 시 호출할 수 있는 공개 메서드
    public void onUserLoggedIn() {
        updateMenuVisibility();
    }
}