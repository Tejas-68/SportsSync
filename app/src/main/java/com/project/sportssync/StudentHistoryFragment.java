package com.project.sportssync;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentHistoryFragment extends Fragment {

    private FirebaseFirestore db;
    private ListView lvHistory;
    private ProgressBar progressBar;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_student_history, container, false);
        
        db = FirebaseFirestore.getInstance();
        initViews(view);
        loadHistory();
        
        return view;
    }

    private void initViews(View view) {
        lvHistory = view.findViewById(R.id.lvHistory);
        progressBar = view.findViewById(R.id.progressBar);

        android.content.SharedPreferences prefs = com.project.sportssync.security.SecurePrefs.get(getContext());
        userId = prefs.getString("userId", null);
    }

    private void loadHistory() {
        if (userId == null) {
            Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        db.collection("attendanceRequests")
                .whereEqualTo("userId", userId)
                .orderBy("requestedAt", com.google.firebase.firestore.Query.Direction.DESCENDING) // ✅ FIXED field
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> historyList = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String date = doc.getString("date");
                        String sport = doc.getString("sport");
                        String status = doc.getString("status");
                        String timestamp = doc.getTimestamp("requestedAt") != null ?
                                formatTimestamp(doc.getTimestamp("requestedAt").toDate()) : "Unknown"; // ✅ FIXED field

                        String historyItem = String.format("%s - %s (%s)\n%s",
                                date, sport, status, timestamp);
                        historyList.add(historyItem);
                    }

                    progressBar.setVisibility(View.GONE);
                    updateHistoryList(historyList);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to load history: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateHistoryList(List<String> historyList) {
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_list_item_1,
                historyList
        );
        lvHistory.setAdapter(adapter);
    }

    private String formatTimestamp(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
        return sdf.format(date);
    }
}
