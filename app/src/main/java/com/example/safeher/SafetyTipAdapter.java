package com.example.safeher;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SafetyTipAdapter
        extends RecyclerView.Adapter<SafetyTipAdapter.TipViewHolder> {

    private final List<SafetyTip> tips;

    public SafetyTipAdapter(List<SafetyTip> tips) {
        this.tips = tips;
    }

    @NonNull
    @Override
    public TipViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_safety_tip, parent, false);

        return new TipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull TipViewHolder holder, int position) {

        SafetyTip tip = tips.get(position);
        holder.tipTitle.setText(tip.getTitle());
        holder.tipDescription.setText(tip.getDescription());
    }

    @Override
    public int getItemCount() {
        return tips.size();
    }

    static class TipViewHolder extends RecyclerView.ViewHolder {

        TextView tipTitle;
        TextView tipDescription;

        public TipViewHolder(@NonNull View itemView) {
            super(itemView);

            tipTitle = itemView.findViewById(R.id.tipTitle);
            tipDescription =
                    itemView.findViewById(R.id.tipDescription);
        }
    }
}
