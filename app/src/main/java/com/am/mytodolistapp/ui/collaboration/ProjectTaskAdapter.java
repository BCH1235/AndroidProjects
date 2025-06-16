package com.am.mytodolistapp.ui.collaboration;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.firebase.ProjectTask;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;


// 프로젝트 할 일 목록을 RecyclerView에 표시하기 위한 어댑터
// 각 할 일의 완료 토글, 수정, 삭제 이벤트를 처리
public class ProjectTaskAdapter extends ListAdapter<ProjectTask, ProjectTaskAdapter.TaskViewHolder> {

    private final OnTaskActionListener onToggleCompleteListener;
    private final OnTaskActionListener onEditListener;
    private final OnTaskActionListener onDeleteListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd", Locale.KOREAN);

    public interface OnTaskActionListener {
        void onAction(ProjectTask task);
    }

    public ProjectTaskAdapter(OnTaskActionListener onToggleCompleteListener,
                              OnTaskActionListener onEditListener,
                              OnTaskActionListener onDeleteListener) {
        super(DIFF_CALLBACK);
        this.onToggleCompleteListener = onToggleCompleteListener;
        this.onEditListener = onEditListener;
        this.onDeleteListener = onDeleteListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 통합된 할 일 아이템 레이아웃을 사용
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_todo_unified, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        ProjectTask task = getItem(position);
        holder.bind(task);
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final CheckBox checkBoxCompleted;
        private final TextView textTitle;
        private final TextView textDetails;
        private final ImageButton buttonEdit;
        private final ImageButton buttonDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBoxCompleted = itemView.findViewById(R.id.checkbox_completed);
            textTitle = itemView.findViewById(R.id.text_todo_title);
            textDetails = itemView.findViewById(R.id.text_todo_details);
            buttonEdit = itemView.findViewById(R.id.button_edit_todo);
            buttonDelete = itemView.findViewById(R.id.button_delete_todo);
        }


        public void bind(ProjectTask task) {
            textTitle.setText(task.getTitle());
            updateDetailsText(task);
            applyCompletionStyle(task.isCompleted());
            setupListeners(task);
        }// ProjectTask 데이터를 뷰에 바인딩하고 리스너를 설정

        private void updateDetailsText(ProjectTask task) {
            StringBuilder details = new StringBuilder();

            if (task.getDueDate() != null) {
                details.append("기한: ").append(dateFormat.format(new Date(task.getDueDate())));
            }

            if (details.length() > 0) {
                textDetails.setText(details.toString());
                textDetails.setVisibility(View.VISIBLE);
            } else {
                textDetails.setVisibility(View.GONE);
            }
        }// 할 일의 상세 정보를 TextView에 표시

        private void applyCompletionStyle(boolean isCompleted) {
            if (isCompleted) {
                textTitle.setPaintFlags(textTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                textTitle.setAlpha(0.6f);
                textDetails.setAlpha(0.6f);
            } else {
                textTitle.setPaintFlags(textTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                textTitle.setAlpha(1.0f);
                textDetails.setAlpha(1.0f);
            }
            checkBoxCompleted.setChecked(isCompleted);
        } // 할 일의 완료 상태에 따라 텍스트에 취소선을 적용

        private void setupListeners(ProjectTask task) {
            checkBoxCompleted.setOnClickListener(v -> {
                if (onToggleCompleteListener != null) onToggleCompleteListener.onAction(task);
            });
            buttonEdit.setOnClickListener(v -> {
                if (onEditListener != null) onEditListener.onAction(task);
            });
            buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog(task));
        }// 체크박스, 수정 버튼, 삭제 버튼에 대한 클릭 리스너를 설정

        private void showDeleteConfirmationDialog(ProjectTask task) {
            new AlertDialog.Builder(itemView.getContext())
                    .setTitle("할 일 삭제")
                    .setMessage("'" + task.getTitle() + "' 항목을 삭제하시겠습니까?")
                    .setPositiveButton("삭제", (dialog, which) -> {
                        if (onDeleteListener != null) {
                            onDeleteListener.onAction(task);
                            Toast.makeText(itemView.getContext(), "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("취소", null)
                    .show();
        }
    }

    private static final DiffUtil.ItemCallback<ProjectTask> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ProjectTask>() {
                @Override
                public boolean areItemsTheSame(@NonNull ProjectTask oldItem, @NonNull ProjectTask newItem) {
                    return Objects.equals(oldItem.getTaskId(), newItem.getTaskId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull ProjectTask oldItem, @NonNull ProjectTask newItem) {

                    return Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                            Objects.equals(oldItem.getContent(), newItem.getContent()) &&
                            oldItem.isCompleted() == newItem.isCompleted() &&
                            Objects.equals(oldItem.getDueDate(), newItem.getDueDate()) &&
                            oldItem.getUpdatedAt() == newItem.getUpdatedAt();
                }
            };
}