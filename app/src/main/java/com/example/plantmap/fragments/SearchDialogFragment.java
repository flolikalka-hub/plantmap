package com.example.plantmap.fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.model.SearchFilter;
import com.example.plantmap.search.PlantSearchEngine;
import com.example.plantmap.viewmodel.SearchDialogViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SearchDialogFragment extends DialogFragment {
    public static final String TAG = "search_dialog";
    public static final String REQUEST_KEY = "search_request";
    public static final String RESULT_KEY = "search_result";

    private final PlantSearchEngine engine = new PlantSearchEngine();
    private SearchDialogViewModel viewModel;

    public static SearchDialogFragment newInstance(ArrayList<PlantPoint> points) {
        SearchDialogFragment fragment = new SearchDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("all_points", points);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this).get(SearchDialogViewModel.class);

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);

        AutoCompleteTextView nameInput = new AutoCompleteTextView(requireContext());
        nameInput.setHint("Название сорта");
        EditText typeInput = new EditText(requireContext());
        typeInput.setHint("Тип растения");
        EditText groupInput = new EditText(requireContext());
        groupInput.setHint("Группа растения");
        EditText potVolumeInput = new EditText(requireContext());
        potVolumeInput.setHint("Литраж горшка");
        potVolumeInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        AutoCompleteTextView flowerColorInput = new AutoCompleteTextView(requireContext());
        flowerColorInput.setHint("Цвет цветка");
        EditText addInput = new EditText(requireContext());
        addInput.setHint("Дополнительная информация");
        EditText countInput = new EditText(requireContext());
        countInput.setHint("Количество");
        countInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText dateInput = new EditText(requireContext());
        dateInput.setHint("Дата обработки");
        dateInput.setFocusable(false);
        EditText feedingDateInput = new EditText(requireContext());
        feedingDateInput.setHint("Дата подкормки");
        feedingDateInput.setFocusable(false);

        restoreText(nameInput, SearchDialogViewModel.keyName());
        restoreText(typeInput, SearchDialogViewModel.keyType());
        restoreText(groupInput, SearchDialogViewModel.keyGroup());
        restoreText(potVolumeInput, SearchDialogViewModel.keyPot());
        restoreText(flowerColorInput, SearchDialogViewModel.keyColor());
        restoreText(countInput, SearchDialogViewModel.keyCount());
        restoreText(addInput, SearchDialogViewModel.keyAdditional());

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        bindDateInput(dateInput, true, sdf);
        bindDateInput(feedingDateInput, false, sdf);

        nameInput.addTextChangedListener(watcher(SearchDialogViewModel.keyName()));
        typeInput.addTextChangedListener(watcher(SearchDialogViewModel.keyType()));
        groupInput.addTextChangedListener(watcher(SearchDialogViewModel.keyGroup()));
        potVolumeInput.addTextChangedListener(watcher(SearchDialogViewModel.keyPot()));
        flowerColorInput.addTextChangedListener(watcher(SearchDialogViewModel.keyColor()));
        countInput.addTextChangedListener(watcher(SearchDialogViewModel.keyCount()));
        addInput.addTextChangedListener(watcher(SearchDialogViewModel.keyAdditional()));

        layout.addView(nameInput);
        layout.addView(typeInput);
        layout.addView(groupInput);
        layout.addView(potVolumeInput);
        layout.addView(flowerColorInput);
        layout.addView(addInput);
        layout.addView(countInput);
        layout.addView(dateInput);
        layout.addView(feedingDateInput);
        scrollView.addView(layout);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Поиск")
                .setView(scrollView)
                .setPositiveButton("Найти", null)
                .setNegativeButton("Отменить", (d, which) -> sendResult(new HashSet<>()))
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            SearchFilter filter = viewModel.buildFilter();
            List<PlantPoint> allPoints = extractPoints();
            Set<PlantPoint> result = engine.applyFilter(allPoints, filter);
            sendResult(result);
            dismiss();
        }));

        return dialog;
    }

    private void bindDateInput(EditText dateInput, boolean processing, SimpleDateFormat sdf) {
        Long restored = processing ? viewModel.getProcessingDate() : viewModel.getFeedingDate();
        if (restored != null) {
            Calendar restoredDate = Calendar.getInstance();
            restoredDate.setTimeInMillis(restored);
            dateInput.setText(sdf.format(restoredDate.getTime()));
        }

        dateInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            Long current = processing ? viewModel.getProcessingDate() : viewModel.getFeedingDate();
            if (current != null) {
                calendar.setTimeInMillis(current);
            }
            DatePickerDialog picker = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, dayOfMonth, 0, 0, 0);
                        selected.set(Calendar.MILLISECOND, 0);
                        long millis = selected.getTimeInMillis();
                        if (processing) {
                            viewModel.setProcessingDate(millis);
                        } else {
                            viewModel.setFeedingDate(millis);
                        }
                        dateInput.setText(sdf.format(selected.getTime()));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            picker.show();
        });

        dateInput.setOnLongClickListener(v -> {
            if (processing) {
                viewModel.setProcessingDate(null);
            } else {
                viewModel.setFeedingDate(null);
            }
            dateInput.setText("");
            return true;
        });
    }

    private void restoreText(EditText input, String key) {
        input.setText(viewModel.getText(key));
    }

    private TextWatcher watcher(String key) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setText(key, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        };
    }

    @SuppressWarnings("unchecked")
    private List<PlantPoint> extractPoints() {
        Bundle args = getArguments();
        if (args == null) {
            return new ArrayList<>();
        }
        ArrayList<PlantPoint> points = (ArrayList<PlantPoint>) args.getSerializable("all_points");
        return points == null ? new ArrayList<>() : points;
    }

    private void sendResult(Set<PlantPoint> result) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(RESULT_KEY, new HashSet<>(result));
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, bundle);
    }
}