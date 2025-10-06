package com.project.sportssync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ApprovalRequestsAdapter extends RecyclerView.Adapter<ApprovalRequestsAdapter.VH> {

    public interface ApprovalActionListener {
        void onApproveClicked(int position);
        void onRejectClicked(int position);
    }

    private final List<AdminApprovalRequestsActivity.ApprovalRequest> items;
    private final ApprovalActionListener listener;

    public ApprovalRequestsAdapter(List<AdminApprovalRequestsActivity.ApprovalRequest> items, ApprovalActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_approval_request, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AdminApprovalRequestsActivity.ApprovalRequest r = items.get(position);
        h.txtTitle.setText(r.uucms != null ? r.uucms : r.userId);
        h.txtStatus.setText(r.status);
        h.chkSelect.setOnCheckedChangeListener(null);
        h.chkSelect.setChecked(r.selected);
        h.chkSelect.setOnCheckedChangeListener((buttonView, isChecked) -> r.selected = isChecked);
        h.btnApprove.setOnClickListener(v -> listener.onApproveClicked(h.getBindingAdapterPosition()));
        h.btnReject.setOnClickListener(v -> listener.onRejectClicked(h.getBindingAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public List<AdminApprovalRequestsActivity.ApprovalRequest> getSelected() {
        List<AdminApprovalRequestsActivity.ApprovalRequest> sel = new ArrayList<>();
        for (AdminApprovalRequestsActivity.ApprovalRequest r : items) {
            if (r.selected) sel.add(r);
        }
        return sel;
    }

    public boolean areAllSelected() {
        if (items.isEmpty()) return false;
        for (AdminApprovalRequestsActivity.ApprovalRequest r : items) {
            if (!r.selected) return false;
        }
        return true;
    }

    public void setAllSelected(boolean selected) {
        for (AdminApprovalRequestsActivity.ApprovalRequest r : items) {
            r.selected = selected;
        }
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView txtTitle;
        TextView txtStatus;
        CheckBox chkSelect;
        Button btnApprove;
        Button btnReject;

        VH(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            chkSelect = itemView.findViewById(R.id.chkSelect);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}



