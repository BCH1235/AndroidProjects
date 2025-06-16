package com.am.mytodolistapp.ui.location;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

    private static final String TAG = "LocationBasedTaskFragment";

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

        // ViewModel 초기화
        initViewModel();

        // 위치 서비스 초기화
        initLocationServices();

        // UI 초기화
        initViews(view);

        // RecyclerView 설정
        setupRecyclerView();

        // 스와이프 삭제 기능 설정
        setupSwipeToDelete();

        // FAB 클릭 리스너 설정
        setupFabClickListener();

        // 데이터 관찰 시작
        observeData();
    }

    private void initViewModel() {
        viewModel = new ViewModelProvider(this).get(LocationBasedTaskViewModel.class);

        // 위치 삭제 확인 리스너 설정
        viewModel.setOnLocationDeleteListener(new LocationBasedTaskViewModel.OnLocationDeleteListener() {
            @Override
            public void onLocationDeleteConfirmed(LocationItem location, int todoCount) {
                showDeleteConfirmationDialog(location, todoCount);
            }

            @Override
            public void onLocationDeleteCancelled() {
                // 삭제 취소 시 특별한 처리는 없음
                Log.d(TAG, "Location deletion cancelled");
            }
        });
    }

    private void initLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "Fragment에서 위치 업데이트 감지: " +
                            location.getLatitude() + ", " + location.getLongitude() +
                            " (정확도: " + location.getAccuracy() + "m)");
                }
            }
        };
    }

    private void initViews(View view) {
        recyclerViewLocations = view.findViewById(R.id.recycler_view_locations);
        fabAddLocation = view.findViewById(R.id.fab_add_location);
    }

    private void setupRecyclerView() {
        recyclerViewLocations.setLayoutManager(new LinearLayoutManager(getContext()));
        locationAdapter = new LocationListAdapter(viewModel);
        recyclerViewLocations.setAdapter(locationAdapter);
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false; // 드래그 이동은 지원하지 않음
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < locationAdapter.getCurrentList().size()) {
                    LocationItem swipedLocation = locationAdapter.getCurrentList().get(position);

                    // 안전한 삭제 진행 (확인 다이얼로그 포함)
                    viewModel.deleteLocationSafely(swipedLocation);
                }
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);

                // 스와이프 중일 때 시각적 피드백
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && viewHolder != null) {
                    viewHolder.itemView.setAlpha(0.7f);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);

                // 스와이프가 끝났을 때 원래 상태로 복원
                viewHolder.itemView.setAlpha(1.0f);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewLocations);
    }

    private void setupFabClickListener() {
        fabAddLocation.setOnClickListener(v -> {
            MapLocationPickerDialogFragment dialog = new MapLocationPickerDialogFragment();
            dialog.show(requireActivity().getSupportFragmentManager(), "MapLocationPickerDialog");
        });
    }

    private void observeData() {
        // 위치 목록 관찰
        viewModel.getAllLocations().observe(getViewLifecycleOwner(), locations -> {
            locationAdapter.submitList(locations);

            // 빈 목록 처리
            if (locations == null || locations.isEmpty()) {
                // 빈 상태 UI 표시 로직 (필요한 경우 구현)
                Log.d(TAG, "No locations available");
            } else {
                Log.d(TAG, "Loaded " + locations.size() + " locations");
            }
        });
    }

    private void showDeleteConfirmationDialog(LocationItem location, int todoCount) {
        String message;
        if (todoCount > 0) {
            message = "'" + location.getName() + "' 위치에는 " + todoCount + "개의 할 일이 있습니다.\n\n" +
                    "위치를 삭제하면 관련된 모든 할 일도 함께 삭제됩니다.\n\n" +
                    "정말 삭제하시겠습니까?";
        } else {
            message = "'" + location.getName() + "' 위치를 삭제하시겠습니까?";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("위치 삭제 확인")
                .setMessage(message)
                .setPositiveButton("삭제", (dialog, which) -> {
                    // 실제 삭제 실행
                    viewModel.deleteLocationWithTodos(location);

                    // 삭제 완료 피드백
                    showLocationDeletedSnackbar(location, todoCount);
                })
                .setNegativeButton("취소", (dialog, which) -> {
                    // 삭제 취소 시 리스트 새로고침
                    locationAdapter.notifyDataSetChanged();
                })
                .setOnCancelListener(dialog -> {
                    // 다이얼로그 취소 시에도 리스트 새로고침
                    locationAdapter.notifyDataSetChanged();
                })
                .show();
    }

    private void showLocationDeletedSnackbar(LocationItem location, int todoCount) {
        String message = "'" + location.getName() + "' 위치가 삭제되었습니다";
        if (todoCount > 0) {
            message += " (" + todoCount + "개 할 일 포함)";
        }

        Snackbar.make(recyclerViewLocations, message, Snackbar.LENGTH_LONG)
                .setAction("실행 취소", v -> {

                    Toast.makeText(getContext(), "삭제된 위치와 할 일은 복원할 수 없습니다.", Toast.LENGTH_SHORT).show();
                })
                .setAnchorView(fabAddLocation)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        startLocationUpdates();

        // Geofence 재초기화
        if (viewModel != null) {
            viewModel.reinitializeGeofences();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "위치 기반 기능을 위해 위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10초
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(5000) // 최소 5초
                    .setMaxUpdateDelayMillis(15000) // 최대 15초 지연
                    .build();

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            Log.d(TAG, "위치 업데이트 시작됨");

        } catch (SecurityException e) {
            Log.e(TAG, "위치 권한이 없습니다", e);
            Toast.makeText(getContext(), "위치 권한을 확인해주세요.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "위치 업데이트 시작 실패", e);
        }
    }

    private void stopLocationUpdates() {
        try {
            if (fusedLocationClient != null && locationCallback != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
                Log.d(TAG, "위치 업데이트 중지됨");
            }
        } catch (Exception e) {
            Log.e(TAG, "위치 업데이트 중지 실패", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // 리소스 정리
        stopLocationUpdates();

        // ViewModel 리스너 해제
        if (viewModel != null) {
            viewModel.setOnLocationDeleteListener(null);
        }

        Log.d(TAG, "LocationBasedTaskFragment 뷰가 해제됨");
    }
}