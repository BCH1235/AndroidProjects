package com.am.mytodolistapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

            // 위치 카드 클릭 - 해당 위치의 할 일 목록으로 이동
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    RecyclerView recyclerView = (RecyclerView) itemView.getParent();
                    if (recyclerView != null && recyclerView.getAdapter() instanceof LocationListAdapter) {
                        LocationListAdapter adapter = (LocationListAdapter) recyclerView.getAdapter();
                        LocationItem location = adapter.getItem(position);

                        // 위치별 할 일 목록 Fragment로 이동
                        LocationTaskListFragment fragment = LocationTaskListFragment.newInstance(
                                location.getId(), location.getName());

                        if (itemView.getContext() instanceof AppCompatActivity) {
                            AppCompatActivity activity = (AppCompatActivity) itemView.getContext();
                            activity.getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, fragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    }
                }
            });

            // 스위치 클릭 - 위치 알림 활성화/비활성화 (수정된 부분)
            switchLocationEnabled.setOnCheckedChangeListener(null); // 기존 리스너 제거
        }

        public void bind(LocationItem location) {
            textLocationName.setText(location.getName());
            textLocationDetails.setText("반경 " + (int)location.getRadius() + "m");

            // 리스너를 null로 설정한 후 체크 상태 변경 (무한 루프 방지)
            switchLocationEnabled.setOnCheckedChangeListener(null);
            switchLocationEnabled.setChecked(location.isEnabled());

            // 새로운 리스너 설정
            switchLocationEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                // 현재 아이템의 위치를 다시 가져와서 안전하게 처리
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    RecyclerView recyclerView = (RecyclerView) itemView.getParent();
                    if (recyclerView != null && recyclerView.getAdapter() instanceof LocationListAdapter) {
                        LocationListAdapter adapter = (LocationListAdapter) recyclerView.getAdapter();
                        LocationItem currentLocation = adapter.getItem(position);
                        currentLocation.setEnabled(isChecked);
                        viewModel.updateLocation(currentLocation);
                    }
                }
            });
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
                    oldItem.getLatitude() == newItem.getLatitude() &&
                    oldItem.getLongitude() == newItem.getLongitude() &&
                    oldItem.getRadius() == newItem.getRadius() &&
                    oldItem.isEnabled() == newItem.isEnabled();
        }
    };
}