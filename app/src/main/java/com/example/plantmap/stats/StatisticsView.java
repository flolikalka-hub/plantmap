package com.example.plantmap.stats;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.example.plantmap.R;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.model.StatItem;
import com.example.plantmap.plant.PlantRepository;
import com.example.plantmap.plant.PlantUniversalForm;
import com.example.plantmap.plan.PlanView;

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
                "Проверка давности обработки (от вредителей и болезней) и подкормки растений",
                List.of(
                        new StatItem("Не обрабатывались более ... дней",
                                "Введите количество дней",
                                true,
                                () -> showDaysDialog()),
                        new StatItem("Не обрабатывались никогда",
                                "",
                                false,
                                () -> showResultNeverProcDialog()),
                        new StatItem("Не подкармливались более ... дней",
                                "Введите количество дней",
                                true,
                                () -> showFeedingDaysDialog()),

                        new StatItem("Не подкармливались никогда",
                                "",
                                false,
                                () -> showResultNeverFeedingDialog())
                )
        ));

        // Секция "Количество"
        stats.add(new StatItem(
                "Количество",
                "Подсчет количества растений в наличии на территории",
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

                    boolean allEmpty =
                            name.isEmpty() &&
                                    type.isEmpty() &&
                                    group.isEmpty() &&
                                    color.isEmpty() &&
                                    potVolume == null;

                    if (allEmpty) {
                        // просто ничего не делаем
                        return;
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

        String dialogTitle = "Не обрабатывались никогда";
        showResultDialog(dialogTitle, neverProcessed);
    }

    private void showResultNeverFeedingDialog() {

        List<PlantPoint> neverFeeding =
                plantRepository.getNeverFeedingPoints();

        if (neverFeeding.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle("Не подкармливались растения")
                    .setMessage("Таких растений нет")
                    .setPositiveButton("Понятно", null)
                    .show();
            return;
        }

        String dialogTitle = "Не подкармливались никогда";
        showResultDialog(dialogTitle, neverFeeding);
    }

    private  void  showDaysDialog() {
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Количество дней");

        new AlertDialog.Builder(context)
                .setTitle("Не обрабатывались более...")
                .setView(input)
                .setPositiveButton("Показать", (dialog, which) -> {

                    String text = input.getText().toString();

                    if (text.isEmpty()) return;

                    int days = Integer.parseInt(text);

                    showOldProcessedPoints(days);

                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showFeedingDaysDialog() {
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Количество дней");

        new AlertDialog.Builder(context)
                .setTitle("Не подкармливались более...")
                .setView(input)
                .setPositiveButton("Показать", (dialog, which) -> {

                    String text = input.getText().toString();

                    if (text.isEmpty()) return;

                    int days = Integer.parseInt(text);

                    showOldFeedingPoints(days);

                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showOldProcessedPoints(int days) {

        List<PlantPoint> oldPoints =
                plantRepository.getNotProcessedMoreThanDays(days);

        if (oldPoints.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle("Не обрабатывались более " + days + " дней")
                    .setMessage("Таких растений нет")
                    .setPositiveButton("Понятно", null)
                    .show();
            return;
        }

        String dialogTitle = "Не обрабатывались более " + days + " дней";
        showResultDialog(dialogTitle, oldPoints);
    }

    private void showOldFeedingPoints(int days) {

        List<PlantPoint> oldPoints =
                plantRepository.getNotFeedingMoreThanDays(days);

        if (oldPoints.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle("Не подкармливались более " + days + " дней")
                    .setMessage("Таких растений нет")
                    .setPositiveButton("Понятно", null)
                    .show();
            return;
        }

        String dialogTitle = "Не подкармливались более " + days + " дней";
        showResultDialog(dialogTitle, oldPoints);
    }

    private void showResultDialog(
            String dialogTitle,
            List<PlantPoint> resPoints
    ) {
        StringBuilder sb = new StringBuilder();

        for (PlantPoint p : resPoints) {
            //Log.d("CHECK", "Stat id: " + p.id);
            sb.append(p.plant.name)
                    .append(" (")
                    .append(p.count)
                    .append(")")
                    .append("\n");
        }
        new AlertDialog.Builder(context)
                .setTitle(dialogTitle)
                .setMessage(sb.toString())
                .setPositiveButton("Закрыть", null)
                .setNeutralButton("Показать на плане", (d, w) -> {

                    Set<PlantPoint> resultSet =
                            new HashSet<>(resPoints);

                    if (showOnPlanListener != null) {
                        showOnPlanListener.onShowOnPlan(resultSet);
                    }
                })
                .show();
    }

    public interface OnShowOnPlanListener {
        void onShowOnPlan(Set<PlantPoint> points);
    }
}