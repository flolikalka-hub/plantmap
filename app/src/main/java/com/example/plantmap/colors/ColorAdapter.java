package com.example.plantmap.colors;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plantmap.R;
import com.example.plantmap.model.FlowerColor;

import java.util.List;

public class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {

    public interface OnColorActionListener {
        void onEditClick(FlowerColor color);
        void onDeleteClick(FlowerColor color);
    }

    private Context context;
    private List<FlowerColor> colors;
    private OnColorActionListener listener;

    public ColorAdapter(Context context,
                        List<FlowerColor> colors,
                        OnColorActionListener listener) {
        this.context = context;
        this.colors = colors;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ColorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.color_item, parent, false);
        return new ColorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ColorViewHolder holder, int position) {
        FlowerColor color = colors.get(position);

        // название цвета
        holder.name.setText(color.name);

        // окраска всей карточки + контрастный текст
        int textColor = applyCardColor(holder.card, holder.name, color.hex);

        // динамически подбираем цвет кнопок под фон
        holder.editBtn.setColorFilter(textColor);
        holder.deleteBtn.setColorFilter(textColor);

        holder.editBtn.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(color);
        });

        holder.deleteBtn.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(color);
        });
    }

    @Override
    public int getItemCount() {
        return colors.size();
    }

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView name;
        ImageButton editBtn, deleteBtn;

        ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (CardView) itemView;
            name = itemView.findViewById(R.id.colorName);
            editBtn = itemView.findViewById(R.id.editBtn);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
        }
    }

     //Полная покраска карточки + подбор контрастного текста.
     //Возвращает цвет текста, чтобы использовать для кнопок.
    private int applyCardColor(CardView card, TextView textView, String hex) {
        if (hex == null || hex.isEmpty()) {
            card.setCardBackgroundColor(Color.TRANSPARENT);
            textView.setTextColor(Color.BLACK);
            return Color.BLACK;
        }

        try {
            int bgColor = Color.parseColor(hex);
            card.setCardBackgroundColor(bgColor);

            boolean isDark = ColorUtils.calculateLuminance(bgColor) < 0.5;
            int textColor = isDark ? Color.WHITE : Color.BLACK;
            textView.setTextColor(textColor);
            return textColor;

        } catch (IllegalArgumentException e) {
            card.setCardBackgroundColor(android.R.attr.windowBackground);
            textView.setTextColor(Color.BLACK);
            return Color.BLACK;
        }
    }
}