package com.sportssync.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sportssync.app.R;
import com.sportssync.app.activities.adapters.AttendanceHistoryAdapter;
import com.sportssync.app.activities.models.AttendanceRequest;
import com.sportssync.app.activities.utils.FirebaseManager;
import com.sportssync.app.activities.utils.PreferenceManager;
import java.util.ArrayList;
import java.util.List;

public class AttendanceHistoryActivity extends AppCompatActivity {

    private RecyclerView rvAttendanceHistory;
    private TextView tvNoHistory;
    private AttendanceHistoryAdapter adapter;
    private List<AttendanceRequest> historyList;
    private FirebaseManager firebaseManager;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_history);

        firebaseManager = FirebaseManager.getInstance();
        preferenceManager = new PreferenceManager(this);
        historyList = new ArrayList<>();

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadAttendanceHistory();
    }

    private void initViews() {
        rvAttendanceHistory = findViewById(R.id.rvAttendanceHistory);
        tvNoHistory = findViewById(R.id.tvNoHistory);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new AttendanceHistoryAdapter(historyList);
        rvAttendanceHistory.setLayoutManager(new LinearLayoutManager(this));
        rvAttendanceHistory.setAdapter(adapter);
    }

    private void loadAttendanceHistory() {
        String studentId = preferenceManager.getUserId();

        firebaseManager.getDb().collection("attendanceRequests")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    historyList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        tvNoHistory.setVisibility(View.VISIBLE);
                        rvAttendanceHistory.setVisibility(View.GONE);
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            AttendanceRequest request = document.toObject(AttendanceRequest.class);
                            historyList.add(request);
                        }
                        adapter.notifyDataSetChanged();
                        tvNoHistory.setVisibility(View.GONE);
                        rvAttendanceHistory.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    tvNoHistory.setText("Error loading history");
                    tvNoHistory.setVisibility(View.VISIBLE);
                    rvAttendanceHistory.setVisibility(View.GONE);
                });
    }
}