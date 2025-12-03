package com.project.sportssync;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BorrowedItemsAdapter extends RecyclerView.Adapter<BorrowedItemsAdapter.ViewHolder> {

    private List<BorrowRequest> items;
    private OnReturnClickListener returnListener;

    public interface OnReturnClickListener {
        void onReturn(BorrowRequest request);
    }

    public BorrowedItemsAdapter(List<BorrowRequest> items, OnReturnClickListener returnListener) {
        this.items = items;
        this.returnListener = returnListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_borrowed, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BorrowRequest request = items.get(position);
        
        holder.tvSport.setText(request.getSport());
        
        StringBuilder equipmentText = new StringBuilder();
        for (BorrowRequest.BorrowedEquipment eq : request.getEquipment()) {
            equipmentText.append("• ").append(eq.getName())
                    .append(" (").append(eq.getQuantity()).append(")\n");
        }
        holder.tvEquipment.setText(equipmentText.toString());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String date = sdf.format(request.getBorrowedAt().toDate());
        holder.tvDate.setText("Borrowed: " + date);

        // Display deadline information
        if (request.getBorrowedUntil() != null) {
            String deadlineDate = sdf.format(request.getBorrowedUntil().toDate());
            String deadlineStatus = request.getDeadlineStatus();
            
            Context context = holder.itemView.getContext();
            
            if ("overdue".equals(deadlineStatus)) {
                // Red - Overdue
                int daysOverdue = request.getDaysOverdue();
                holder.tvDeadline.setText("⚠️ OVERDUE by " + daysOverdue + " day" + (daysOverdue > 1 ? "s" : ""));
                holder.tvDeadline.setTextColor(context.getResources().getColor(R.color.analytics_absent));
                holder.tvDeadline.setVisibility(View.VISIBLE);
                holder.tvDeadlineDate.setText("Due: " + deadlineDate);
                holder.tvDeadlineDate.setVisibility(View.VISIBLE);
            } else if ("due_soon".equals(deadlineStatus)) {
                // Yellow - Due soon
                int daysRemaining = request.getDaysRemaining();
                String timeText = daysRemaining == 0 ? "today" : "in " + daysRemaining + " day" + (daysRemaining > 1 ? "s" : "");
                holder.tvDeadline.setText("⏰ Due " + timeText);
                holder.tvDeadline.setTextColor(context.getResources().getColor(R.color.analytics_pending));
                holder.tvDeadline.setVisibility(View.VISIBLE);
                holder.tvDeadlineDate.setText("Due: " + deadlineDate);
                holder.tvDeadlineDate.setVisibility(View.VISIBLE);
            } else if ("safe".equals(deadlineStatus)) {
                // Green - Safe
                int daysRemaining = request.getDaysRemaining();
                holder.tvDeadline.setText("✓ " + daysRemaining + " day" + (daysRemaining > 1 ? "s" : "") + " remaining");
                holder.tvDeadline.setTextColor(context.getResources().getColor(R.color.analytics_present));
                holder.tvDeadline.setVisibility(View.VISIBLE);
                holder.tvDeadlineDate.setText("Due: " + deadlineDate);
                holder.tvDeadlineDate.setVisibility(View.VISIBLE);
            } else {
                holder.tvDeadline.setVisibility(View.GONE);
                holder.tvDeadlineDate.setVisibility(View.GONE);
            }
        } else {
            holder.tvDeadline.setVisibility(View.GONE);
            holder.tvDeadlineDate.setVisibility(View.GONE);
        }

        holder.btnReturn.setOnClickListener(v -> returnListener.onReturn(request));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSport, tvEquipment, tvDate, tvDeadline, tvDeadlineDate;
        Button btnReturn;

        ViewHolder(View itemView) {
            super(itemView);
            tvSport = itemView.findViewById(R.id.tvSport);
            tvEquipment = itemView.findViewById(R.id.tvEquipment);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDeadline = itemView.findViewById(R.id.tvDeadline);
            tvDeadlineDate = itemView.findViewById(R.id.tvDeadlineDate);
            btnReturn = itemView.findViewById(R.id.btnReturn);
        }
    }
}
