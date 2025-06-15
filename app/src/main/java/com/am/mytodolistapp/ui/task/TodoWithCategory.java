package com.am.mytodolistapp.ui.task;

import com.am.mytodolistapp.data.TodoItem;

public class TodoWithCategory {
    private final TodoItem todoItem;
    private final String categoryName;
    private final String categoryColor;

    public TodoWithCategory(TodoItem todoItem, String categoryName, String categoryColor) {
        this.todoItem = todoItem;
        this.categoryName = categoryName;
        this.categoryColor = categoryColor;
    }

    public TodoItem getTodoItem() { return todoItem; }
    public String getCategoryName() { return categoryName; }
    public String getCategoryColor() { return categoryColor; }
}