package com.sportssync.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.sportssync.app.R;
import com.sportssync.app.activities.models.Sport;
import java.util.List;

public class SportManageAdapter extends RecyclerView.Adapter<SportManageAdapter.ViewHolder> {

    private List<Sport> sportsList;
    private OnSportActionListener listener;

    public interface OnSportActionListener {
        void onToggleActive(Sport sport, boolean isActive);
        void onAddEquipment(Sport sport);
    }

    public SportManageAdapter(List<Sport> sportsList, OnSportActionListener listener) {
        this.sportsList = sportsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sport_manage, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sport sport = sportsList.get(position);

        holder.tvSportName.setText(sport.getSportName());
        holder.switchActive.setChecked(sport.isActive());

        holder.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggleActive(sport, isChecked);
            }
        });

        com.sportssync.app.adapters.EquipmentManageAdapter equipmentAdapter = new com.sportssync.app.adapters.EquipmentManageAdapter(sport.getEquipmentList());
        holder.rvEquipment.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
        holder.rvEquipment.setAdapter(equipmentAdapter);

        holder.btnAddEquipment.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddEquipment(sport);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sportsList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSportName;
        SwitchMaterial switchActive;
        RecyclerView rvEquipment;
        MaterialButton btnAddEquipment;

        ViewHolder(View itemView) {
            super(itemView);
            tvSportName = itemView.findViewById(R.id.tvSportName);
            switchActive = itemView.findViewById(R.id.switchActive);
            rvEquipment = itemView.findViewById(R.id.rvEquipment);
            btnAddEquipment = itemView.findViewById(R.id.btnAddEquipment);
        }
    }
}