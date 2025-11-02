package com.sportssync.app.activities.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sportssync.app.R;
import com.sportssync.app.activities.models.AttendanceRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AttendanceHistoryAdapter extends RecyclerView.Adapter<AttendanceHistoryAdapter.ViewHolder> {

    private List<AttendanceRequest> requests;

    public AttendanceHistoryAdapter(List<AttendanceRequest> requests) {
        this.requests = requests;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceRequest request = requests.get(position);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

        holder.tvDate.setText(dateFormat.format(new Date(request.getRequestedAt())));
        holder.tvTime.setText(timeFormat.format(new Date(request.getRequestedAt())));

        String status = request.getStatus().substring(0, 1).toUpperCase() +
                request.getStatus().substring(1);
        holder.tvStatus.setText(status);

        if (request.getStatus().equals("approved")) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#4CAF50"));
        } else if (request.getStatus().equals("rejected")) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#F44336"));
        } else {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#FF9800"));
        }
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvTime;
        TextView tvStatus;

        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}