package com.example.plantmap.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.plantmap.MainActivity;
import com.example.plantmap.R;
import com.example.plantmap.stats.StatisticsView;

public class StatsFragment extends BaseFragment {
    @Override
    protected int getHelpTextResId() {
        return R.string.help_stats;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        StatisticsView statisticsView = new StatisticsView(
                requireContext(),
                ((MainActivity) requireActivity()).getRepository(),
                points -> ((MainActivity) requireActivity()).openPlanWithResults(points)
        );
        return statisticsView.createView();
    }
}
