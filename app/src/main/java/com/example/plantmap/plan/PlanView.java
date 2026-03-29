package com.example.plantmap.plan;

import android.app.AlertDialog;
import android.content.Context; //объект среды
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View; //базовый визуальный элемент
import android.view.ViewConfiguration;

import androidx.core.content.ContextCompat;

import com.example.plantmap.R;
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

//extends view - значит planview частный случай view
public class PlanView extends View {
    private Paint paint; //кисточка

    private Drawable planDrawable; // план территории
    private float planWidth, planHeight;   // реальные размеры плана на экране
    private float planScale;               // масштаб плана относительно его оригинального размера
    private float planOriginalWidth, planOriginalHeight; // размеры Drawable

    private ArrayList<PlantPoint> points; // точки на плане
    private static final float POINT_RADIUS = 15f; // константа радиуса для рисования
    private static final float HIT_RADIUS = 30f; // константа для попаданию в точку, чтобы не мучаться с "пиксель в пиксель"
    private EditMode currentMode = EditMode.VIEW; // режимы, по умолчанию ПРОСМОТР
    private PlantPoint draggedPoint = null; // перетаскивание (в режиме рпадактирования зажал - перетащил - отпустил)
    private PlantPoint pressedPoint = null; // просто выделенная
    private boolean isDragging = false; // флаг перемещения
    private float downX, downY; // коорд
    private final float touchSlop; // чтобы не было "ложного" перетаскивания из-за помех
    private PlantPoint selectedPoint = null; // выделение выбранной точки

    private PlantRepository repository; // все связанное с БД вынесено из view

    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.0f;  // текущий масштаб
    private final float MIN_SCALE = 1.0f;  // минимальный масштаб
    private final float MAX_SCALE = 5.0f;  // максимальный масштаб
    private float offsetX = 0f; // смещение плана по X
    private float offsetY = 0f; // смещение плана по Y
    private boolean isDraggingPlan = false; // флаг движения плана
    private float focusX;
    private float focusY;
    private boolean gestureConsumed = false; // подтверждение действия для разграничения режмных и масштабирования
    // поиск (не режим, а состояние отображения)
    private boolean searchActive = false;
    private String searchQuery = "";
    private final Set<PlantPoint> searchResultsSet = new HashSet<>();
    private Paint searchStrokePaint; // ободка для точек, чтобы не конфликтовать с режимными окрасами

    // а был ли поиск вообще
    public interface SearchStateListener {
        void onSearchApplied();

        void onSearchCleared();
    }

    private SearchStateListener searchStateListener;
    private PlantSearchEngine searchEngine = new PlantSearchEngine();


    // ИНФОРМАТИВНОЕ
    // отступы устройства
    private int pl;
    private int pr;
    private int pt;
    private int pb;


    //                                  КОНСТРУКТОР, вызывается в момент создания view
    public PlanView(Context context, PlantRepository repository) {
        // контекст - дступ к ресурсам, экрану, системе, БД ("где я живу")
        super(context);

        this.repository = repository;

        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        touchSlop = ViewConfiguration
                .get(context)
                .getScaledTouchSlop();

        // кисти
        // точки
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5f);        // толщина линии 5 пикселей
        // обводка поиска
        searchStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        searchStrokePaint.setStyle(Paint.Style.STROKE);
        searchStrokePaint.setStrokeWidth(5f);
        searchStrokePaint.setColor(Color.MAGENTA);


        // план территории
        planDrawable = ContextCompat.getDrawable(context, R.drawable.terr_plan);
        // получаем оригинальные размеры
        planOriginalWidth = planDrawable.getIntrinsicWidth();
        planOriginalHeight = planDrawable.getIntrinsicHeight();

        // точки
        points = new ArrayList<>(); // инициализируем
        points.clear();
        points.addAll(repository.getAllPoints());

        updatePlanGeometry();
    }


    //                                  МЕТОДЫ КЛАССА
    // РЕЖИМЫ
    public void setEditMode(EditMode mode) {
        this.currentMode = mode;
    }

    // считаем размеры один раз
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

    private void updatePlanGeometry() {
        pl = getPaddingLeft();
        pr = getPaddingRight();
        pt = getPaddingTop();
        pb = getPaddingBottom();
        // ПЛАН
        // ширина под экран
        planWidth = Math.max(0f, getWidth() - pl - pr);
        if (planOriginalWidth > 0f) {
            // сохраняем пропорции
            planScale = planWidth / planOriginalWidth;
            planHeight = planOriginalHeight * planScale;
        }
    }

    // переводим экранные координаты в плановые
    private float[] screenToPlan(float x, float y) {
        float planX = (x - offsetX - pl) / (planScale * scaleFactor);
        float planY = (y - offsetY - pt) / (planScale * scaleFactor);
        return new float[]{planX, planY};
    }

    // проверяем границы
    private boolean isInsidePlan(float planX, float planY) {
        float r = POINT_RADIUS;

        return planX >= r &&
                planY >= r &&
                planX <= planOriginalWidth - r &&
                planY <= planOriginalHeight - r;
    }


    // рисовашки
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Сохраняем состояние канвы
        canvas.save();

        // Сначала смещаем план (offset), потом масштабируем
        canvas.translate(offsetX, offsetY);
        canvas.scale(scaleFactor, scaleFactor);

        // ПЛАН
        // отрисовка плана территории из SVG
        if (planDrawable != null) {
            planDrawable.setBounds(pl, (int) pt, pl + (int) planWidth, (int) (pt + planHeight));
            planDrawable.draw(canvas);
        }

        // ТОЧКИ
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);      // цвет текста
        textPaint.setTextSize(20f * planScale);           // размер шрифта, можно масштабировать
        textPaint.setTextAlign(Paint.Align.LEFT); // центрирование далее вручную

        for (PlantPoint p : points) {
            if (p.plant == null) continue;
            // окраска для обычных/выбранных точек в режимах
            if (p == selectedPoint) {
                paint.setColor(Color.YELLOW); // выделение — желтый
                textPaint.setColor(Color.BLACK);
            } else {
                paint.setColor(Color.BLUE); // обычные точки
                textPaint.setColor(Color.WHITE);
            }

            boolean isFound = searchActive && searchResultsSet.contains(p);

            // обводка найденных
            if (isFound) {
                float screenX = pl + p.x * planScale;
                float screenY = pt + p.y * planScale;
                float screenRadius = POINT_RADIUS * planScale + 4f;

                canvas.drawCircle(
                        screenX,
                        screenY,
                        screenRadius,
                        searchStrokePaint
                );
            }

            float screenX = pl + p.x * planScale;
            float screenY = pt + p.y * planScale;
            float screenRadius = POINT_RADIUS * planScale;
            canvas.drawCircle(screenX, screenY, screenRadius, paint);


            // Отрисовка цифры по центру
            String text = String.valueOf(p.count);
            Rect bounds = new Rect();
            textPaint.getTextBounds(text, 0, text.length(), bounds);

            // Вычисляем смещение для центрирования
            float textX = screenX - bounds.width() / 2f;
            float textY = screenY + bounds.height() / 2f;

            canvas.drawText(text, textX, textY, textPaint);
            //Log.d("CHECK", "Plan id: " + p.id);
        }
        canvas.restore();
    }


    // Логика тапа
    private void handleTap(float x, float y) {
        if (currentMode == EditMode.ADD_POINT) {
            if (!isInsidePlan(x, y)) {
                showOutOfPlanWarning();
                return;
            }
            // создать точку и открыть ввод данных
            PlantPoint pendingPoint = new PlantPoint(x, y);

            PlantDialogs.showNewPlantDialog(
                    getContext(),
                    pendingPoint,
                    repository, () -> {
                        points.add(pendingPoint); // обновляем список точек
                        invalidate();             // перерисовываем экран
                    });
        } else if (currentMode == EditMode.EDIT_POINT) {
            // выбрать точку, открыть меню редактирования
            if (pressedPoint != null && !isDragging) {
                // фиксируем точку в локальную переменную,
                // чтобы коллбеки не зависели от обнуления поля pressedPoint в ACTION_UP
                PlantPoint tappedPoint = pressedPoint;
                // contains дает true только если это тот же объект, а не просто совпадение id
                if (!points.contains(tappedPoint)) {
                    pressedPoint = null;
                    return;
                }
                // изменить количество / удалить
                PlantDialogs.showEditPointDialog(
                        getContext(),
                        pressedPoint,
                        repository,
                        () -> {
                            showDeleteConfirmation(tappedPoint);
                        },
                        () -> {
                            invalidate();// перерисовываем экран
                        }
                );
            }
            // если isDragging == true — значит, это было перемещение
        }
    }

    // подтверждение удаления
    private void showDeleteConfirmation(PlantPoint point) {
        new AlertDialog.Builder(getContext())
                .setTitle("Удаление точки")
                .setMessage("Вы уверены, что хотите удалить эту точку?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    points.remove(point);

                    if (selectedPoint == point) selectedPoint = null;
                    if (draggedPoint == point) draggedPoint = null;

                    // удаляем из БД
                    if (point.id != 0) {
                        repository.deletePoint(point.id);
                    }

                    invalidate();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // предупреждение о выходе за границы территории
    private void showOutOfPlanWarning() {
        new AlertDialog.Builder(getContext())
                .setTitle("Нельзя создать точку")
                .setMessage("Точку можно создавать только внутри плана.")
                .setPositiveButton("Понятно", null)
                .show();
    }


    // событие касания
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // при удалении растения если жест все еще не завершен, чтобы краша не было
        if (planScale == 0f) {
            return false;
        }

        // Передаём событие детектору масштабирования
        scaleDetector.onTouchEvent(event);

        // координаты касания
        float x = event.getX();
        float y = event.getY();

        // версия по жесту
        switch (event.getAction()) {

            // подготовка
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

            // логика перемещения
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

                        // dx/dy экранные пиксели. Переводим в координаты плана
                        float dxPlan = dx / (planScale * scaleFactor);
                        float dyPlan = dy / (planScale * scaleFactor);
                        // делим на scaleFactor, чтобы движение пальца совпадало с масштабом
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

                    // Ограничение границ плана
                    // Используем реальные размеры плана
                    float maxOffsetX = 0;
                    float maxOffsetY = 0;
                    float minOffsetX = getWidth() - planWidth * scaleFactor;
                    float minOffsetY = getHeight() - planHeight * scaleFactor;

                    // Ограничиваем смещение
                    offsetX = Math.min(maxOffsetX, Math.max(offsetX, minOffsetX));
                    offsetY = Math.min(maxOffsetY, Math.max(offsetY, minOffsetY));

                    downX = x;
                    downY = y;
                    invalidate();
                }
                return true;

            // логика: редактирования кол-ва / удаления (edit) | просмотра растения (view)
            case MotionEvent.ACTION_UP:
                if (!gestureConsumed) {
                    if (currentMode == EditMode.VIEW) {
                        // координаты касания относительно плана
                        float[] p = screenToPlan(x, y);
                        // если мы в режиме просмотра — ищем точку под пальцем
                        PlantPoint tapped = findPointAt(p[0], p[1]);
                        selectedPoint = tapped;  // выделяем точку
                        invalidate();
                        if (tapped != null) {
                            showPlantInfo(tapped); // показываем данные
                        }
                    } else {
                        float[] p = screenToPlan(x, y);
                        handleTap(p[0], p[1]);
                    }
                } else if (draggedPoint != null && draggedPoint.id != 0) {
                    // координаты изменились, сохраняем в базе
                    repository.updatePoint(draggedPoint.id, draggedPoint);
                }
                draggedPoint = null;
                pressedPoint = null;
                isDragging = false;
                isDraggingPlan = false;
                return true;
        }
        return super.onTouchEvent(event); // если непонятно как обработать - пусть стандартный view развлекается
    }

    // в режиме просмотра смотрим инфу о растении в точке
    private void showPlantInfo(PlantPoint point) {
        Context context = getContext();

        if (point == null || point.plant == null) {
            return;
        }

        // для даты (отсекаем секунды часы минуты)
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String dateStr = (point.processingDate != null)
                ? sdf.format(new Date(point.processingDate))
                : ""; // пустое значение, если дата не задана

        String feedingDateStr = (point.feedingDate != null)
                ? sdf.format(new Date(point.feedingDate))
                : "";

        // для литража (убираем null)
        String potVolumeStr = (point.plant.potVolume != null)
                ? point.plant.potVolume.toString()
                : "";

        // для цвета (убираем null)
        String colorStr = (point.plant.flowerColor != null)
                ? point.plant.flowerColor.toString()
                : "";

        // для доп инфы (убираем null)
        String addInfoStr = (point.plant.additionalInfo != null)
                ? point.plant.additionalInfo.toString()
                : "";

        String message = "Название сорта: " + point.plant.name + "\n" +
                "Тип растения: " + point.plant.type + "\n" +
                "Группа: " + point.plant.group + "\n" +
                "Литраж горшка: " + potVolumeStr  + "\n" +
                "Цвет цветка: " + colorStr + "\n" +
                "Дополнительно: " + addInfoStr + "\n" +
                "Количество в точке: " + point.count + "\n" +
                "Дата обработки: " + dateStr + "\n" +
                "Дата подкормки: " + feedingDateStr;

        new AlertDialog.Builder(context)
                .setTitle("Информация о растении")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // Понять, что попали в существующую точку
    private PlantPoint findPointAt(float touchX, float touchY) {
        PlantPoint closest = null;
        float minDistanceSq = Float.MAX_VALUE;

        for (PlantPoint p : points) {
            if (p.plant == null) continue;
            // смещение от центра точки
            float dx = touchX - p.x;
            float dy = touchY - p.y;
            float distanceSquared = dx * dx + dy * dy;
            // чтобы при масштабировании не было слишишком большого радиуса попадания
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

    // масштабирование
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            gestureConsumed = true;
            // Ограничиваем масштаб
            float prevScale = scaleFactor;
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_SCALE, Math.min(scaleFactor, MAX_SCALE));

            // Смещение для масштабирования относительно точки фокуса
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();
            offsetX = (offsetX - focusX) * (scaleFactor / prevScale) + focusX;
            offsetY = (offsetY - focusY) * (scaleFactor / prevScale) + focusY;
            // Перерисовываем view с новым масштабом
            invalidate();
            return true;
        }
    }

    // перезагрузка точек (при удалении / изменении в БД)
    public void reloadPoints() {
        points.clear();
        points.addAll(repository.getAllPoints());
        selectedPoint = null;
        draggedPoint = null;
        pressedPoint = null;
        invalidate();
    }

    // ПОИСК
    // работает с результатами, полученными извне
    public void setSearchResults(Set<PlantPoint> results) {
        searchResultsSet.clear();
        searchResultsSet.addAll(results);
        searchActive = !searchResultsSet.isEmpty();

        if (searchStateListener != null) {
            if (searchActive) {
                searchStateListener.onSearchApplied();
            } else {
                searchStateListener.onSearchCleared();
            }
        }

        invalidate();
    }

    // очистка поиска
    public void clearSearch() {
        searchActive = false;
        searchResultsSet.clear();
        if (searchStateListener != null) {
            searchStateListener.onSearchCleared();
        }
        invalidate();
    }

    // слушатель состояния
    public void setSearchStateListener(SearchStateListener listener) {
        this.searchStateListener = listener;
    }

    // открывает диалог поиска
    public void showSearchDialog() {
        PlantSearchDialog.showAdvancedSearchDialog(
                getContext(),
                points,
                searchEngine,
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