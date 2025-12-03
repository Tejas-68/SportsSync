package com.project.sportssync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EquipmentSelectAdapter extends RecyclerView.Adapter<EquipmentSelectAdapter.ViewHolder> {

    private List<EquipmentSelectionActivity.EquipmentItemView> items;
    private OnQuantityChangeListener quantityChangeListener;

    public interface OnQuantityChangeListener {
        void onQuantityChanged();
    }

    public EquipmentSelectAdapter(List<EquipmentSelectionActivity.EquipmentItemView> items) {
        this.items = items;
    }

    public void setQuantityChangeListener(OnQuantityChangeListener listener) {
        this.quantityChangeListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EquipmentSelectionActivity.EquipmentItemView item = items.get(position);
        
        holder.tvEquipmentName.setText(item.getName());
        holder.tvAvailable.setText("Available: " + item.getAvailableQuantity() + 
                                   " / " + item.getTotalQuantity());
        holder.tvQuantity.setText(String.valueOf(item.getSelectedQuantity()));

        holder.btnMinus.setOnClickListener(v -> {
            if (item.getSelectedQuantity() > 0) {
                item.setSelectedQuantity(item.getSelectedQuantity() - 1);
                holder.tvQuantity.setText(String.valueOf(item.getSelectedQuantity()));
                if (quantityChangeListener != null) {
                    quantityChangeListener.onQuantityChanged();
                }
            }
        });

        holder.btnPlus.setOnClickListener(v -> {
            if (item.getSelectedQuantity() < item.getAvailableQuantity()) {
                item.setSelectedQuantity(item.getSelectedQuantity() + 1);
                holder.tvQuantity.setText(String.valueOf(item.getSelectedQuantity()));
                if (quantityChangeListener != null) {
                    quantityChangeListener.onQuantityChanged();
                }
            }
        });

        holder.btnMinus.setEnabled(item.getSelectedQuantity() > 0);
        holder.btnPlus.setEnabled(item.getSelectedQuantity() < item.getAvailableQuantity() && 
                                  item.getAvailableQuantity() > 0);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEquipmentName, tvAvailable, tvQuantity;
        Button btnMinus, btnPlus;

        ViewHolder(View itemView) {
            super(itemView);
            tvEquipmentName = itemView.findViewById(R.id.tvEquipmentName);
            tvAvailable = itemView.findViewById(R.id.tvAvailable);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
        }
    }
}
