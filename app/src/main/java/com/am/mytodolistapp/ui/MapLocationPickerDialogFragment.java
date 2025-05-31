package com.am.mytodolistapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.LocationItem;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import java.util.Arrays;

public class MapLocationPickerDialogFragment extends DialogFragment implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private AutocompleteSupportFragment autocompleteFragment;
    private SeekBar seekBarRadius;
    private TextView textRadiusValue;
    private Button buttonCancel, buttonSave;
    private LocationBasedTaskViewModel viewModel;

    // 선택된 위치 정보
    private String selectedLocationName = "";
    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;
    private float selectedRadius = 100.0f;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(LocationBasedTaskViewModel.class);

        // Places API 초기화
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_map_location_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupMap();
        setupAutocomplete();
        setupSeekBar();
        setupButtons();
    }

    private void initViews(View view) {
        seekBarRadius = view.findViewById(R.id.seek_bar_radius);
        textRadiusValue = view.findViewById(R.id.text_radius_value);
        buttonCancel = view.findViewById(R.id.button_cancel);
        buttonSave = view.findViewById(R.id.button_save);
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void setupAutocomplete() {
        autocompleteFragment = (AutocompleteSupportFragment) getChildFragmentManager()
                .findFragmentById(R.id.autocomplete_fragment);

        if (autocompleteFragment != null) {
            // 반환받을 정보 설정
            autocompleteFragment.setPlaceFields(Arrays.asList(
                    Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
            ));

            // 장소 선택 리스너
            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    selectedLocationName = place.getName();
                    if (place.getLatLng() != null) {
                        selectedLatitude = place.getLatLng().latitude;
                        selectedLongitude = place.getLatLng().longitude;
                        updateMapMarker();
                    }
                }

                @Override
                public void onError(@NonNull com.google.android.gms.common.api.Status status) {
                    Toast.makeText(getContext(), "장소 검색 오류", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupSeekBar() {
        // 반경 범위: 50~500m
        seekBarRadius.setMin(50);
        seekBarRadius.setMax(500);
        seekBarRadius.setProgress(100);
        textRadiusValue.setText("100m");

        seekBarRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedRadius = progress;
                textRadiusValue.setText(progress + "m");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupButtons() {
        buttonCancel.setOnClickListener(v -> dismiss());

        buttonSave.setOnClickListener(v -> {
            if (selectedLocationName.isEmpty() || (selectedLatitude == 0.0 && selectedLongitude == 0.0)) {
                Toast.makeText(getContext(), "위치를 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            LocationItem newLocation = new LocationItem(selectedLocationName, selectedLatitude, selectedLongitude);
            newLocation.setRadius(selectedRadius);

            viewModel.insertLocation(newLocation);
            Toast.makeText(getContext(), "✅ 위치가 추가되었습니다", Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;

        // 기본 위치 (서울)
        LatLng seoul = new LatLng(37.5665, 126.9780);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 10));

        // 지도 클릭 리스너
        googleMap.setOnMapClickListener(latLng -> {
            selectedLatitude = latLng.latitude;
            selectedLongitude = latLng.longitude;
            selectedLocationName = "선택된 위치";
            updateMapMarker();
        });
    }

    private void updateMapMarker() {
        if (googleMap != null && selectedLatitude != 0.0 && selectedLongitude != 0.0) {
            googleMap.clear();
            LatLng latLng = new LatLng(selectedLatitude, selectedLongitude);
            googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(selectedLocationName));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int)(getResources().getDisplayMetrics().heightPixels * 0.9)
            );
        }
    }
}