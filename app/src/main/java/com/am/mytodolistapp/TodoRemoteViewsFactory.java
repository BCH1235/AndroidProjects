package com.am.mytodolistapp;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.am.mytodolistapp.data.AppDatabase;
import com.am.mytodolistapp.data.TodoItem;

import java.util.ArrayList;
import java.util.List;

public class TodoRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private Context context;
    private List<TodoItem> todoList = new ArrayList<>();

    public TodoRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        loadTodos();
    }

    private void loadTodos() {
        // 동기적으로 데이터베이스에서 할 일 목록 불러오기
        AppDatabase db = AppDatabase.getDatabase(context);
        todoList = db.todoDao().getAllTodos().getValue(); // LiveData 접근은 백그라운드에서 주의 필요
        if (todoList == null) {
            todoList = new ArrayList<>();
        }
    }

    @Override
    public void onDataSetChanged() {
        AppDatabase db = AppDatabase.getDatabase(context);
        todoList = db.todoDao().getAllTodosSync();
    }


    @Override
    public void onDestroy() {
        todoList.clear();
    }

    @Override
    public int getCount() {
        return todoList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        TodoItem item = todoList.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_list_item);
        views.setTextViewText(R.id.todo_title, item.getTitle());

        // 완료 버튼 누르면 브로드캐스트 전송
        Intent completeIntent = new Intent("com.am.mytodolistapp.ACTION_COMPLETE_TODO");
        completeIntent.putExtra("TODO_ID", item.getId());
        views.setOnClickFillInIntent(R.id.todo_complete_button, completeIntent);

        return views;
    }


    @Override
    public RemoteViews getLoadingView() {
        return null; // 기본 로딩 뷰
    }

    @Override
    public int getViewTypeCount() {
        return 1; // 한 종류의 아이템 뷰만 사용
    }

    @Override
    public long getItemId(int position) {
        return todoList.get(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
