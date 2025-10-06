package com.project.sportssync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {

    private List<RequestModel> requestList;
    private OnApproveClickListener approveListener;
    private OnExitClickListener exitListener;

    public interface OnApproveClickListener {
        void onApprove(RequestModel request);
    }

    public interface OnExitClickListener {
        void onExit(RequestModel request);
    }

    public RequestAdapter(List<RequestModel> requestList,
                          OnApproveClickListener approveListener,
                          OnExitClickListener exitListener) {
        this.requestList = requestList;
        this.approveListener = approveListener;
        this.exitListener = exitListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RequestModel request = requestList.get(position);
        holder.txtUucms.setText("UUCMS: " + request.getUucms());
        holder.txtSport.setText("Sport: " + request.getSport());
        holder.txtStatus.setText("Status: " + request.getStatus());

        holder.btnApprove.setOnClickListener(v -> {
            if (approveListener != null) {
                approveListener.onApprove(request);
            }
        });

        holder.btnExit.setOnClickListener(v -> {
            if (exitListener != null) {
                exitListener.onExit(request);
            }
        });

        if ("pending".equalsIgnoreCase(request.getStatus())) {
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnExit.setVisibility(View.GONE);
        } else if ("approved".equalsIgnoreCase(request.getStatus())) {
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnExit.setVisibility(View.VISIBLE);
        } else {
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnExit.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtUucms, txtSport, txtStatus;
        Button btnApprove, btnExit;

        public ViewHolder(View itemView) {
            super(itemView);
            txtUucms = itemView.findViewById(R.id.txtUucms);
            txtSport = itemView.findViewById(R.id.txtSport);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnExit = itemView.findViewById(R.id.btnExit);
        }
    }
}