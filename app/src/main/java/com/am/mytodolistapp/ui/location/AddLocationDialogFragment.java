package com.am.mytodolistapp.ui.location;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.am.mytodolistapp.R;
import com.am.mytodolistapp.data.LocationItem;

public class AddLocationDialogFragment extends DialogFragment {

    private EditText editLocationName, editLatitude, editLongitude;
    private SeekBar seekBarRadius;
    private TextView textRadiusValue;
    private Button buttonCancel, buttonAdd;
    private LocationBasedTaskViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(LocationBasedTaskViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_add_location, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // UI 요소 찾기
        editLocationName = view.findViewById(R.id.edit_location_name);
        editLatitude = view.findViewById(R.id.edit_latitude);
        editLongitude = view.findViewById(R.id.edit_longitude);
        seekBarRadius = view.findViewById(R.id.seek_bar_radius);
        textRadiusValue = view.findViewById(R.id.text_radius_value);
        buttonCancel = view.findViewById(R.id.button_cancel);
        buttonAdd = view.findViewById(R.id.button_add);

        // SeekBar 설정
        seekBarRadius.setMin(50);
        seekBarRadius.setMax(500);
        seekBarRadius.setProgress(100);
        textRadiusValue.setText("100m");

        seekBarRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                textRadiusValue.setText(progress + "m");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 버튼 이벤트
        buttonCancel.setOnClickListener(v -> dismiss());
        buttonAdd.setOnClickListener(v -> addLocation());

        editLocationName.requestFocus();
    }

    private void addLocation() {
        String name = editLocationName.getText().toString().trim();
        String latStr = editLatitude.getText().toString().trim();
        String lngStr = editLongitude.getText().toString().trim();

        if (name.isEmpty() || latStr.isEmpty() || lngStr.isEmpty()) {
            Toast.makeText(getContext(), "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double latitude = Double.parseDouble(latStr);
            double longitude = Double.parseDouble(lngStr);
            float radius = seekBarRadius.getProgress();

            LocationItem newLocation = new LocationItem(name, latitude, longitude);
            newLocation.setRadius(radius);

            viewModel.insertLocation(newLocation);
            dismiss();
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "위도/경도는 숫자로 입력해주세요.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}