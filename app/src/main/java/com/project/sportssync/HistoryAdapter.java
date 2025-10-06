package com.project.sportssync;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import android.widget.BaseAdapter;

import java.util.List;

public class HistoryAdapter extends BaseAdapter {

    private Context context;
    private List<String> historyList;

    public HistoryAdapter(Context context, List<String> historyList) {
        this.context = context;
        this.historyList = historyList;
    }

    @Override
    public int getCount() {
        return historyList.size();
    }

    @Override
    public Object getItem(int position) {
        return historyList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_history, parent, false);
        }

        String record = historyList.get(position);
        TextView txtRecord = convertView.findViewById(R.id.txtRecord);
        txtRecord.setText(record);

        txtRecord.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        convertView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white));
        return convertView;
    }
}
