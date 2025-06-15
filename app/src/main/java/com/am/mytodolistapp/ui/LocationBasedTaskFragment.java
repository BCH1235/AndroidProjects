package com.am.mytodolistapp.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location; // <<--- 중요! 이 import 구문이 올바른 클래스를 가리킵니다.
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.LocationItem;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class LocationBasedTaskFragment extends Fragment {

    private RecyclerView recyclerViewLocations;
    private LocationListAdapter locationAdapter;
    private FloatingActionButton fabAddLocation;
    private LocationBasedTaskViewModel viewModel;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_location_based_task, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LocationBasedTaskViewModel.class);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                // [수정된 부분] 올바른 Location 클래스를 사용합니다.
                for (Location location : locationResult.getLocations()) {
                    Log.d("LocationUpdate", "Fragment에서 위치 업데이트 감지: " + location.getLatitude() + ", " + location.getLongitude());
                }
            }
        };

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

    @Override
    public void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Geofence 테스트를 위해 위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10초
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000) // 최소 5초
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        Log.d("LocationBasedTask", "주기적 위치 업데이트를 시작합니다.");
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        Log.d("LocationBasedTask", "주기적 위치 업데이트를 중지합니다.");
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

                    viewModel.deleteLocation(swipedLocation);

                    Snackbar.make(recyclerViewLocations, "\"" + swipedLocation.getName() + "\" 위치가 삭제되었습니다", Snackbar.LENGTH_LONG)
                            .setAction("실행 취소", v -> viewModel.insertLocation(swipedLocation))
                            .show();
                }
            }
        }).attachToRecyclerView(recyclerViewLocations);
    }
}