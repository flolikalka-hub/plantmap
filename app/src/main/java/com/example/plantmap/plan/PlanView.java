package com.example.plantmap.plan;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
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

public class PlanView extends View {
    private Paint paint;

    private Drawable planDrawable;
    private float planWidth, planHeight;
    private float planScale;
    private float planOriginalWidth, planOriginalHeight;

    private ArrayList<PlantPoint> points;
    private static final float POINT_RADIUS = 7f;
    private static final float HIT_RADIUS = POINT_RADIUS * 3f;
    private EditMode currentMode = EditMode.VIEW;
    private PlantPoint draggedPoint = null;
    private PlantPoint pressedPoint = null;
    private boolean isDragging = false;
    private float downX, downY;
    private final float touchSlop;
    private PlantPoint selectedPoint = null;
    private PlantRepository repository;
    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.0f;
    private final float MIN_SCALE = 1.0f;
    private final float MAX_SCALE = 5.0f;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private boolean isDraggingPlan = false;
    private float focusX;
    private float focusY;
    private boolean gestureConsumed = false;
    private boolean searchActive = false;
    private String searchQuery = "";
    private final Set<PlantPoint> searchResultsSet = new HashSet<>();
    private Paint searchStrokePaint;
    public interface SearchStateListener {
        void onSearchApplied();

        void onSearchCleared();
    }

    private SearchStateListener searchStateListener;
    private PlantSearchEngine searchEngine = new PlantSearchEngine();
    private int pl;
    private int pr;
    private int pt;
    private int pb;

    public PlanView(Context context, PlantRepository repository) {
        super(context);

        this.repository = repository;

        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        touchSlop = ViewConfiguration
                .get(context)
                .getScaledTouchSlop();

        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5f);
        searchStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        searchStrokePaint.setStyle(Paint.Style.STROKE);
        searchStrokePaint.setStrokeWidth(5f);
        searchStrokePaint.setColor(Color.MAGENTA);


        planDrawable = ContextCompat.getDrawable(context, R.drawable.terr_plan);
        planOriginalWidth = planDrawable.getIntrinsicWidth();
        planOriginalHeight = planDrawable.getIntrinsicHeight();
        points = new ArrayList<>();
        points.clear();
        points.addAll(repository.getAllPoints());

        updatePlanGeometry();
    }
    public void setEditMode(EditMode mode) {
        this.currentMode = mode;
    }
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
        planWidth = Math.max(0f, getWidth() - pl - pr);
        if (planOriginalWidth > 0f) {
            planScale = planWidth / planOriginalWidth;
            planHeight = planOriginalHeight * planScale;
        }
    }

    private float[] screenToPlan(float x, float y) {
        float planX = (x - offsetX - pl) / (planScale * scaleFactor);
        float planY = (y - offsetY - pt) / (planScale * scaleFactor);
        return new float[]{planX, planY};
    }
    private boolean isInsidePlan(float planX, float planY) {
        float r = POINT_RADIUS;

        return planX >= r &&
                planY >= r &&
                planX <= planOriginalWidth - r &&
                planY <= planOriginalHeight - r;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scaleFactor, scaleFactor);
        if (planDrawable != null) {
            planDrawable.setBounds(pl, (int) pt, pl + (int) planWidth, (int) (pt + planHeight));
            planDrawable.draw(canvas);
        }
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(10f * planScale);
        textPaint.setTextAlign(Paint.Align.LEFT);

        for (PlantPoint p : points) {
            if (p.plant == null) continue;
            if (p == selectedPoint) {
                paint.setColor(Color.YELLOW);
                textPaint.setColor(Color.BLACK);
            } else {
                paint.setColor(Color.BLUE);
                textPaint.setColor(Color.WHITE);
            }

            boolean isFound = searchActive && searchResultsSet.contains(p);
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
            String text = String.valueOf(p.count);
            Rect bounds = new Rect();
            textPaint.getTextBounds(text, 0, text.length(), bounds);
            float textX = screenX - bounds.width() / 2f;
            float textY = screenY + bounds.height() / 2f;

            canvas.drawText(text, textX, textY, textPaint);
        }
        canvas.restore();
    }
    private void handleTap(float x, float y) {
        if (currentMode == EditMode.ADD_POINT) {
            if (!isInsidePlan(x, y)) {
                showOutOfPlanWarning();
                return;
            }
            PlantPoint pendingPoint = new PlantPoint(x, y);

            PlantDialogs.showNewPlantDialog(
                    getContext(),
                    pendingPoint,
                    repository, () -> {
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
                        getContext(),
                        pressedPoint,
                        repository,
                        () -> {
                            showDeleteConfirmation(tappedPoint);
                        },
                        () -> {
                            invalidate();
                        }
                );
            }
        }
    }
    private void showDeleteConfirmation(PlantPoint point) {
        new AlertDialog.Builder(getContext())
                .setTitle("Удаление точки")
                .setMessage("Вы уверены, что хотите удалить эту точку?")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    points.remove(point);

                    if (selectedPoint == point) selectedPoint = null;
                    if (draggedPoint == point) draggedPoint = null;
                    if (point.id != 0) {
                        repository.deletePoint(point.id);
                    }

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
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (planScale == 0f) {
            return false;
        }
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
                    if (currentMode == EditMode.VIEW) {
                        float[] p = screenToPlan(x, y);
                        PlantPoint tapped = findPointAt(p[0], p[1]);
                        selectedPoint = tapped;
                        invalidate();
                        if (tapped != null) {
                            showPlantInfo(tapped);
                        }
                    } else {
                        float[] p = screenToPlan(x, y);
                        handleTap(p[0], p[1]);
                    }
                } else if (draggedPoint != null && draggedPoint.id != 0) {
                    repository.updatePoint(draggedPoint.id, draggedPoint);
                }
                draggedPoint = null;
                pressedPoint = null;
                isDragging = false;
                isDraggingPlan = false;
                return true;
        }
        return super.onTouchEvent(event);
    }
    private void showPlantInfo(PlantPoint point) {
        Context context = getContext();

        if (point == null || point.plant == null) {
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String dateStr = (point.processingDate != null)
                ? sdf.format(new Date(point.processingDate))
                : "";

        String feedingDateStr = (point.feedingDate != null)
                ? sdf.format(new Date(point.feedingDate))
                : "";
        String potVolumeStr = (point.plant.potVolume != null)
                ? point.plant.potVolume.toString()
                : "";
        String colorStr = (point.plant.flowerColor != null)
                ? point.plant.flowerColor.toString()
                : "";
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
    public void reloadPoints() {
        points.clear();
        points.addAll(repository.getAllPoints());
        selectedPoint = null;
        draggedPoint = null;
        pressedPoint = null;
        invalidate();
    }
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
    public void clearSearch() {
        searchActive = false;
        searchResultsSet.clear();
        if (searchStateListener != null) {
            searchStateListener.onSearchCleared();
        }
        invalidate();
    }
    public void setSearchStateListener(SearchStateListener listener) {
        this.searchStateListener = listener;
    }
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