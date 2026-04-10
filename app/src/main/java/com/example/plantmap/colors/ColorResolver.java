package com.example.plantmap.colors;

import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

import com.example.plantmap.db.ColorDataAccess;
import com.example.plantmap.model.ColorModifier;
import com.example.plantmap.model.FlowerColor;

import java.util.ArrayList;
import java.util.List;

public class ColorResolver {
    private final ColorDataAccess colorDa;

    public ColorResolver(ColorDataAccess colorDa) {
        this.colorDa = colorDa;
    }

    public List<Integer> resolveColors(String rawColor) {
        List<Integer> result = new ArrayList<>();
        if (rawColor == null || rawColor.isEmpty()) return result;

        String[] parts = rawColor.toLowerCase().trim().split("-");

        List<ColorModifier> modifiers = new ArrayList<>();

        for (String part : parts) {
            ColorModifier m = colorDa.findModifierByRoot(part);
            if (m != null) modifiers.add(m);
        }

        for (String part : parts) {
            FlowerColor c = colorDa.findColorByRoot(part);
            if (c != null) {
                int base = Color.parseColor(c.hex);
                result.add(applyModifiers(base, modifiers));
            }
        }

        return result;
    }

    // применение модификаторов (темно-, нежно- и тд)
    private int applyModifiers(int color, List<ColorModifier> modifiers) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);

        for (ColorModifier m : modifiers) {
            hsl[1] = clamp(hsl[1] + (float) m.saturation);
            hsl[2] = clamp(hsl[2] + (float) m.lightness);
        }

        return ColorUtils.HSLToColor(hsl);
    }

    private float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

}