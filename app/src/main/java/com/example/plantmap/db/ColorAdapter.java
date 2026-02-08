package com.example.plantmap.db;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.example.plantmap.R;
import com.example.plantmap.model.FlowerColor;

import java.util.List;

public class ColorAdapter extends RecyclerView.Adapter<ColorAdapter.ColorViewHolder> {

    public interface OnEditClickListener {
        void onEditClick(FlowerColor color);
    }

    private Context context;
    private List<FlowerColor> colors;
    private OnEditClickListener listener;

    public ColorAdapter(Context context,
                        List<FlowerColor> colors,
                        OnEditClickListener listener) {
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
        applyCardColor(holder.card, holder.name, color.hex);

        // позже кнопку редактирования
        holder.card.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(color);
        });
    }

    @Override
    public int getItemCount() {
        return colors.size();
    }

    static class ColorViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView name;

        ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (CardView) itemView;
            name = itemView.findViewById(R.id.colorName);
        }
    }

    //Полная покраска карточки + подбор контрастного текста
    private void applyCardColor(CardView card, TextView textView, String hex) {
        if (hex == null || hex.isEmpty()) {
            card.setCardBackgroundColor(Color.TRANSPARENT);
            textView.setTextColor(Color.BLACK);
            return;
        }

        try {
            int bgColor = Color.parseColor(hex);
            card.setCardBackgroundColor(bgColor);

            // вычисляем яркость цвета
            boolean isDark = ColorUtils.calculateLuminance(bgColor) < 0.5;
            textView.setTextColor(isDark ? Color.WHITE : Color.BLACK);

        } catch (IllegalArgumentException e) {
            card.setCardBackgroundColor(Color.LTGRAY);
            textView.setTextColor(Color.BLACK);
        }
    }
}
