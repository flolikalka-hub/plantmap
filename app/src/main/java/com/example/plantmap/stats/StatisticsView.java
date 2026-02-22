package com.example.plantmap.stats;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.example.plantmap.R;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.model.StatItem;
import com.example.plantmap.plant.PlantRepository;
import com.example.plantmap.plant.PlantUniversalForm;
import com.example.plantmap.view.PlanView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StatisticsView {
    private Context context;
    private PlantRepository plantRepository;
    private PlanView planView;
    private OnShowOnPlanListener showOnPlanListener;

    public StatisticsView(Context context,
                          PlantRepository plantRepository,
                          OnShowOnPlanListener listener) {
        this.context = context;
        this.plantRepository = plantRepository;
        this.showOnPlanListener = listener;
    }

    public View createView() {
        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        List<StatItem> stats = new ArrayList<>();

        // Секция "Уход"
        stats.add(new StatItem(
                "Уход",
                "Проверка давности обработки (прополка, обрезка и т.д.) растений",
                List.of(
                        new StatItem("Не обрабатывались более ... дней",
                                "Введите количество дней",
                                true,
                                () -> showDaysDialog()),
                        new StatItem("Не обрабатывались никогда",
                                "",
                                false,
                                () -> showResultNeverProcDialog())
                )
        ));

        // Секция "Количество"
        stats.add(new StatItem(
                "Количество",
                "Подсчет количества растений в наличии",
                List.of(
                        new StatItem("Всего растений",
                                "",
                                false,
                                () -> showResultAllPlantsDialog()),
                        new StatItem("Всего по параметрам",
                                "Можно задать фильтры (цвет, сорт и т.д.)",
                                true,
                                () -> showFilteredCountDialog())
                )
        ));

        addStatItems(rootLayout, stats);

        // оборачиваем в ScrollView
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.addView(rootLayout);

        return scrollView;
    }

    private void addStatItems(LinearLayout parent, List<StatItem> items) {
        for (StatItem item : items) {
            CardView card = (CardView) LayoutInflater.from(context)
                    .inflate(R.layout.stat_item, parent, false);

            TextView titleView = card.findViewById(R.id.statRequest);
            TextView subtitleView = card.findViewById(R.id.requestInfo);

            titleView.setText(item.title);
            subtitleView.setText(item.subtitle != null ? item.subtitle : "");

            parent.addView(card); // сначала карточка

            if (item.isSection()) {
                LinearLayout childrenLayout = new LinearLayout(context);
                childrenLayout.setOrientation(LinearLayout.VERTICAL);
                childrenLayout.setVisibility(View.GONE);
                parent.addView(childrenLayout); // сразу после карточки

                addStatItems(childrenLayout, item.children);

                ImageView arrow = card.findViewById(R.id.statArrow);
                arrow.setVisibility(View.VISIBLE);

                card.setOnClickListener(v -> {
                    boolean expanded = childrenLayout.getVisibility() == View.VISIBLE;
                    childrenLayout.setVisibility(expanded ? View.GONE : View.VISIBLE);
                    arrow.setRotation(expanded ? 0 : 90);
                });
            } else {
                ImageView arrow = card.findViewById(R.id.statArrow);
                arrow.setVisibility(View.GONE);

                card.setOnClickListener(v -> {
                    if (item.action != null) item.action.run();
                });
            }
        }
    }

    // потом добавить "Подробнее", где будет список всех этих растений (т.е. какое растение и количество)
    private void showResultAllPlantsDialog() {
        int total = plantRepository.getTotalPlantCount();
        new AlertDialog.Builder(context)
                .setTitle("Всего растений:")
                .setMessage(String.valueOf(total))
                .setPositiveButton("Понятно", null)
                .show();
    }

    // по фильтрам подсчет растений
    private  void  showFilteredCountDialog() {
        PlantUniversalForm form = new PlantUniversalForm(context, plantRepository);

        // сборка
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(form.getView());

        // оборачиваем в скролл, чтобы в альбомной поля можно было посмотреть
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.addView(layout);

        // диалог
        new AlertDialog.Builder(context)
                .setTitle("Фильтры растений")
                .setView(scrollView)
                .setPositiveButton("Применить", (dialog, which) -> {
                    // собираем параметры
                    String name = form.getNameInput().getText().toString();
                    String type = form.getTypeInput().getText().toString();
                    String group = form.getGroupInput().getText().toString();
                    String color = form.getFlowerColorInput().getText().toString();
                    Integer potVolume = null;
                    String potVolumeStr = form.getPotVolumeInput().getText().toString();
                    if (!potVolumeStr.isEmpty()) {
                        potVolume = Integer.parseInt(potVolumeStr);
                    }

                    // вызываем метод репозитория для подсчёта
                    int count = plantRepository.getFilteredPlantCount(name, type, group, color, potVolume);

                    // показываем результат
                    new AlertDialog.Builder(context)
                            .setTitle("Количество растений:")
                            .setMessage(String.valueOf(count))
                            .setPositiveButton("Понятно", null)
                            .show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showResultNeverProcDialog() {

        List<PlantPoint> neverProcessed =
                plantRepository.getNeverProcessedPoints();

        if (neverProcessed.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle("Не обработанные растения")
                    .setMessage("Таких растений нет")
                    .setPositiveButton("Понятно", null)
                    .show();
            return;
        }

        StringBuilder sb = new StringBuilder();

        for (PlantPoint p : neverProcessed) {
            //Log.d("CHECK", "Stat id: " + p.id);
            sb.append(p.plant.name)
                    .append(" (")
                    .append(p.count)
                    .append(")")
                    .append("\n");
        }

        new AlertDialog.Builder(context)
                .setTitle("Не обрабатывались никогда")
                .setMessage(sb.toString())
                .setPositiveButton("Закрыть", null)
                .setNeutralButton("Показать на плане", (d, w) -> {

                    Set<PlantPoint> resultSet =
                            new HashSet<>(neverProcessed);

                    if (showOnPlanListener != null) {
                        showOnPlanListener.onShowOnPlan(resultSet);
                    }
                })
                .show();
    }
    private  void  showDaysDialog() {
        new AlertDialog.Builder(context)
                .setTitle("Дни")
                .setMessage("Тут дни будут")
                .setPositiveButton("Ок", null)
                .show();
    }

    public interface OnShowOnPlanListener {
        void onShowOnPlan(Set<PlantPoint> points);
    }
}