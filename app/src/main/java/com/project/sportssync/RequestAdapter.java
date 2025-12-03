package com.project.sportssync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.ViewHolder> {

    private List<RequestModel> requestList;
    private OnApproveClickListener approveListener;
    private OnDenyClickListener denyListener;

    public interface OnApproveClickListener {
        void onApprove(RequestModel request);
    }

    public interface OnDenyClickListener {
        void onDeny(RequestModel request);
    }

    public RequestAdapter(List<RequestModel> requestList, OnApproveClickListener approveListener, OnDenyClickListener denyListener) {
        this.requestList = requestList;
        this.approveListener = approveListener;
        this.denyListener = denyListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RequestModel request = requestList.get(position);
        
        // Show student name with UUCMS
        if (request.getStudentName() != null && !request.getStudentName().isEmpty()) {
            holder.txtUucms.setText(request.getStudentName() + " (" + request.getUucms() + ")");
        } else {
            holder.txtUucms.setText("UUCMS: " + request.getUucms());
        }
        
        holder.txtStatus.setText(request.getStatus());

        // Show request type and details
        if ("return".equals(request.getType())) {
            holder.txtRequestType.setText("Return Request");
            holder.txtSport.setText("Sport: " + request.getSport());
            
            // Show equipment details
            if (request.getEquipmentList() != null && !request.getEquipmentList().isEmpty()) {
                StringBuilder equipment = new StringBuilder("Equipment: ");
                for (int i = 0; i < request.getEquipmentList().size(); i++) {
                    BorrowRequest.BorrowedEquipment eq = request.getEquipmentList().get(i);
                    equipment.append(eq.getName()).append(" (").append(eq.getQuantity()).append(")");
                    if (i < request.getEquipmentList().size() - 1) {
                        equipment.append(", ");
                    }
                }
                holder.txtEquipmentDetails.setText(equipment.toString());
                holder.txtEquipmentDetails.setVisibility(View.VISIBLE);
            } else {
                holder.txtEquipmentDetails.setVisibility(View.GONE);
            }
        } else {
            holder.txtRequestType.setText("Attendance Request");
            holder.txtSport.setText("Sport: " + request.getSport());
            holder.txtEquipmentDetails.setVisibility(View.GONE);
        }

        // Show timestamp
        holder.txtTimestamp.setText(getRelativeTime(request.getRequestedAt()));

        holder.btnApprove.setOnClickListener(v -> approveListener.onApprove(request));
        holder.btnReject.setOnClickListener(v -> denyListener.onDeny(request));
    }

    private String getRelativeTime(Timestamp timestamp) {
        if (timestamp == null) return "Just now";
        
        long now = System.currentTimeMillis();
        long then = timestamp.toDate().getTime();
        long diff = now - then;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) return "Just now";
        if (minutes < 60) return minutes + " min ago";
        if (hours < 24) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        if (days < 7) return days + " day" + (days > 1 ? "s" : "") + " ago";
        
        return new java.text.SimpleDateFormat("MMM dd, hh:mm a").format(timestamp.toDate());
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtUucms, txtSport, txtStatus, txtRequestType, txtEquipmentDetails, txtTimestamp;
        Button btnApprove, btnReject;

        public ViewHolder(View itemView) {
            super(itemView);
            txtUucms = itemView.findViewById(R.id.txtUucms);
            txtSport = itemView.findViewById(R.id.txtSport);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtRequestType = itemView.findViewById(R.id.txtRequestType);
            txtEquipmentDetails = itemView.findViewById(R.id.txtEquipmentDetails);
            txtTimestamp = itemView.findViewById(R.id.txtTimestamp);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}