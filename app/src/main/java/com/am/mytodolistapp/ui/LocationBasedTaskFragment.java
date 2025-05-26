package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

// 위치별 할 일 관리 메인 화면
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

        // FAB 클릭 - 새 위치 추가
        fabAddLocation.setOnClickListener(v -> {
            AddLocationDialogFragment dialog = new AddLocationDialogFragment();
            dialog.show(requireActivity().getSupportFragmentManager(), "AddLocationDialog");
        });

        // 위치 목록 관찰
        viewModel.getAllLocations().observe(getViewLifecycleOwner(), locations -> {
            locationAdapter.submitList(locations);
        });
    }
}