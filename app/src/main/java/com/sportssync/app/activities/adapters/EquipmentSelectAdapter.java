package com.sportssync.app.activities.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.sportssync.app.R;
import java.util.List;
import com.sportssync.app.activities.models.Equipment;

public class EquipmentSelectAdapter extends RecyclerView.Adapter<EquipmentSelectAdapter.ViewHolder> {

    private List<Equipment> equipmentList;
    private OnBorrowClickListener listener;

    public interface OnBorrowClickListener {
        void onBorrowClick(Equipment equipment, int quantity);
    }

    public EquipmentSelectAdapter(List<Equipment> equipmentList, OnBorrowClickListener listener) {
        this.equipmentList = equipmentList;
        this.listener = listener;
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
        Equipment equipment = equipmentList.get(position);

        holder.tvEquipmentName.setText(equipment.getEquipmentName());
        holder.tvAvailability.setText("Available: " + equipment.getAvailableQuantity());

        final int[] quantity = {0};
        holder.tvQuantity.setText(String.valueOf(quantity[0]));

        holder.btnMinus.setOnClickListener(v -> {
            if (quantity[0] > 0) {
                quantity[0]--;
                holder.tvQuantity.setText(String.valueOf(quantity[0]));
            }
        });

        holder.btnPlus.setOnClickListener(v -> {
            if (quantity[0] < equipment.getAvailableQuantity()) {
                quantity[0]++;
                holder.tvQuantity.setText(String.valueOf(quantity[0]));
            }
        });

        holder.btnBorrow.setOnClickListener(v -> {
            if (quantity[0] > 0 && listener != null) {
                listener.onBorrowClick(equipment, quantity[0]);
            }
        });

        holder.btnBorrow.setEnabled(equipment.getAvailableQuantity() > 0);
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEquipmentName;
        TextView tvAvailability;
        TextView tvQuantity;
        MaterialButton btnMinus;
        MaterialButton btnPlus;
        MaterialButton btnBorrow;

        ViewHolder(View itemView) {
            super(itemView);
            tvEquipmentName = itemView.findViewById(R.id.tvEquipmentName);
            tvAvailability = itemView.findViewById(R.id.tvAvailability);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnBorrow = itemView.findViewById(R.id.btnBorrow);
        }
    }
}