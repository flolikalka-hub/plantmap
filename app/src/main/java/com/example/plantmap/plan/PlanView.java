package com.example.plantmap.plan;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.plantmap.R;
import com.example.plantmap.db.yandex_images.PlantPhotoLoader;
import com.example.plantmap.plant.PlantRepository;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.search.PlantSearchDialog;
import com.example.plantmap.search.PlantSearchEngine;
import com.example.plantmap.plant.PlantDialogs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Основное View для отображения интерактивного плана территории.
 * Поддерживает:
 * - Просмотр информации о растениях (тап по точке)
 * - Добавление новых точек (режим ADD_POINT) с привязкой к сетке
 * - Редактирование точек (режим EDIT_POINT): изменение количества, перемещение, удаление
 * - Масштабирование (pinch-to-zoom) и панорамирование (drag) плана
 * - Подсветку результатов поиска
 *
 * Все координаты точек хранятся в эталонных единицах плана (plan coordinates),
 * не зависящих от текущего масштаба и размера экрана.
 */
public class PlanView extends View {

    // --- Визуальные константы ---
    /** Плотность экрана целевого устройства (A16), используется для расчёта размеров. */
    //private static float density = 2.8125f;
    /** Радиус точки при отрисовке (в единицах плана). */
    //private static final float POINT_RADIUS = 4f / density;
    private static final float POINT_RADIUS = 1.42f;
    /** Радиус попадания в точку при касании (для удобства). */
    private static final float HIT_RADIUS = POINT_RADIUS * 3f;
    /** Размер текста номера точки. */
    //private float textSize = 7f/ density;
    private float textSize = 2.48f;
    /** Толщина линии обводки точек. */
    //private float strokeWidth = 3f/ density;
    private float strokeWidth = 1.06f;
    /** Отступ обводки найденных точек. */
    //private float indentStroke = 2f/ density;
    private float indentStroke = 0.71f;

    /** Шаг сетки по X (в единицах плана) для режимов добавления/редактирования. */
    private static final float GRID_STEP_X = POINT_RADIUS * 2f;
    /** Шаг сетки по Y. */
    private static final float GRID_STEP_Y = POINT_RADIUS * 2f;

    // --- Графика и кисти ---
    private Paint paint;
    private Drawable planDrawable;
    private Paint gridPaint;
    private Paint searchStrokePaint;

    // --- Геометрия плана ---
    /** Фактическая ширина плана на экране (с учётом паддингов). */
    private float planWidth;
    /** Фактическая высота плана (пропорционально ширине). */
    private float planHeight;
    /** Масштаб относительно оригинального размера изображения. */
    private float planScale;
    /** Оригинальная ширина изображения плана. */
    private float planOriginalWidth;
    /** Оригинальная высота изображения плана. */
    private float planOriginalHeight;

    // --- Данные ---
    /** Точки, отображаемые на плане. */
    private ArrayList<PlantPoint> points;
    private PlantRepository repository;

    // --- Режимы и состояние взаимодействия ---
    private EditMode currentMode = EditMode.VIEW;
    private PlantPoint draggedPoint = null;
    private PlantPoint pressedPoint = null;
    private PlantPoint selectedPoint = null;
    private boolean isDragging = false;
    private boolean isDraggingPlan = false;
    private float downX, downY;
    private final float touchSlop;

    // --- Масштабирование и прокрутка ---
    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.0f;
    private final float MIN_SCALE = 1.0f;
    private final float MAX_SCALE = 10.0f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private float focusX, focusY;
    /** Флаг, что текущий жест уже обработан (масштабирование или перетаскивание). */
    private boolean gestureConsumed = false;

    // --- Поиск ---
    private boolean searchActive = false;
    private final Set<PlantPoint> searchResultsSet = new HashSet<>();

    // --- Слушатели ---
    /** Слушатель событий начала/окончания поиска. */
    public interface SearchStateListener {
        void onSearchApplied();
        void onSearchCleared();
    }
    private SearchStateListener searchStateListener;
    private PlantSearchEngine searchEngine = new PlantSearchEngine();

    // Отступы (padding), устанавливаемые извне
    private int pl, pr, pt;

    /**
     * Конструктор. Инициализирует кисти, детектор жестов, загружает точки из БД.
     */
    public PlanView(Context context, PlantRepository repository) {
        super(context);
        this.repository = repository;

        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        // Кисть для точек
        paint = new Paint();
        paint.setColor(ContextCompat.getColor(context, R.color.default_color));
        paint.setStrokeWidth(strokeWidth);

        // Кисть для обводки найденных точек
        searchStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        searchStrokePaint.setStyle(Paint.Style.STROKE);
        searchStrokePaint.setStrokeWidth(strokeWidth);
        searchStrokePaint.setColor(ContextCompat.getColor(context, R.color.search_highlight));

        // Изображение плана
        planDrawable = ContextCompat.getDrawable(context, R.drawable.terr_plan);
        // Размеры плана в эталонных единицах (совпадают с координатами точек)
        planOriginalWidth = 453f;
        planOriginalHeight = 687f;

        // Кисть для сетки
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(ContextCompat.getColor(context, R.color.grid_color));

        // Загружаем точки из репозитория
        points = new ArrayList<>();
        points.addAll(repository.getAllPoints());

        updatePlanGeometry();
    }

    // --- Режимы ---
    public void setEditMode(EditMode mode) {
        this.currentMode = mode;
        invalidate();
    }

    // --- Геометрия ---
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updatePlanGeometry();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            updatePlanGeometry();
        }
    }

    /**
     * Пересчитывает размеры и масштаб плана на основе текущих размеров View и отступов.
     */
    private void updatePlanGeometry() {
        pl = getPaddingLeft();
        pr = getPaddingRight();
        pt = getPaddingTop();
        planWidth = Math.max(0f, getWidth() - pl - pr);
        if (planOriginalWidth > 0f) {
            planScale = planWidth / planOriginalWidth;
            planHeight = planOriginalHeight * planScale;
        }
    }

    /**
     * Преобразует экранные координаты в координаты плана (с учётом смещения и масштаба).
     */
    private float[] screenToPlan(float x, float y) {
        float planX = (x - offsetX - pl) / (planScale * scaleFactor);
        float planY = (y - offsetY - pt) / (planScale * scaleFactor);
        return new float[]{planX, planY};
    }

    /**
     * Проверяет, что точка (в координатах плана) находится внутри границ плана.
     */
    private boolean isInsidePlan(float planX, float planY) {
        float r = POINT_RADIUS;
        return planX >= r &&
                planY >= r &&
                planX <= planOriginalWidth - r &&
                planY <= planOriginalHeight - r;
    }

    // --- Отрисовка ---
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scaleFactor, scaleFactor);

        // План
        if (planDrawable != null) {
            planDrawable.setBounds(pl, (int) pt, pl + (int) planWidth, (int) (pt + planHeight));
            planDrawable.draw(canvas);
        }

        // Сетка (только в режимах добавления/редактирования)
        if (currentMode == EditMode.ADD_POINT || currentMode == EditMode.EDIT_POINT) {
            for (float x = 0; x <= planOriginalWidth; x += GRID_STEP_X) {
                float screenX = pl + x * planScale;
                canvas.drawLine(screenX, pt, screenX, pt + planHeight, gridPaint);
            }
            for (float y = 0; y <= planOriginalHeight; y += GRID_STEP_Y) {
                float screenY = pt + y * planScale;
                canvas.drawLine(pl, screenY, pl + planWidth, screenY, gridPaint);
            }
        }

        // Точки
        // TODO: Вынести создание Paint за пределы onDraw для повышения производительности
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(ContextCompat.getColor(getContext(), R.color.point_text_default));
        textPaint.setTextSize(textSize * planScale);
        textPaint.setTextAlign(Paint.Align.LEFT);

        for (PlantPoint p : points) {
            if (p.plant == null) continue;

            // Выбор цвета в зависимости от выделения
            if (p == selectedPoint) {
                paint.setColor(ContextCompat.getColor(getContext(), R.color.point_selected));
                textPaint.setColor(ContextCompat.getColor(getContext(), R.color.point_text_selected));
            } else {
                paint.setColor(ContextCompat.getColor(getContext(), R.color.point_default));
                textPaint.setColor(ContextCompat.getColor(getContext(), R.color.point_text_default));
            }

            boolean isFound = searchActive && searchResultsSet.contains(p);
            float screenX = pl + p.x * planScale;
            float screenY = pt + p.y * planScale;

            // Обводка найденных точек
            if (isFound) {
                float screenRadius = POINT_RADIUS * planScale + indentStroke;
                canvas.drawCircle(screenX, screenY, screenRadius, searchStrokePaint);
            }

            // Сама точка
            float screenRadius = POINT_RADIUS * planScale;
            canvas.drawCircle(screenX, screenY, screenRadius, paint);

            // Текст (количество)
            String text = String.valueOf(p.count);
            Rect bounds = new Rect();
            textPaint.getTextBounds(text, 0, text.length(), bounds);
            float textX = screenX - bounds.width() / 2f;
            float textY = screenY + bounds.height() / 2f;
            canvas.drawText(text, textX, textY, textPaint);
        }
        canvas.restore();
    }

    // --- Обработка касаний ---
    // TODO разбить на несколько методов поменьше
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (planScale == 0f) return false;
        scaleDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                gestureConsumed = false;
                downX = x;
                downY = y;
                isDragging = false;
                isDraggingPlan = false;
                if (currentMode == EditMode.EDIT_POINT || currentMode == EditMode.VIEW) {
                    float[] p = screenToPlan(x, y);
                    pressedPoint = findPointAt(p[0], p[1]);
                    selectedPoint = pressedPoint;
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = x - downX;
                float dy = y - downY;
                if (currentMode == EditMode.EDIT_POINT && pressedPoint != null) {
                    if (!isDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                        draggedPoint = pressedPoint;
                        isDragging = true;
                    }
                    if (isDragging && draggedPoint != null) {
                        gestureConsumed = true;
                        float dxPlan = dx / (planScale * scaleFactor);
                        float dyPlan = dy / (planScale * scaleFactor);
                        float newX = draggedPoint.x + dxPlan;
                        float newY = draggedPoint.y + dyPlan;
                        if (isInsidePlan(newX, newY)) {
                            draggedPoint.setX(newX);
                            draggedPoint.setY(newY);
                        }
                        downX = x;
                        downY = y;
                        invalidate();
                    }
                } else if (!isDraggingPlan) {
                    if (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop) {
                        isDraggingPlan = true;
                    }
                }
                if (isDraggingPlan) {
                    gestureConsumed = true;
                    offsetX += dx;
                    offsetY += dy;
                    float maxOffsetX = 0;
                    float maxOffsetY = 0;
                    float minOffsetX = getWidth() - planWidth * scaleFactor;
                    float minOffsetY = getHeight() - planHeight * scaleFactor;
                    offsetX = Math.min(maxOffsetX, Math.max(offsetX, minOffsetX));
                    offsetY = Math.min(maxOffsetY, Math.max(offsetY, minOffsetY));
                    downX = x;
                    downY = y;
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (!gestureConsumed) {
                    float[] p = screenToPlan(x, y);
                    if (currentMode == EditMode.VIEW) {
                        PlantPoint tapped = findPointAt(p[0], p[1]);
                        selectedPoint = tapped;
                        invalidate();
                        if (tapped != null) showPlantInfo(tapped);
                    } else {
                        handleTap(p[0], p[1]);
                    }
                } else if (draggedPoint != null) {
                    // Привязка к сетке после перетаскивания
                    float snappedX = Math.round(draggedPoint.x / GRID_STEP_X) * GRID_STEP_X;
                    float snappedY = Math.round(draggedPoint.y / GRID_STEP_Y) * GRID_STEP_Y;
                    if (isInsidePlan(snappedX, snappedY) && !isPointAt(snappedX, snappedY, draggedPoint)) {
                        draggedPoint.setX(snappedX);
                        draggedPoint.setY(snappedY);
                    }
                    if (draggedPoint.id != 0) {
                        repository.updatePoint(draggedPoint.id, draggedPoint);
                    }
                }
                draggedPoint = null;
                pressedPoint = null;
                isDragging = false;
                isDraggingPlan = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Обработка тапа: добавление точки или вызов меню редактирования.
     */
    private void handleTap(float x, float y) {
        if (currentMode == EditMode.ADD_POINT) {
            float snappedX = Math.round(x / GRID_STEP_X) * GRID_STEP_X;
            float snappedY = Math.round(y / GRID_STEP_Y) * GRID_STEP_Y;
            if (!isInsidePlan(x, y)) {
                showOutOfPlanWarning();
                return;
            }
            if (isPointAt(snappedX, snappedY)) {
                showNodeOccupiedWarning();
                return;
            }
            PlantPoint pendingPoint = new PlantPoint(snappedX, snappedY);
            PlantDialogs.showNewPlantDialog(getContext(), pendingPoint, repository, () -> {
                points.add(pendingPoint);
                invalidate();
            });
        } else if (currentMode == EditMode.EDIT_POINT) {
            if (pressedPoint != null && !isDragging) {
                PlantPoint tappedPoint = pressedPoint;
                if (!points.contains(tappedPoint)) {
                    pressedPoint = null;
                    return;
                }
                PlantDialogs.showEditPointDialog(
                        getContext(), tappedPoint, repository,
                        () -> showDeleteConfirmation(tappedPoint),
                        () -> invalidate()
                );
            }
        }
    }

    /**
     * Проверяет, есть ли точка в указанных координатах (с допуском).
     */
    private boolean isPointAt(float x, float y) {
        float tolerance = 2f;
        for (PlantPoint p : points) {
            if (Math.abs(p.x - x) < tolerance && Math.abs(p.y - y) < tolerance) {
                return true;
            }
        }
        return false;
    }

    /**
     * Проверяет занятость узла, исключая заданную точку.
     */
    private boolean isPointAt(float x, float y, PlantPoint exclude) {
        float tolerance = 2f;
        for (PlantPoint p : points) {
            if (p == exclude) continue;
            if (Math.abs(p.x - x) < tolerance && Math.abs(p.y - y) < tolerance) {
                return true;
            }
        }
        return false;
    }

    private void showNodeOccupiedWarning() {
        new AlertDialog.Builder(getContext())
                .setTitle("Невозможно создать точку")
                .setMessage("В этом узле сетки уже есть точка. Выберите другой узел.")
                .setPositiveButton("ОК", null)
                .show();
    }

    private void showDeleteConfirmation(PlantPoint point) {
        new AlertDialog.Builder(getContext())
                .setTitle("Удаление точки")
                .setMessage("Вы уверены, что хотите удалить эту точку?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    points.remove(point);
                    if (selectedPoint == point) selectedPoint = null;
                    if (draggedPoint == point) draggedPoint = null;
                    if (point.id != 0) repository.deletePoint(point.id);
                    invalidate();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showOutOfPlanWarning() {
        new AlertDialog.Builder(getContext())
                .setTitle("Нельзя создать точку")
                .setMessage("Точку можно создавать только внутри плана.")
                .setPositiveButton("Понятно", null)
                .show();
    }

    /**
     * Отображает диалог с информацией о растении в выбранной точке.
     * Использует кастомный макет dialog_plant_info
     */
    private void showPlantInfo(PlantPoint point) {
        Context context = getContext();
        if (point == null || point.plant == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String dateStr = (point.processingDate != null) ? sdf.format(new Date(point.processingDate)) : "";
        String feedingDateStr = (point.feedingDate != null) ? sdf.format(new Date(point.feedingDate)) : "";
        String potVolumeStr = (point.potVolume != null) ? point.potVolume.toString() : "";
        String colorStr = "";
        if (point.plant.flowerColorId != null) {
            String colorName = repository.getColorIdToNameMap().get(point.plant.flowerColorId);
            colorStr = (colorName != null) ? colorName : "";
        }
        String addInfoStr = (point.plant.additionalInfo != null) ? point.plant.additionalInfo : "";

        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_plant_info, null);

        TextView tvName = dialogView.findViewById(R.id.tv_name);
        TextView tvType = dialogView.findViewById(R.id.tv_type);
        TextView tvGroup = dialogView.findViewById(R.id.tv_group);
        TextView tvPotVolume = dialogView.findViewById(R.id.tv_pot_volume);
        TextView tvColor = dialogView.findViewById(R.id.tv_color);
        TextView tvAdditional = dialogView.findViewById(R.id.tv_additional);
        TextView tvCount = dialogView.findViewById(R.id.tv_count);
        TextView tvProcessingDate = dialogView.findViewById(R.id.tv_processing_date);
        TextView tvFeedingDate = dialogView.findViewById(R.id.tv_feeding_date);
        ImageView ivPhoto = dialogView.findViewById(R.id.iv_plant_photo);
        Button btnClose = dialogView.findViewById(R.id.btn_close);

        tvName.append(point.plant.name);
        tvType.append(point.plant.type);
        tvGroup.append(point.plant.group);
        tvPotVolume.append(potVolumeStr);
        tvColor.append(colorStr);
        tvAdditional.append(addInfoStr);
        tvCount.append(String.valueOf(point.count));
        tvProcessingDate.append(dateStr);
        tvFeedingDate.append(feedingDateStr);

        // Загрузка фото
        if (point.plant.imagePublicKey != null && !point.plant.imagePublicKey.isEmpty()) {
            ivPhoto.setVisibility(View.VISIBLE);
            PlantPhotoLoader.loadPlantPhoto(context, point.plant, ivPhoto, null);
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Информация о растении")
                .setView(dialogView)
                .create();
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * Поиск точки под указанными координатами плана.
     * Возвращает ближайшую точку в пределах HIT_RADIUS.
     */
    private PlantPoint findPointAt(float touchX, float touchY) {
        PlantPoint closest = null;
        float minDistanceSq = Float.MAX_VALUE;
        for (PlantPoint p : points) {
            if (p.plant == null) continue;
            float dx = touchX - p.x;
            float dy = touchY - p.y;
            float distanceSquared = dx * dx + dy * dy;
            float scaledHitRad = HIT_RADIUS * HIT_RADIUS / scaleFactor;
            if (distanceSquared <= scaledHitRad) {
                if (distanceSquared < minDistanceSq) {
                    minDistanceSq = distanceSquared;
                    closest = p;
                }
            }
        }
        return closest;
    }

    // --- Масштабирование ---
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            gestureConsumed = true;
            float prevScale = scaleFactor;
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_SCALE, Math.min(scaleFactor, MAX_SCALE));
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();
            offsetX = (offsetX - focusX) * (scaleFactor / prevScale) + focusX;
            offsetY = (offsetY - focusY) * (scaleFactor / prevScale) + focusY;
            invalidate();
            return true;
        }
    }

    // --- Публичные методы ---

    /** Перезагружает все точки из БД, сбрасывает выделение. */
    public void reloadPoints() {
        points.clear();
        points.addAll(repository.getAllPoints());
        selectedPoint = null;
        draggedPoint = null;
        pressedPoint = null;
        invalidate();
    }

    /** Устанавливает результаты поиска и активирует подсветку. */
    public void setSearchResults(Set<PlantPoint> results) {
        searchResultsSet.clear();
        searchResultsSet.addAll(results);
        searchActive = !searchResultsSet.isEmpty();
        if (searchStateListener != null) {
            if (searchActive) searchStateListener.onSearchApplied();
            else searchStateListener.onSearchCleared();
        }
        invalidate();
    }

    /** Снимает подсветку поиска. */
    public void clearSearch() {
        searchActive = false;
        searchResultsSet.clear();
        if (searchStateListener != null) searchStateListener.onSearchCleared();
        invalidate();
    }

    public void setSearchStateListener(SearchStateListener listener) {
        this.searchStateListener = listener;
    }

    /** Открывает диалог расширенного поиска. */
    public void showSearchDialog() {
        PlantSearchDialog.showAdvancedSearchDialog(
                getContext(),
                points,
                searchEngine,
                repository,
                new PlantSearchDialog.OnSearchListener() {
                    @Override
                    public void onSearchApplied(Set<PlantPoint> result) {
                        setSearchResults(result);
                    }
                    @Override
                    public void onSearchCleared() {
                        clearSearch();
                    }
                }
        );
    }

    public boolean isSearchActive() {
        return searchActive;
    }
}