package com.am.mytodolistapp;

import android.app.Application;

import com.jakewharton.threetenabp.AndroidThreeTen;

// 앱 전체의 시작점 및 전역 초기화를 위한 클래스
public class MyTodoApplication extends Application {


    @Override
    public void onCreate() {
        super.onCreate();
        // 앱 전체에서 사용할 라이브러리 등 초기화 작업

        // ThreeTenABP (Joda-Time 백포트) 라이브러리 초기화
        AndroidThreeTen.init(this);
    }
}