package com.sportssync.app.activities.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.sportssync.app.R;
import java.util.List;
import com.sportssync.app.activities.models.Sport;

public class SportSelectAdapter extends RecyclerView.Adapter<SportSelectAdapter.ViewHolder> {

    private List<Sport> sports;
    private OnSportClickListener listener;

    public interface OnSportClickListener {
        void onSportClick(Sport sport);
    }

    public SportSelectAdapter(List<Sport> sports, OnSportClickListener listener) {
        this.sports = sports;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sport_select, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sport sport = sports.get(position);
        holder.tvSportName.setText(sport.getSportName());

        holder.cardSport.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSportClick(sport);
            }
        });
    }

    @Override
    public int getItemCount() {
        return sports.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardSport;
        TextView tvSportName;

        ViewHolder(View itemView) {
            super(itemView);
            cardSport = itemView.findViewById(R.id.cardSport);
            tvSportName = itemView.findViewById(R.id.tvSportName);
        }
    }
}