package com.sportssync.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sportssync.app.R;
import com.sportssync.app.activities.models.Equipment;
import java.util.List;

public class EquipmentManageAdapter extends RecyclerView.Adapter<EquipmentManageAdapter.ViewHolder> {

    private List<Equipment> equipmentList;

    public EquipmentManageAdapter(List<Equipment> equipmentList) {
        this.equipmentList = equipmentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_equipment_manage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Equipment equipment = equipmentList.get(position);
        holder.tvEquipmentName.setText(equipment.getEquipmentName());
        holder.tvQuantity.setText("Qty: " + equipment.getTotalQuantity() +
                " (Available: " + equipment.getAvailableQuantity() + ")");
    }

    @Override
    public int getItemCount() {
        return equipmentList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEquipmentName;
        TextView tvQuantity;

        ViewHolder(View itemView) {
            super(itemView);
            tvEquipmentName = itemView.findViewById(R.id.tvEquipmentName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
        }
    }
}