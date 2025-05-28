package com.am.mytodolistapp.data;

import android.app.Application;
import androidx.lifecycle.LiveData;

import java.util.List;

// 앱의 할 일 데이터 관리 총괄
public class TodoRepository {

    private TodoDao mTodoDao; // 데이터베이스 접근 객체
    private LiveData<List<TodoItem>> mAllTodos; // 모든 할 일 목록 LiveData

    // 생성자: 데이터베이스 및 DAO 초기화
    public TodoRepository(Application application) {
        // Application Context 를 사용해 데이터베이스 인스턴스를 가져옴
        AppDatabase db = AppDatabase.getDatabase(application);
        // 데이터베이스 인스턴스에서 DAO 를 가져옴
        mTodoDao = db.todoDao();
        // DAO 를 통해 모든 할 일 목록 LiveData 를 가져옴 (Room 이 자동으로 백그라운드 처리)
        mAllTodos = mTodoDao.getAllTodos();
    }

    // --- 데이터 조회 ---

    public LiveData<List<TodoItem>> getAllTodos() {
        return mAllTodos;
    }//모든 할 일 목록 제공


    public void insert(TodoItem todoItem) {
        // AppDatabase 에 정의된 ExecutorService 를 사용해 백그라운드에서 DAO 의 insert 실행
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mTodoDao.insert(todoItem);
        });
    }// 역할: 새 할 일 추가 요청 (백그라운드 실행)


    public void update(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mTodoDao.update(todoItem);
        });
    }// 역할: 할 일 수정 요청 (백그라운드 실행)


    public void delete(TodoItem todoItem) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mTodoDao.delete(todoItem);
        });
    }// 할 일 삭제 요청 (백그라운드 실행)


    public void deleteAllTodos() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            mTodoDao.deleteAllTodos();
        });
    }// 모든 할 일 삭제 요청 (백그라운드 실행)

    // 역할: ID로 특정 할 일 직접 가져오기 (동기 방식 - 백그라운드 호출 필요)
    public TodoItem getTodoByIdSync(int id) {
        // DAO 의 동기 조회 메소드를 직접 호출하여 반환
        return mTodoDao.getTodoByIdSync(id);
    }
}