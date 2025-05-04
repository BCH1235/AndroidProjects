package com.am.mytodolistapp;

import android.os.Bundle;
import android.view.MenuItem; // MenuItem 임포트

import androidx.annotation.NonNull; // NonNull 임포트
import androidx.appcompat.app.ActionBarDrawerToggle; // ActionBarDrawerToggle 임포트
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar; // Toolbar 임포트
import androidx.core.view.GravityCompat; // GravityCompat 임포트
import androidx.drawerlayout.widget.DrawerLayout; // DrawerLayout 임포트
import androidx.fragment.app.Fragment; // Fragment 임포트
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.am.mytodolistapp.ui.AnalysisFragment; // AnalysisFragment 임포트
import com.am.mytodolistapp.ui.TaskListFragment;
import com.google.android.material.navigation.NavigationView; // NavigationView 임포트

// NavigationView 리스너 인터페이스 구현 추가
public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1단계에서 수정한 레이아웃 설정
        setContentView(R.layout.activity_main);

        // Toolbar 찾기 및 액션바로 설정
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // DrawerLayout 및 NavigationView 찾기
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // ActionBarDrawerToggle 설정 (햄버거 아이콘과 드로어 연결)
        // R.string... 부분은 아래에서 추가할 문자열 리소스 ID 입니다.
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState(); // 토글 상태 동기화 (햄버거 아이콘 표시)

        // NavigationView 리스너 설정
        navigationView.setNavigationItemSelectedListener(this);

        // 앱 처음 실행 시 또는 화면 회전 시 프래그먼트 로드
        if (savedInstanceState == null) {
            // 초기 화면으로 TaskListFragment 로드
            loadFragment(new TaskListFragment());
            // NavigationView 에서 '할 일 목록' 메뉴 항목을 기본 선택 상태로 표시
            navigationView.setCheckedItem(R.id.nav_task_list);
        }
    }

    // NavigationView 메뉴 아이템 클릭 시 호출될 메서드
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int itemId = item.getItemId(); // 클릭된 메뉴 아이템의 ID 가져오기

        if (itemId == R.id.nav_task_list) {
            selectedFragment = new TaskListFragment();
        } else if (itemId == R.id.nav_analysis) {
            selectedFragment = new AnalysisFragment();
        }
        // 다른 메뉴 아이템이 있다면 여기에 추가 (else if ...)

        if (selectedFragment != null) {
            loadFragment(selectedFragment); // 선택된 프래그먼트 로드
        }

        // 메뉴 아이템 클릭 후 드로어 닫기
        drawerLayout.closeDrawer(GravityCompat.START);
        return true; // 이벤트 처리 완료
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