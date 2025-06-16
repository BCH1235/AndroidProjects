package com.am.mytodolistapp.ui.location;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText; // EditText import 추가
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
    private EditText editLocationName; // 위치 이름 입력란 추가
    private SeekBar seekBarRadius;
    private TextView textRadiusValue;
    private Button buttonCancel, buttonSave;
    private LocationBasedTaskViewModel viewModel;

    // 선택된 위치 정보
    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;
    private float selectedRadius = 100.0f;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(LocationBasedTaskViewModel.class);

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
        editLocationName = view.findViewById(R.id.edit_location_name); //  EditText 초기화
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
            autocompleteFragment.setPlaceFields(Arrays.asList(
                    Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS
            ));

            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    //  선택된 장소의 이름을 EditText에 설정
                    if (place.getName() != null) {
                        editLocationName.setText(place.getName());
                    }
                    if (place.getLatLng() != null) {
                        selectedLatitude = place.getLatLng().latitude;
                        selectedLongitude = place.getLatLng().longitude;
                        updateMapMarker(place.getName());
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
            //  EditText에서 위치 이름을 가져오도록 변경
            String finalLocationName = editLocationName.getText().toString().trim();

            if (finalLocationName.isEmpty()) {
                Toast.makeText(getContext(), "위치 이름을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedLatitude == 0.0 && selectedLongitude == 0.0) {
                Toast.makeText(getContext(), "지도를 탭하거나 장소를 검색하여 위치를 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            LocationItem newLocation = new LocationItem(finalLocationName, selectedLatitude, selectedLongitude);
            newLocation.setRadius(selectedRadius);

            viewModel.insertLocation(newLocation);
            Toast.makeText(getContext(), "✅ 위치가 추가되었습니다", Toast.LENGTH_SHORT).show();
            dismiss();
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        LatLng seoul = new LatLng(37.5665, 126.9780);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 10));

        googleMap.setOnMapClickListener(latLng -> {
            selectedLatitude = latLng.latitude;
            selectedLongitude = latLng.longitude;
            String defaultName = "선택된 위치";
            editLocationName.setText(defaultName); // 지도 클릭 시 기본 이름 설정
            editLocationName.requestFocus(); //  바로 수정할 수 있도록 포커스 이동
            updateMapMarker(defaultName);
        });
    }

    //  마커 제목을 파라미터로 받도록 변경
    private void updateMapMarker(String markerTitle) {
        if (googleMap != null && selectedLatitude != 0.0 && selectedLongitude != 0.0) {
            googleMap.clear();
            LatLng latLng = new LatLng(selectedLatitude, selectedLongitude);
            googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(markerTitle));
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