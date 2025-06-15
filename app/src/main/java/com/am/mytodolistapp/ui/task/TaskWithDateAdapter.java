package com.am.mytodolistapp.ui.task;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.TodoItem;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class TaskWithDateAdapter extends ListAdapter<TaskListViewModel.TodoWithCategory, TaskWithDateAdapter.TaskViewHolder> {

    private final TaskListViewModel viewModel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());

    public TaskWithDateAdapter(TaskListViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_todo_with_date, parent, false);
        return new TaskViewHolder(view, viewModel);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskListViewModel.TodoWithCategory todoWithCategory = getItem(position);
        holder.bind(todoWithCategory);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkboxCompleted;
        private final TextView textTodoTitle;
        private final TextView textTodoDate;
        private final ImageButton buttonEditTodo;
        private final TaskListViewModel viewModel;

        public TaskViewHolder(@NonNull View itemView, TaskListViewModel viewModel) {
            super(itemView);
            this.viewModel = viewModel;

            checkboxCompleted = itemView.findViewById(R.id.checkbox_completed);
            textTodoTitle = itemView.findViewById(R.id.text_todo_title);
            textTodoDate = itemView.findViewById(R.id.text_todo_date);
            buttonEditTodo = itemView.findViewById(R.id.button_edit_todo);

            // 편집 버튼 클릭 리스너
            buttonEditTodo.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    TaskListViewModel.TodoWithCategory todoWithCategory = getItem(position);
                    TodoItem todo = todoWithCategory.getTodoItem();

                    EditTodoDialogFragment dialogFragment = EditTodoDialogFragment.newInstance(todo);
                    if (itemView.getContext() instanceof AppCompatActivity) {
                        AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
                        dialogFragment.show(activity.getSupportFragmentManager(), "EditTodoDialog");
                    }
                }
            });

            // 할일 항목 길게 눌러서 삭제
            itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    TaskListViewModel.TodoWithCategory todoWithCategory = getItem(position);
                    TodoItem todo = todoWithCategory.getTodoItem();

                    // 삭제 확인 대화상자 표시
                    androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(itemView.getContext());
                    builder.setTitle("할일 삭제")
                            .setMessage("'" + todo.getTitle() + "'을(를) 삭제하시겠습니까?")
                            .setPositiveButton("삭제", (dialog, which) -> {
                                viewModel.delete(todo);
                                Toast.makeText(itemView.getContext(), "할일이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("취소", null)
                            .show();
                }
                return true;
            });
        }

        public void bind(TaskListViewModel.TodoWithCategory todoWithCategory) {
            TodoItem todo = todoWithCategory.getTodoItem();

            // 제목 설정
            textTodoTitle.setText(todo.getTitle());

            // 리스너 제거 후 상태 설정
            // 기존 리스너 제거 후 상태 설정
            checkboxCompleted.setOnCheckedChangeListener(null);
            checkboxCompleted.setChecked(todo.isCompleted());


            final int currentTodoId = todo.getId();
            checkboxCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    TaskListViewModel.TodoWithCategory currentItem = getItem(position);
                    if (currentItem != null &&
                            currentItem.getTodoItem().getId() == currentTodoId) {
                        viewModel.toggleCompletion(currentItem.getTodoItem());
                    }
                }
            });

            // 나머지 UI 업데이트
            setupDateDisplay(todo);
            updateCompletionUI(todo);
        }
        private void updateCompletionUI(TodoItem todo) {
            if (todo.isCompleted()) {
                textTodoTitle.setPaintFlags(textTodoTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                textTodoTitle.setAlpha(0.6f);
            } else {
                textTodoTitle.setPaintFlags(textTodoTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                textTodoTitle.setAlpha(1.0f);
            }
        }

        private void setupDateDisplay(TodoItem todo) {
            if (todo.getDueDate() == null) {
                textTodoDate.setVisibility(View.GONE);
                return;
            }

            Date dueDate = new Date(todo.getDueDate());
            Calendar dueCal = Calendar.getInstance();
            dueCal.setTime(dueDate);

            Calendar today = Calendar.getInstance();
            Calendar yesterday = Calendar.getInstance();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);

            // 오늘 날짜면 숨김
            if (isSameDay(dueCal, today)) {
                textTodoDate.setVisibility(View.GONE);
            } else {
                textTodoDate.setVisibility(View.VISIBLE);
                textTodoDate.setText(dateFormat.format(dueDate));

                // 기한이 지났으면 빨간색, 미래면 검정색
                if (dueCal.before(today)) {
                    textTodoDate.setTextColor(Color.RED);
                } else {
                    textTodoDate.setTextColor(Color.BLACK);
                }
            }
        }

        private boolean isSameDay(Calendar cal1, Calendar cal2) {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
        }
    }

    private static final DiffUtil.ItemCallback<TaskListViewModel.TodoWithCategory> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<TaskListViewModel.TodoWithCategory>() {
                @Override
                public boolean areItemsTheSame(@NonNull TaskListViewModel.TodoWithCategory oldItem,
                                               @NonNull TaskListViewModel.TodoWithCategory newItem) {
                    return oldItem.getTodoItem().getId() == newItem.getTodoItem().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull TaskListViewModel.TodoWithCategory oldItem,
                                                  @NonNull TaskListViewModel.TodoWithCategory newItem) {
                    TodoItem oldTodo = oldItem.getTodoItem();
                    TodoItem newTodo = newItem.getTodoItem();

                    return Objects.equals(oldTodo.getTitle(), newTodo.getTitle()) &&
                            oldTodo.isCompleted() == newTodo.isCompleted() &&
                            Objects.equals(oldTodo.getDueDate(), newTodo.getDueDate()) &&
                            Objects.equals(oldTodo.getCategoryId(), newTodo.getCategoryId()) &&
                            Objects.equals(oldItem.getCategoryName(), newItem.getCategoryName());
                }
            };
}