package com.example.plantmap.stats;

import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.example.plantmap.R;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.model.StatItem;
import com.example.plantmap.plant.PlantRepository;
import com.example.plantmap.plant.PlantUniversalForm;
import com.example.plantmap.util.InputValidators;
import com.example.plantmap.util.LayoutUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Представление раздела статистики (StatsFragment).
 * Строит интерфейс с карточками StatItem: секции можно раскрывать,
 * конечные пункты запускают диалоги с результатами.
 *
 * Результаты (списки точек) можно отобразить на плане через
 * {@link OnShowOnPlanListener}.
 */
public class StatisticsView {
    private Context context;
    private PlantRepository plantRepository;
    private OnShowOnPlanListener showOnPlanListener;

    /**
     * Слушатель для отображения набора точек на плане.
     * Вызывается, когда пользователь нажимает "Показать на плане" в диалоге результата.
     */
    public interface OnShowOnPlanListener {
        void onShowOnPlan(Set<PlantPoint> points);
    }

    public StatisticsView(Context context,
                          PlantRepository plantRepository,
                          OnShowOnPlanListener listener) {
        this.context = context;
        this.plantRepository = plantRepository;
        this.showOnPlanListener = listener;
    }

    /**
     * Создаёт корневое View со списком карточек статистики.
     */
    public View createView() {
        LayoutUtils.ScrollableLayout scrollableLayout = LayoutUtils.createVerticalScrollView(context);
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

        addStatItems(scrollableLayout.layout, stats);
        return scrollableLayout.scrollView;
    }

    /**
     * Рекурсивно добавляет элементы StatItem в контейнер.
     * Секции отрисовываются как раскрывающиеся карточки со стрелкой.
     */
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

    /**
     * Показывает общее количество растений на плане.
     */
    private void showResultAllPlantsDialog() {
        int total = plantRepository.getTotalPlantCount();
        new AlertDialog.Builder(context)
                .setTitle("Всего растений:")
                .setMessage(String.valueOf(total))
                .setPositiveButton("Понятно", null)
                .show();
    }

    /**
     * Открывает форму фильтрации и выводит количество растений по параметрам.
     */
    private void showFilteredCountDialog() {
        PlantUniversalForm form = new PlantUniversalForm(context, plantRepository);
        form.setMode(PlantUniversalForm.MODE_SEARCH);

        LayoutUtils.ScrollableLayout scrollableLayout = LayoutUtils.createVerticalScrollView(context);
        scrollableLayout.layout.addView(form.getView());

        new AlertDialog.Builder(context)
                .setTitle("Фильтры растений")
                .setView(scrollableLayout.scrollView)
                .setPositiveButton("Применить", (dialog, which) -> {
                    String name = form.getNameInput().getText().toString();
                    String type = form.getTypeInput().getText().toString();
                    String group = form.getGroupInput().getText().toString();

                    Integer color = null;
                    Integer potVolume = InputValidators.validatePositiveOptionalInt(form.getPotVolumeInput());
                    if (potVolume == null && !form.getPotVolumeInput().getText().toString().trim().isEmpty()) {
                        return;
                    }

                    boolean allEmpty = name.isEmpty() && type.isEmpty() && group.isEmpty()
                            && color == null && potVolume == null;
                    if (allEmpty) return;

                    int count = plantRepository.getFilteredPlantCount(name, type, group, color, potVolume);

                    new AlertDialog.Builder(context)
                            .setTitle("Количество растений:")
                            .setMessage(String.valueOf(count))
                            .setPositiveButton("Понятно", null)
                            .show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    /**
     * Показывает список растений, которые ни разу не обрабатывались.
     */
    private void showResultNeverProcDialog() {
        List<PlantPoint> neverProcessed = plantRepository.getNeverProcessedPoints();
        if (neverProcessed.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle("Не обработанные растения")
                    .setMessage("Таких растений нет")
                    .setPositiveButton("Понятно", null)
                    .show();
            return;
        }
        showResultDialog("Не обрабатывались никогда", neverProcessed);
    }

    /**
     * Показывает список растений, которые ни разу не подкармливались.
     */
    private void showResultNeverFeedingDialog() {
        List<PlantPoint> neverFeeding = plantRepository.getNeverFeedingPoints();
        if (neverFeeding.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle("Не подкармливались растения")
                    .setMessage("Таких растений нет")
                    .setPositiveButton("Понятно", null)
                    .show();
            return;
        }
        showResultDialog("Не подкармливались никогда", neverFeeding);
    }

    /**
     * Запрашивает количество дней и показывает растения, не обрабатывавшиеся дольше этого срока.
     */
    private void showDaysDialog() {
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

    /**
     * Запрашивает количество дней и показывает растения, не подкармливавшиеся дольше этого срока.
     */
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
        List<PlantPoint> oldPoints = plantRepository.getNotProcessedMoreThanDays(days);
        if (oldPoints.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle("Не обрабатывались более " + days + " дней")
                    .setMessage("Таких растений нет")
                    .setPositiveButton("Понятно", null)
                    .show();
            return;
        }
        showResultDialog("Не обрабатывались более " + days + " дней", oldPoints);
    }

    private void showOldFeedingPoints(int days) {
        List<PlantPoint> oldPoints = plantRepository.getNotFeedingMoreThanDays(days);
        if (oldPoints.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle("Не подкармливались более " + days + " дней")
                    .setMessage("Таких растений нет")
                    .setPositiveButton("Понятно", null)
                    .show();
            return;
        }
        showResultDialog("Не подкармливались более " + days + " дней", oldPoints);
    }

    /**
     * Показывает диалог со списком растений (название + количество).
     * Предлагает кнопку "Показать на плане", которая вызывает
     * {@link OnShowOnPlanListener#onShowOnPlan(Set)}.
     */
    private void showResultDialog(String dialogTitle, List<PlantPoint> resPoints) {
        StringBuilder sb = new StringBuilder();
        for (PlantPoint p : resPoints) {
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
                    Set<PlantPoint> resultSet = new HashSet<>(resPoints);
                    if (showOnPlanListener != null) {
                        showOnPlanListener.onShowOnPlan(resultSet);
                    }
                })
                .show();
    }
}