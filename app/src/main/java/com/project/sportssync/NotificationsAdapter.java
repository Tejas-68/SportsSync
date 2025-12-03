package com.project.sportssync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {
    
    private List<NotificationModel> notifications;
    private OnNotificationClickListener listener;
    
    public interface OnNotificationClickListener {
        void onNotificationClick(String notificationId);
    }
    
    public NotificationsAdapter(List<NotificationModel> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationModel notification = notifications.get(position);
        
        holder.tvTitle.setText(notification.getTitle());
        holder.tvBody.setText(notification.getBody());
        
        // Format timestamp
        if (notification.getCreatedAt() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault());
            holder.tvTime.setText(sdf.format(notification.getCreatedAt().toDate()));
        }
        
        // Show unread indicator
        if (!notification.isRead()) {
            holder.itemView.setBackgroundColor(0xFFF3F4F6); // Light gray for unread
            holder.tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            holder.itemView.setBackgroundColor(0xFFFFFFFF); // White for read
            holder.tvTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (!notification.isRead()) {
                listener.onNotificationClick(notification.getId());
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return notifications.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBody, tvTime;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvBody = itemView.findViewById(R.id.tvNotificationBody);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
        }
    }
}
