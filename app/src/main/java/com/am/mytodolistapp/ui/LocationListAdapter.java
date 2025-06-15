package com.am.mytodolistapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.LocationItem;

public class LocationListAdapter extends ListAdapter<LocationItem, LocationListAdapter.LocationViewHolder> {

    private final LocationBasedTaskViewModel viewModel;

    public LocationListAdapter(LocationBasedTaskViewModel viewModel) {
        super(DIFF_CALLBACK);
        this.viewModel = viewModel;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_location, parent, false);
        return new LocationViewHolder(itemView, viewModel);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {
        private final TextView textLocationName;
        private final TextView textLocationDetails;
        private final Switch switchLocationEnabled;
        private final LocationBasedTaskViewModel viewModel;

        public LocationViewHolder(@NonNull View itemView, LocationBasedTaskViewModel viewModel) {
            super(itemView);
            this.viewModel = viewModel;

            textLocationName = itemView.findViewById(R.id.text_location_name);
            textLocationDetails = itemView.findViewById(R.id.text_location_details);
            switchLocationEnabled = itemView.findViewById(R.id.switch_location_enabled);

            setupClickListeners();
        }

        private void setupClickListeners() {
            // 위치 카드 클릭 - 해당 위치의 할 일 목록으로 이동
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    LocationItem location = getLocationFromPosition(position);
                    if (location != null) {
                        navigateToLocationTasks(location);
                    }
                }
            });

            // 길게 눌러서 삭제 기능 추가
            itemView.setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    LocationItem location = getLocationFromPosition(position);
                    if (location != null) {
                        showDeleteConfirmationDialog(location);
                    }
                }
                return true; // 이벤트 소비됨을 표시
            });

            // 스위치 초기 리스너 제거 (bind에서 설정됨)
            switchLocationEnabled.setOnCheckedChangeListener(null);
        }

        private LocationItem getLocationFromPosition(int position) {
            RecyclerView recyclerView = (RecyclerView) itemView.getParent();
            if (recyclerView != null && recyclerView.getAdapter() instanceof LocationListAdapter) {
                LocationListAdapter adapter = (LocationListAdapter) recyclerView.getAdapter();
                if (position < adapter.getCurrentList().size()) {
                    return adapter.getItem(position);
                }
            }
            return null;
        }

        private void navigateToLocationTasks(LocationItem location) {
            if (itemView.getContext() instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) itemView.getContext();

                LocationTaskListFragment fragment = LocationTaskListFragment.newInstance(
                        location.getId(), location.getName());

                activity.getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        }

        private void showDeleteConfirmationDialog(LocationItem location) {
            // 먼저 해당 위치의 할 일 개수를 확인
            viewModel.getLocationTodoCount(location.getId(), count -> {
                String message;
                if (count > 0) {
                    message = "'" + location.getName() + "' 위치에는 " + count + "개의 할 일이 있습니다.\n\n" +
                            "위치를 삭제하면 관련된 모든 할 일도 함께 삭제됩니다.\n\n" +
                            "정말 삭제하시겠습니까?";
                } else {
                    message = "'" + location.getName() + "' 위치를 삭제하시겠습니까?";
                }

                new AlertDialog.Builder(itemView.getContext())
                        .setTitle("위치 삭제 확인")
                        .setMessage(message)
                        .setPositiveButton("삭제", (dialog, which) -> {
                            viewModel.deleteLocationWithTodos(location);

                            String toastMessage = "'" + location.getName() + "' 위치가 삭제되었습니다";
                            if (count > 0) {
                                toastMessage += " (" + count + "개 할 일 포함)";
                            }
                            Toast.makeText(itemView.getContext(), toastMessage, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("취소", null)
                        .show();
            });
        }

        private void handleLocationToggle(LocationItem location, boolean isChecked) {
            if (location == null) return;

            // 위치 활성화/비활성화 시 사용자에게 피드백 제공
            String message = isChecked ?
                    location.getName() + " 위치 알림이 활성화되었습니다" :
                    location.getName() + " 위치 알림이 비활성화되었습니다";

            location.setEnabled(isChecked);
            viewModel.updateLocation(location);

            Toast.makeText(itemView.getContext(), message, Toast.LENGTH_SHORT).show();
        }

        public void bind(LocationItem location) {
            if (location == null) return;

            // 위치 이름 설정
            textLocationName.setText(location.getName());

            // 위치 세부정보 설정 (반경과 좌표 정보)
            String detailText = String.format("반경 %dm\n%.6f, %.6f",
                    (int) location.getRadius(),
                    location.getLatitude(),
                    location.getLongitude());
            textLocationDetails.setText(detailText);

            // 스위치 상태 설정 (무한 루프 방지를 위해 리스너를 null로 설정 후 상태 변경)
            switchLocationEnabled.setOnCheckedChangeListener(null);
            switchLocationEnabled.setChecked(location.isEnabled());

            // 새로운 리스너 설정
            switchLocationEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // 현재 아이템의 위치를 다시 가져와서 안전하게 처리
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    LocationItem currentLocation = getLocationFromPosition(position);
                    if (currentLocation != null) {
                        handleLocationToggle(currentLocation, isChecked);
                    }
                }
            });

            // 위치 활성화 상태에 따른 시각적 피드백
            float alpha = location.isEnabled() ? 1.0f : 0.6f;
            textLocationName.setAlpha(alpha);
            textLocationDetails.setAlpha(alpha);
        }
    }

    private static final DiffUtil.ItemCallback<LocationItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<LocationItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull LocationItem oldItem, @NonNull LocationItem newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull LocationItem oldItem, @NonNull LocationItem newItem) {
            return oldItem.getName().equals(newItem.getName()) &&
                    Double.compare(oldItem.getLatitude(), newItem.getLatitude()) == 0 &&
                    Double.compare(oldItem.getLongitude(), newItem.getLongitude()) == 0 &&
                    Float.compare(oldItem.getRadius(), newItem.getRadius()) == 0 &&
                    oldItem.isEnabled() == newItem.isEnabled();
        }
    };
}