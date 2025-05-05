package com.am.mytodolistapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;

import com.am.mytodolistapp.data.AppDatabase;
import com.am.mytodolistapp.data.TodoItem;

/**
 * Implementation of App Widget functionality.
 */
public class TodoListWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.todo_list_widget);

        // 리스트뷰에 RemoteViewsService 연결
        Intent intent = new Intent(context, TodoWidgetService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))); // 고유 인식 위해 필요

        views.setRemoteAdapter(R.id.todo_list_view, intent);
        views.setEmptyView(R.id.todo_list_view, R.id.todo_list_view); // 데이터 없을 때 표시될 뷰

        // 업데이트 적용
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.todo_list_widget);

            // 어댑터 연결
            Intent intent = new Intent(context, TodoWidgetService.class);
            views.setRemoteAdapter(R.id.todo_list_view, intent);
            views.setEmptyView(R.id.todo_list_view, R.id.empty_view);

            Intent completeIntentTemplate = new Intent(context, TodoListWidget.class);
            completeIntentTemplate.setAction("com.am.mytodolistapp.ACTION_COMPLETE_TODO");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    completeIntentTemplate,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            views.setPendingIntentTemplate(R.id.todo_list_view, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if ("com.am.mytodolistapp.ACTION_COMPLETE_TODO".equals(intent.getAction())) {
            int todoId = intent.getIntExtra("TODO_ID", -1);
            if (todoId != -1) {
                AppDatabase.databaseWriteExecutor.execute(() -> {
                    TodoItem item = AppDatabase.getDatabase(context).todoDao().getTodoByIdSync(todoId);
                    if (item != null && !item.isCompleted()) {
                        item.setCompleted(true);
                        item.setCompletionTimestamp(System.currentTimeMillis());
                        AppDatabase.getDatabase(context).todoDao().update(item);

                        // 위젯 갱신
                        AppWidgetManager manager = AppWidgetManager.getInstance(context);
                        ComponentName thisWidget = new ComponentName(context, TodoListWidget.class);
                        manager.notifyAppWidgetViewDataChanged(manager.getAppWidgetIds(thisWidget), R.id.todo_list_view);
                    }
                });
            }
        }
    }

}