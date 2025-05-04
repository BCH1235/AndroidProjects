package com.am.mytodolistapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.am.mytodolistapp.ui.TaskListFragment;

// 앱의 메인 화면 역할을 하는 액티비티
public class MainActivity extends AppCompatActivity {

    // 액티비티가 처음 생성될 때 호출
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // activity_main.xml 레이아웃을 화면에 표시
        setContentView(R.layout.activity_main);


        // 화면 회전 등으로 액티비티가 재생성될 때 Fragment 가 중복 추가되는 것을 방지
        if (savedInstanceState == null) {
            // Fragment 를 관리하는 객체 가져오기
            FragmentManager fragmentManager = getSupportFragmentManager();
            // Fragment 변경 작업 시작
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            // 표시할 첫 화면 Fragment 생성
            TaskListFragment taskListFragment = new TaskListFragment();

            // 레이아웃의 fragment_container 영역에 taskListFragment 추가
            fragmentTransaction.add(R.id.fragment_container, taskListFragment);
            // 변경 사항 확정 및 적용
            fragmentTransaction.commit();
        }
    }
}