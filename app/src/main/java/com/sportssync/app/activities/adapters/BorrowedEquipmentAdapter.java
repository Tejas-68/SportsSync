package com.sportssync.app.activities.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.sportssync.app.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.sportssync.app.activities.models.BorrowRecord;

public class BorrowedEquipmentAdapter extends RecyclerView.Adapter<BorrowedEquipmentAdapter.ViewHolder> {

    private List<BorrowRecord> records;
    private OnReturnClickListener listener;

    public interface OnReturnClickListener {
        void onReturnClick(BorrowRecord record);
    }

    public BorrowedEquipmentAdapter(List<BorrowRecord> records, OnReturnClickListener listener) {
        this.records = records;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_borrowed_equipment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BorrowRecord record = records.get(position);

        holder.tvEquipmentName.setText(record.getEquipmentName());
        holder.tvSportName.setText(record.getSportName());
        holder.tvQuantity.setText("Quantity: " + record.getQuantity());

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String returnTime = sdf.format(new Date(record.getReturnBy()));
        holder.tvReturnTime.setText("Return by " + returnTime);

        holder.tvStatus.setText(record.getStatus().substring(0, 1).toUpperCase() +
                record.getStatus().substring(1));

        holder.btnReturn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReturnClick(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEquipmentName;
        TextView tvSportName;
        TextView tvQuantity;
        TextView tvReturnTime;
        TextView tvStatus;
        MaterialButton btnReturn;

        ViewHolder(View itemView) {
            super(itemView);
            tvEquipmentName = itemView.findViewById(R.id.tvEquipmentName);
            tvSportName = itemView.findViewById(R.id.tvSportName);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvReturnTime = itemView.findViewById(R.id.tvReturnTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnReturn = itemView.findViewById(R.id.btnReturn);
        }
    }
}