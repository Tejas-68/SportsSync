package com.sportssync.app.activities.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.sportssync.app.R;
import java.util.List;
import com.sportssync.app.activities.models.ReturnRequest;

public class ReturnRequestAdapter extends RecyclerView.Adapter<ReturnRequestAdapter.ViewHolder> {

    private List<ReturnRequest> requests;
    private OnActionClickListener listener;

    public interface OnActionClickListener {
        void onApprove(ReturnRequest request);
        void onReject(ReturnRequest request);
    }

    public ReturnRequestAdapter(List<ReturnRequest> requests, OnActionClickListener listener) {
        this.requests = requests;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_return_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReturnRequest request = requests.get(position);

        holder.tvStudentName.setText(request.getStudentName());
        holder.tvEquipmentName.setText(request.getEquipmentName());
        holder.tvQuantity.setText("Quantity: " + request.getQuantity());

        long timeDiff = System.currentTimeMillis() - request.getRequestedAt();
        String timeAgo = getTimeAgo(timeDiff);
        holder.tvRequestTime.setText(timeAgo);

        holder.tvStatus.setText(request.getStatus().substring(0, 1).toUpperCase() +
                request.getStatus().substring(1));

        if (request.getStatus().equals("pending")) {
            holder.actionButtons.setVisibility(View.VISIBLE);
            holder.btnApprove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onApprove(request);
                }
            });

            holder.btnReject.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReject(request);
                }
            });
        } else {
            holder.actionButtons.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    private String getTimeAgo(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + " day" + (days > 1 ? "s" : "") + " ago";
        if (hours > 0) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        if (minutes > 0) return minutes + " min" + (minutes > 1 ? "s" : "") + " ago";
        return "Just now";
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStudentName;
        TextView tvEquipmentName;
        TextView tvQuantity;
        TextView tvRequestTime;
        TextView tvStatus;
        LinearLayout actionButtons;
        MaterialButton btnApprove;
        MaterialButton btnReject;

        ViewHolder(View itemView) {
            super(itemView);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvEquipmentName = itemView.findViewById(R.id.tvEquipmentName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvRequestTime = itemView.findViewById(R.id.tvRequestTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            actionButtons = itemView.findViewById(R.id.actionButtons);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}