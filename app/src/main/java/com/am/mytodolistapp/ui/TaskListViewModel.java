package com.am.mytodolistapp.ui;

import android.app.Application; // Application 임포트
import androidx.lifecycle.AndroidViewModel; // ViewModel 대신 AndroidViewModel 사용
import androidx.lifecycle.LiveData;

import com.am.mytodolistapp.data.TodoItem; // TodoItem 임포트
import com.am.mytodolistapp.data.TodoRepository; // TodoRepository 임포트
import com.am.mytodolistapp.data.AppDatabase; // AppDatabase 임포트 (ExecutorService 사용 위해)

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

    // UI 로부터 특정 할 일을 완료 상태로 변경 요청 처리 (실제 소요 시간 포함)
    public void markAsComplete(int todoId, int actualMinutes) {
        // 데이터베이스 작업은 백그라운드 스레드에서 실행
        AppDatabase.databaseWriteExecutor.execute(() -> {
            // 해당 ID 의 할 일 객체를 동기적으로 가져옴
            TodoItem itemToComplete = mRepository.getTodoByIdSync(todoId);

            if (itemToComplete != null) {
                // 객체 상태 변경 (완료, 실제 시간, 완료 시각 설정)
                itemToComplete.setCompleted(true);
                itemToComplete.setActualTimeMinutes(actualMinutes);
                itemToComplete.setCompletionTimestamp(System.currentTimeMillis());

                // 변경된 객체로 데이터베이스 업데이트 요청
                mRepository.update(itemToComplete);
            }
            // ID 에 해당하는 아이템이 없을 경우의 처리는 생략됨
        });
    }
}