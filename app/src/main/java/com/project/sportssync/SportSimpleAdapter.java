package com.project.sportssync;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SportSimpleAdapter extends RecyclerView.Adapter<SportSimpleAdapter.ViewHolder> {

    private List<SportModel> sports;
    private Context context;
    private String userId, uucms;

    public SportSimpleAdapter(List<SportModel> sports, Context context, String userId, String uucms) {
        this.sports = sports;
        this.context = context;
        this.userId = userId;
        this.uucms = uucms;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sport_simple, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SportModel sport = sports.get(position);
        
        holder.tvSportName.setText(sport.getName());
        int equipmentCount = sport.getEquipment() != null ? sport.getEquipment().size() : 0;
        holder.tvEquipmentCount.setText(equipmentCount + " items");

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EquipmentSelectionActivity.class);
            intent.putExtra("sportId", sport.getId());
            intent.putExtra("sportName", sport.getName());
            intent.putExtra("userId", userId);
            intent.putExtra("uucms", uucms);
            
            // Pass student name for borrow request optimization
            if (context instanceof StudentDashboardActivity) {
                String studentName = ((StudentDashboardActivity) context).getIntent().getStringExtra("name");
                if (studentName != null) {
                    intent.putExtra("studentName", studentName);
                }
            }
            
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return sports.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSportName, tvEquipmentCount;

        ViewHolder(View itemView) {
            super(itemView);
            tvSportName = itemView.findViewById(R.id.tvSportName);
            tvEquipmentCount = itemView.findViewById(R.id.tvEquipmentCount);
        }
    }
}
