package com.project.sportssync;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CurrentLoginAdapter extends RecyclerView.Adapter<CurrentLoginAdapter.ViewHolder> {

    private List<RequestModel> loginList;
    private OnRemoveClickListener removeListener;

    public interface OnRemoveClickListener {
        void onRemove(RequestModel login);
    }

    public CurrentLoginAdapter(List<RequestModel> loginList,
                               OnRemoveClickListener removeListener) {
        this.loginList = loginList;
        this.removeListener = removeListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_current_login, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RequestModel login = loginList.get(position);
        holder.txtInfo.setText("UUCMS: " + login.getUucms() + " | Sport: " + login.getSport());
        holder.btnRemove.setOnClickListener(v -> removeListener.onRemove(login));
    }

    @Override
    public int getItemCount() {
        return loginList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtInfo;
        Button btnRemove;

        public ViewHolder(View itemView) {
            super(itemView);
            txtInfo = itemView.findViewById(R.id.txtLoginInfo);
            btnRemove = itemView.findViewById(R.id.btnExit);
        }
    }
}