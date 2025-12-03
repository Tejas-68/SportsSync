package com.project.sportssync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Locale;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {
    
    private List<AchievementsActivity.Achievement> achievements;
    
    public AchievementAdapter(List<AchievementsActivity.Achievement> achievements) {
        this.achievements = achievements;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement_badge, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AchievementsActivity.Achievement achievement = achievements.get(position);
        
        holder.tvIcon.setText(achievement.getIcon());
        holder.tvTitle.setText(achievement.getTitle());
        holder.tvDescription.setText(achievement.getDescription());
        holder.tvProgress.setText(String.format(Locale.getDefault(), 
                "%d / %d", achievement.getCurrentProgress(), achievement.getRequiredProgress()));
        
        holder.progressBar.setMax(achievement.getRequiredProgress());
        holder.progressBar.setProgress(achievement.getCurrentProgress());
        
        // Visual state based on unlock status
        if (achievement.isUnlocked()) {
            holder.card.setCardBackgroundColor(0xFFE8F5E9); // Light green
            holder.tvIcon.setAlpha(1.0f);
            holder.tvTitle.setAlpha(1.0f);
            holder.tvUnlocked.setVisibility(View.VISIBLE);
        } else {
            holder.card.setCardBackgroundColor(0xFFFAFAFA); // Light gray
            holder.tvIcon.setAlpha(0.3f);
            holder.tvTitle.setAlpha(0.6f);
            holder.tvUnlocked.setVisibility(View.GONE);
        }
    }
    
    @Override
    public int getItemCount() {
        return achievements.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvIcon, tvTitle, tvDescription, tvProgress, tvUnlocked;
        ProgressBar progressBar;
        
        ViewHolder(View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvProgress = itemView.findViewById(R.id.tvProgress);
            tvUnlocked = itemView.findViewById(R.id.tvUnlocked);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}
