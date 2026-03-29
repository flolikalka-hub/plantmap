package com.example.plantmap.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.plantmap.MainActivity;
import com.example.plantmap.colors.ColorView;

public class ColorsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ColorView colorView = new ColorView(
                requireContext(),
                ((MainActivity) requireActivity()).getRepository().getColorDataAccess()
        );
        return colorView.createColorView();
    }
}