package com.example.plantmap.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Nullable;

import com.example.plantmap.model.ColorModifier;
import com.example.plantmap.model.FlowerColor;

import java.util.ArrayList;
import java.util.List;

public class ColorDataAccess {
    private final DatabaseHelper dbHelper;

    public ColorDataAccess(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }
    // поиск цвета по корню
    public FlowerColor findColorByRoot(String part) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        FlowerColor color = null;

        Cursor c = db.rawQuery(
                "SELECT * FROM colors WHERE ? LIKE root || '%' LIMIT 1",
                new String[]{part}
        );

        if (c.moveToFirst()) {
            color = new FlowerColor();
            color.id = c.getInt(c.getColumnIndexOrThrow("id"));
            color.name = c.getString(c.getColumnIndexOrThrow("name"));
            color.root = c.getString(c.getColumnIndexOrThrow("root"));
            color.hex = c.getString(c.getColumnIndexOrThrow("hex"));
        }

        c.close();
        //db.close();
        return color;
    }

    // поиск модификатора по корню
    public ColorModifier findModifierByRoot(String part) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        ColorModifier modifier = null;

        Cursor c = db.rawQuery(
                "SELECT * FROM modifiers WHERE ? LIKE root || '%' LIMIT 1",
                new String[]{part}
        );

        if (c.moveToFirst()) {
            modifier = new ColorModifier();
            modifier.id = c.getInt(c.getColumnIndexOrThrow("id"));
            modifier.name = c.getString(c.getColumnIndexOrThrow("name"));
            modifier.root = c.getString(c.getColumnIndexOrThrow("root"));
            modifier.lightness = c.getDouble(c.getColumnIndexOrThrow("lightness"));
            modifier.saturation = c.getDouble(c.getColumnIndexOrThrow("saturation"));
        }

        c.close();
        //db.close();
        return modifier;
    }

    // получаем все цвета
    public List<String> getAllColorNames() {
        List<String> colors = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("colors", new String[]{"name"}, null, null, null, null, "name");

        while (c.moveToNext()) {
            colors.add(c.getString(c.getColumnIndexOrThrow("name")));
        }

        c.close();
        //db.close();
        return colors;
    }

    // ЦВЕТА как отдельные объекты
    public List<FlowerColor> getAllColors() {
        List<FlowerColor> colors = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.query(
                "colors",
                null,
                null,
                null,
                null,
                null,
                "name"
        );

        while (c.moveToNext()) {
            FlowerColor color = new FlowerColor();
            color.id = c.getInt(c.getColumnIndexOrThrow("id"));
            color.name = c.getString(c.getColumnIndexOrThrow("name"));
            color.root = c.getString(c.getColumnIndexOrThrow("root"));
            color.hex = c.getString(c.getColumnIndexOrThrow("hex"));
            colors.add(color);
        }

        c.close();
        //db.close();
        return colors;
    }
    // Добавление нового цвета
    public long insertColor(String name, String root, String hex) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name.toLowerCase().trim());
        cv.put("root", root != null ? root.toLowerCase().trim() : null);
        cv.put("hex", hex != null ? hex.toLowerCase().trim() : null);
        long id = db.insert("colors", null, cv); // вернет -1 если не удалось (дубликат прим)
        //se();
        return id;
    }

    // Обновление существующего цвета
    public void updateColor(int id, String name, String root, String hex) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name.toLowerCase().trim());
        cv.put("root", root != null ? root.toLowerCase().trim() : null);
        cv.put("hex", hex != null ? hex.toLowerCase().trim() : null);
        db.update("colors", cv, "id=?", new String[]{String.valueOf(id)});
        //db.close();
    }

    public void deleteColor(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("colors", "id=?", new String[]{String.valueOf(id)});
        //db.close();
    }

    public boolean colorNameExists(String name, @Nullable Integer excludeId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String normalized = name.toLowerCase().trim();

        String sql;
        String[] args;

        if (excludeId == null) {
            sql = "SELECT 1 FROM colors WHERE LOWER(name) = ? LIMIT 1";
            args = new String[]{ normalized };
        } else {
            sql = "SELECT 1 FROM colors WHERE LOWER(name) = ? AND id != ? LIMIT 1";
            args = new String[]{ normalized, String.valueOf(excludeId) };
        }

        Cursor cursor = db.rawQuery(sql, args);
        boolean exists = cursor.moveToFirst();
        cursor.close();
        //db.close();

        return exists;
    }

    public boolean isColorUsed(String colorName, String root) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT 1 FROM plants WHERE " +
                "LOWER(flower_color) = ? " +
                "OR LOWER(flower_color) LIKE ? " +
                "LIMIT 1";

        String lowerName = colorName.toLowerCase().trim();
        String lowerRoot = root == null ? "" : root.toLowerCase().trim();

        String[] args = new String[]{
                "%" + lowerName + "%",
                "%" + lowerRoot + "%"
        };

        Cursor cursor = db.rawQuery(sql, args);
        boolean used = cursor.moveToFirst();
        cursor.close();
        //db.close();

        return used;
    }
}
