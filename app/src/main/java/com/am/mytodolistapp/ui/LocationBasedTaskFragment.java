package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.LocationItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class LocationBasedTaskFragment extends Fragment {

    private RecyclerView recyclerViewLocations;
    private LocationListAdapter locationAdapter;
    private FloatingActionButton fabAddLocation;
    private LocationBasedTaskViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_location_based_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LocationBasedTaskViewModel.class);

        // UI 초기화
        recyclerViewLocations = view.findViewById(R.id.recycler_view_locations);
        fabAddLocation = view.findViewById(R.id.fab_add_location);

        // RecyclerView 설정
        recyclerViewLocations.setLayoutManager(new LinearLayoutManager(getContext()));
        locationAdapter = new LocationListAdapter(viewModel);
        recyclerViewLocations.setAdapter(locationAdapter);

        // 스와이프로 삭제 기능 추가
        setupSwipeToDelete();

        // FAB 클릭 - 새로운 지도 기반 위치 추가
        fabAddLocation.setOnClickListener(v -> {
            MapLocationPickerDialogFragment dialog = new MapLocationPickerDialogFragment();
            dialog.show(requireActivity().getSupportFragmentManager(), "MapLocationPickerDialog");
        });

        // 위치 목록 관찰
        viewModel.getAllLocations().observe(getViewLifecycleOwner(), locations -> {
            locationAdapter.submitList(locations);
        });
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    LocationItem swipedLocation = locationAdapter.getCurrentList().get(position);

                    // 위치 삭제
                    viewModel.deleteLocation(swipedLocation);

                    // 실행 취소 스낵바 표시
                    Snackbar.make(recyclerViewLocations, "\"" + swipedLocation.getName() + "\" 위치가 삭제되었습니다", Snackbar.LENGTH_LONG)
                            .setAction("실행 취소", v -> viewModel.insertLocation(swipedLocation))
                            .show();
                }
            }
        }).attachToRecyclerView(recyclerViewLocations);
    }
}