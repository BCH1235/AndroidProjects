package com.am.mytodolistapp.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.am.mytodolistapp.data.TodoItem;
import com.am.mytodolistapp.data.TodoRepository;

import java.util.List;

// TaskListFragment 등 UI 컨트롤러에 필요한 데이터 제공 및 UI 관련 로직 처리
public class TaskListViewModel extends AndroidViewModel {

    private TodoRepository mRepository;// 데이터 저장소 접근 객체
    private final LiveData<List<TodoItem>> mAllTodos; // UI 가 관찰할 모든 할 일 목록

    // 생성자: Repository 초기화 및 모든 할 일 목록 LiveData 로드
    public TaskListViewModel (Application application) {
        super(application);
        mRepository = new TodoRepository(application); // Repository 인스턴스 생성
        mAllTodos = mRepository.getAllTodos();  // Repository 에서 모든 할 일 목록 가져오기
    }

    // UI 에 모든 할 일 목록 LiveData 제공
    public LiveData<List<TodoItem>> getAllTodos() {
        return mAllTodos;
    }



    // UI 로부터 새 할 일 삽입 요청 처리
    public void insert(TodoItem todoItem) {
        mRepository.insert(todoItem);
    }

    // UI 로부터 할 일 수정 요청 처리
    public void update(TodoItem todoItem) {
        mRepository.update(todoItem);
    }

    // UI 로부터 할 일 삭제 요청 처리
    public void delete(TodoItem todoItem) {
        mRepository.delete(todoItem);
    }

    // UI 로부터 모든 할 일 삭제 요청 처리
    public void deleteAllTodos() {
        mRepository.deleteAllTodos();
    }


}