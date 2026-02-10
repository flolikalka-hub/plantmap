package com.example.plantmap.view;

import android.app.AlertDialog;
import android.content.Context; //объект среды
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View; //базовый визуальный элемент
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.view.ViewConfiguration;

import androidx.core.content.ContextCompat;

import com.example.plantmap.R;
import com.example.plantmap.db.DatabaseHelper;
import com.example.plantmap.model.Plant;
import com.example.plantmap.model.PlantPoint;
import com.example.plantmap.search.SearchFilter;
import com.example.plantmap.util.InputValidators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

    private DatabaseHelper dbHelper; // SQLite

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



    // ИНФОРМАТИВНОЕ
    // отступы устройства
    private int pl;
    private int pr;
    private int pt;
    private int pb;


    //                                  КОНСТРУКТОР, вызывается в момент создания view
    public PlanView(Context context) {
        // контекст - дступ к ресурсам, экрану, системе, БД ("где я живу")
        super(context);

        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        dbHelper = new DatabaseHelper(context);

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
        points.addAll(dbHelper.getAllPoints());

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
        planWidth = Math.max(0f,getWidth()-pl-pr);
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
                planX <= planOriginalWidth  - r &&
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
        textPaint.setTextSize(20f*planScale);           // размер шрифта, можно масштабировать
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
            PlantPoint pendingPoint = new PlantPoint(x,y);
            showInputDialog(pendingPoint);
        } else if (currentMode == EditMode.EDIT_POINT) {
            // выбрать точку, открыть меню редактирования
            if (pressedPoint != null && !isDragging) {
                // contains дает true только если это тот же объект, а не просто совпадение id
                if (!points.contains(pressedPoint)) {
                    pressedPoint = null;
                    return;
                }
                // это был ТАП
                showEditDialog(pressedPoint); // изменить количество / удалить
            }
            // если isDragging == true — значит, это было перемещение
        }
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
                    dbHelper.updatePoint(draggedPoint.id, draggedPoint);
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

        String message = "Название сорта: " + point.plant.name + "\n" +
                "Тип растения: " + point.plant.type + "\n" +
                "Группа: " + point.plant.group + "\n" +
                "Литраж горшка: " + point.plant.potVolume + "\n" +
                "Цвет цветка: " + point.plant.flowerColor + "\n" +
                "Количество в точке: " + point.count + "\n" +
                "Дополнительно: " + point.plant.additionalInfo;

        new AlertDialog.Builder(context)
                .setTitle("Информация о растении")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }



    // ввод данных о растении, когда НОВАЯ ТОЧКА
    private void showInputDialog(PlantPoint point) {
        Context context = getContext();

        // Для автокомплита подтягиваем все растения
        List<Plant> plants = dbHelper.getAllPlants();

        // поля ввода
        AutoCompleteTextView nameInput = new AutoCompleteTextView(context);
        nameInput.setHint("Название сорта");
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);

        EditText typeInput = new EditText(context);
        typeInput.setHint("Тип растения");

        EditText groupInput = new EditText(context);
        groupInput.setHint("Группа растения");

        EditText potVolumeInput = new EditText(context);
        potVolumeInput.setHint("Литраж горшка");
        potVolumeInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        AutoCompleteTextView flowerColorInput = new AutoCompleteTextView(context);
        flowerColorInput.setHint("Цвет цветка");
        // Получаем список цветов из БД
        List<String> colorNames = dbHelper.getAllColorNames();
        // Создаем адаптер
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                colorNames
        );
        // Привязываем адаптер к автокомплиту
        flowerColorInput.setAdapter(colorAdapter);
        flowerColorInput.setThreshold(1); // показывать подсказки после ввода 1 символа

        EditText countInput = new EditText(context);
        countInput.setHint("Количество");
        countInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        EditText addInput = new EditText(context);
        addInput.setHint("Дополнительная информация");

        // чтобы альбомная ориентация была хоть немного адекватной
        nameInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        typeInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        groupInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        potVolumeInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        flowerColorInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        countInput.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        // последнее поле - подтверждение
        addInput.setImeOptions(EditorInfo.IME_ACTION_DONE);

        // настройка автозаплнения по сорту
        ArrayAdapter<Plant> adapter =
                new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_dropdown_item_1line,
                        plants
                );
        nameInput.setAdapter(adapter);
        nameInput.setThreshold(1);

        // автозаполнение остальных полей по сорту
        nameInput.setOnItemClickListener((parent, view, position, id) -> {
            Plant selectedPlant = (Plant) parent.getItemAtPosition(position);

            typeInput.setText(selectedPlant.type != null ? selectedPlant.type : "");
            groupInput.setText(selectedPlant.group != null ? selectedPlant.group : "");
            potVolumeInput.setText(String.valueOf(selectedPlant.potVolume));
            flowerColorInput.setText(selectedPlant.flowerColor != null ? selectedPlant.flowerColor : "");
            addInput.setText(selectedPlant.additionalInfo != null ? selectedPlant.additionalInfo : "");
        });

        // настройки действий (далее) клавитуры на полях
        nameInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                typeInput.requestFocus();
                return true;
            }
            return false;
        });

        typeInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                groupInput.requestFocus();
                return true;
            }
            return false;
        });

        groupInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                potVolumeInput.requestFocus();
                return true;
            }
            return false;
        });

        potVolumeInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                flowerColorInput.requestFocus();
                return true;
            }
            return false;
        });

        flowerColorInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                countInput.requestFocus();
                return true;
            }
            return false;
        });

        countInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                addInput.requestFocus();
                return true;
            }
            return false;
        });

        // последняя закрывает клавиатуру, чтобы проверить данные глазом
        addInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {

                InputMethodManager imm =
                        (InputMethodManager) v.getContext()
                                .getSystemService(Context.INPUT_METHOD_SERVICE);

                if (imm != null) {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }

                v.clearFocus();
                return true;
            }
            return false;
        });

        // чтобы клавиатура всегда открывалась
        nameInput.requestFocus();

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(nameInput);
        layout.addView(typeInput);
        layout.addView(groupInput);
        layout.addView(potVolumeInput);
        layout.addView(flowerColorInput);
        layout.addView(countInput);
        layout.addView(addInput);

        // стандартное диалоговое окно, не xml
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Новое растение")
                .setView(layout)
                // Позитивное всегда закрывает диалог даже если данные неправильные - предостерегаем далее
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Отмена", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {

                String name = nameInput.getText().toString().trim();

                if (name.isEmpty()) {
                    nameInput.setError("Название обязательно");
                    return;
                }

                Integer count = validatePositiveCount(countInput);
                if (count == null) return;

                Integer potVolume = InputValidators.validatePositiveOptionalInt(potVolumeInput);
                if (potVolumeInput.getError() != null) return;

                // создаем временный объект растения для поиска полного совпадения
                Plant tempPlant = new Plant();
                tempPlant.name = name;
                tempPlant.type = typeInput.getText().toString().trim();
                tempPlant.group = groupInput.getText().toString().trim();
                tempPlant.flowerColor = flowerColorInput.getText().toString().trim();
                tempPlant.potVolume = potVolume == null
                        ? 0
                        : potVolume;
                tempPlant.additionalInfo = addInput.getText().toString().trim();

                // ищем точное совпадение всех полей
                Plant plant = dbHelper.findPlantByAllFields(tempPlant);

                // если его нет - тогда добавляем
                if (plant == null) {
                    // если нет ПОЛНЫХ совпадений, создаем новое растение
                    long plantId = dbHelper.addPlant(tempPlant);
                    tempPlant.id = (int) plantId;
                    plant = tempPlant;
                }

                point.plant = plant;
                point.count = count;

                long newId = dbHelper.addPoint(point);  // сохраням в БД с запоминанием id
                point.id = (int) newId;                 // присваиваем
                points.add(point);
                invalidate();
                dialog.dismiss();
            });
        });

        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        );

        dialog.show();
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
            float scaledHitRad = HIT_RADIUS*HIT_RADIUS / scaleFactor;
            if (distanceSquared <= scaledHitRad) {
                if (distanceSquared < minDistanceSq) {
                    minDistanceSq = distanceSquared;
                    closest = p;
                }
            }
        }
        return closest;
    }

    // работа с СУЩЕСТВУЮЩЕЙ ТОЧКОЙ + изменение растения
    private void showEditDialog(PlantPoint point) {
        Context context = getContext();

        // отсекаем зомби после reload
        if (point == null || !points.contains(point)) {
            return;
        }

        if (point.plant == null) {
            return;
        }

        // поле количества
        EditText countInput = new EditText(context);
        countInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        countInput.setText(String.valueOf(point.count));
        countInput.setSelection(countInput.getText().length());
        countInput.setHint("Количество");

        // контейнер
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(countInput);

        // кнопка смены растения
        Button changePlantBtn = new Button(context);
        changePlantBtn.setText("Сменить растение");

        layout.addView(changePlantBtn);

        // диалог
        AlertDialog editDialog = new AlertDialog.Builder(context)
                .setTitle(point.plant.name)
                .setMessage("Изменить количество или удалить точку")
                .setView(layout)
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Удалить", (dialog, which) -> {
                    dbHelper.deletePoint(point.id);

                    points.remove(point);

                    if (selectedPoint == point) selectedPoint = null;
                    if (pressedPoint == point) pressedPoint = null;
                    if (draggedPoint == point) draggedPoint = null;

                    invalidate();
                })
                .setNeutralButton("Отмена", null)
                // только создаем, но не показываем ибо мб переход в редактирование растения
                .create();
        editDialog.setOnShowListener(d -> {
            Button saveButton = editDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {

                Integer count = validatePositiveCount(countInput);
                if (count == null) return;

                point.count = count;
                dbHelper.updatePoint(point.id, point);
                invalidate();
                editDialog.dismiss();
            });
        });

        changePlantBtn.setOnClickListener(v -> {
            // закрываем текущий диалог редактирования
            editDialog.dismiss();
            // открываем диалог смены растения
            showChangePlantDialog(point);
        });

        // показываем диалог
        editDialog.show();
    }

    private void showChangePlantDialog(PlantPoint point) {
        Context context = getContext();

        // как при создании нового, но без полей относящихся к точке
        List<Plant> plants = dbHelper.getAllPlants();

        AutoCompleteTextView nameInput = new AutoCompleteTextView(context);
        nameInput.setHint("Название сорта");

        EditText typeInput = new EditText(context);
        typeInput.setHint("Тип растения");

        EditText groupInput = new EditText(context);
        groupInput.setHint("Группа растения");

        EditText potVolumeInput = new EditText(context);
        potVolumeInput.setHint("Литраж горшка");
        potVolumeInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        AutoCompleteTextView flowerColorInput = new AutoCompleteTextView(context);
        flowerColorInput.setHint("Цвет цветка");
        // Получаем список цветов из БД
        List<String> colorNames = dbHelper.getAllColorNames();
        // Создаем адаптер
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_dropdown_item_1line,
                colorNames
        );
        // Привязываем адаптер к автокомплиту
        flowerColorInput.setAdapter(colorAdapter);
        flowerColorInput.setThreshold(1); // показывать подсказки после ввода 1 символа


        EditText addInput = new EditText(context);
        addInput.setHint("Дополнительная информация");

        ArrayAdapter<Plant> adapter =
                new ArrayAdapter<>(
                        context,
                        android.R.layout.simple_dropdown_item_1line,
                        plants
                );

        nameInput.setAdapter(adapter);
        nameInput.setThreshold(1);

        nameInput.setOnItemClickListener((parent, view, position, id) -> {
            Plant selectedPlant = (Plant) parent.getItemAtPosition(position);

            typeInput.setText(selectedPlant.type);
            groupInput.setText(selectedPlant.group);
            potVolumeInput.setText(String.valueOf(selectedPlant.potVolume));
            flowerColorInput.setText(selectedPlant.flowerColor);
            addInput.setText(selectedPlant.additionalInfo);
        });

        // перезаполняем новым растением
        Plant currentPlant = point.plant;

        nameInput.setText(currentPlant.name);
        typeInput.setText(currentPlant.type);
        groupInput.setText(currentPlant.group);
        potVolumeInput.setText(String.valueOf(currentPlant.potVolume));
        flowerColorInput.setText(currentPlant.flowerColor);
        addInput.setText(currentPlant.additionalInfo);

        // собираем в лэйаут
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        layout.addView(nameInput);
        layout.addView(typeInput);
        layout.addView(groupInput);
        layout.addView(potVolumeInput);
        layout.addView(flowerColorInput);
        layout.addView(addInput);

        // диалог
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Сменить растение")
                .setView(layout)
                .setPositiveButton("Сохранить", null)
                .setNegativeButton("Отмена", null)
                .create();

        // логика сохранения, но без создания точки
        dialog.setOnShowListener(d -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {

                String name = nameInput.getText().toString().trim();
                if (name.isEmpty()) {
                    nameInput.setError("Название обязательно");
                    return;
                }

                Integer potVolume = InputValidators.validatePositiveOptionalInt(potVolumeInput);
                if (potVolumeInput.getError() != null) return;

                Plant tempPlant = new Plant();
                tempPlant.name = name;
                tempPlant.type = typeInput.getText().toString().trim();
                tempPlant.group = groupInput.getText().toString().trim();
                tempPlant.flowerColor = flowerColorInput.getText().toString().trim();
                tempPlant.potVolume = potVolume == null
                        ? 0
                        : potVolume;
                tempPlant.additionalInfo = addInput.getText().toString().trim();

                Plant plant = dbHelper.findPlantByAllFields(tempPlant);
                if (plant == null) {
                    long plantId = dbHelper.addPlant(tempPlant);
                    tempPlant.id = (int) plantId;
                    plant = tempPlant;
                }

                if (plant.id == 0) {
                    // на всякий случай читаем из БД последнюю вставку
                    Plant inserted = dbHelper.findPlantByAllFields(tempPlant);
                    if (inserted != null) plant = inserted;
                }

                // смена растения в точке непосредственная
                point.plant = plant;
                dbHelper.updatePoint(point.id, point);

                invalidate();
                dialog.dismiss();

                // сразу заново открываем диалог редактирования точки
                //showEditDialog(point);
            });
        });

        dialog.show();
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
        points.addAll(dbHelper.getAllPoints());
        selectedPoint = null;
        draggedPoint = null;
        pressedPoint = null;
        invalidate();
    }

    // ПОИСК
    // окошко поиска со всеми полями
    public void showAdvancedSearchDialog() {
        Context context = getContext();

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);

        AutoCompleteTextView nameInput = new AutoCompleteTextView(context);
        nameInput.setHint("Название сорта");

        EditText typeInput = new EditText(context);
        typeInput.setHint("Тип растения");

        EditText groupInput = new EditText(context);
        groupInput.setHint("Группа растения");

        EditText potVolumeInput = new EditText(context);
        potVolumeInput.setHint("Литраж горшка");
        potVolumeInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        AutoCompleteTextView flowerColorInput = new AutoCompleteTextView(context);
        flowerColorInput.setHint("Цвет цветка");

        EditText countInput = new EditText(context);
        countInput.setHint("Количество");
        countInput.setInputType(InputType.TYPE_CLASS_NUMBER);

        EditText addInput = new EditText(context);
        addInput.setHint("Дополнительная информация");

        layout.addView(nameInput);
        layout.addView(typeInput);
        layout.addView(groupInput);
        layout.addView(potVolumeInput);
        layout.addView(flowerColorInput);
        layout.addView(countInput);
        layout.addView(addInput);

        new AlertDialog.Builder(context)
                .setTitle("Поиск")
                .setView(layout)
                .setPositiveButton("Найти", (d, w) -> {

                    SearchFilter filter = new SearchFilter();

                    filter.name = nameInput.getText().toString().trim();
                    filter.type = typeInput.getText().toString().trim();
                    filter.group = groupInput.getText().toString().trim();
                    filter.flowerColor = flowerColorInput.getText().toString().trim();
                    filter.additionalInfo = addInput.getText().toString().trim();

                    filter.potVolume = parseIntOrNull(
                            potVolumeInput.getText().toString().trim()
                    );
                    filter.count = parseIntOrNull(
                            countInput.getText().toString().trim()
                    );

                    applyFilter(filter);
                })
                .setNegativeButton("Отменить", (d, w) -> {
                    searchActive = false;
                    searchResultsSet.clear();
                    if (searchStateListener != null) {
                        searchStateListener.onSearchCleared();
                    }
                    invalidate();
                })
                .show();
    }

    private Integer parseIntOrNull(String s) {
        try {
            return s.isEmpty() ? null : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer validatePositiveCount(EditText input) {
        String text = input.getText().toString().trim();

        if (text.isEmpty()) {
            input.setError("Количество обязательно");
            return null;
        }

        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            input.setError("Неверное число");
            return null;
        }

        if (value <= 0) {
            input.setError("Количество должно быть больше 0");
            return null;
        }

        return value;
    }

    private boolean isFilterEmpty(SearchFilter f) {
        return (f.name == null || f.name.isEmpty())
                && (f.type == null || f.type.isEmpty())
                && (f.group == null || f.group.isEmpty())
                && (f.flowerColor == null || f.flowerColor.isEmpty())
                && (f.additionalInfo == null || f.additionalInfo.isEmpty())
                && f.potVolume == null
                && f.count == null;
    }


    private void applyFilter(SearchFilter filter) {
        searchResultsSet.clear();

        if (isFilterEmpty(filter)) {
            searchActive = false;
            if (searchStateListener != null) {
                searchStateListener.onSearchCleared();
            }
            invalidate();
            return;
        }

        for (PlantPoint p : points) {
            if (matchesFilter(p, filter)) {
                searchResultsSet.add(p);
            }
        }

        if (!searchResultsSet.isEmpty()) {
            searchActive = true;
            if (searchStateListener != null) {
                searchStateListener.onSearchApplied();
            }
        } else {
            searchActive = false;
            if (searchStateListener != null) {
                searchStateListener.onSearchCleared();
            }
        }
        invalidate();
    }

    private boolean matchesFilter(PlantPoint p, SearchFilter f) {
        if (p.plant == null) return false;
        Plant plant = p.plant;

        // фильтр по точке
        if (f.count != null && p.count != f.count) return false;

        // фильтры по растению
        if (!matchesText(plant.name, f.name)) return false;
        if (!matchesText(plant.type, f.type)) return false;
        if (!matchesText(plant.group, f.group)) return false;
        if (!matchesText(plant.flowerColor, f.flowerColor)) return false;
        if (!matchesText(plant.additionalInfo, f.additionalInfo)) return false;

        if (f.potVolume != null && plant.potVolume != f.potVolume) return false;

        return true;
    }

    private boolean matchesText(String field, String filter) {
        if (filter == null || filter.isEmpty()) return true;
        if (field == null) return false;
        return field.toLowerCase().contains(filter.toLowerCase());
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

}