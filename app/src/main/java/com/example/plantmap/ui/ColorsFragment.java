package com.example.plantmap.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.plantmap.MainActivity;
import com.example.plantmap.R;
import com.example.plantmap.colors.ColorView;

public class ColorsFragment extends BaseFragment {
    @Override
    protected int getHelpTextResId() {
        return R.string.help_colors;
    }

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