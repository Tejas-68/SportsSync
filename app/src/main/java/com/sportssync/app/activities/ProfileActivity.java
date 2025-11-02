package com.sportssync.app.activities;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.sportssync.app.R;
import com.sportssync.app.activities.utils.FirebaseManager;
import com.sportssync.app.activities.utils.PreferenceManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvProfileName;
    private TextView tvProfileUucms;
    private TextView tvTotalBorrows;
    private TextView tvActiveBorrows;
    private TextView tvAttendanceCount;
    private TextView tvInfoUucms;
    private TextView tvRegisteredDate;
    private PreferenceManager preferenceManager;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        preferenceManager = new PreferenceManager(this);
        firebaseManager = FirebaseManager.getInstance();

        initViews();
        setupToolbar();
        loadProfileData();
        loadStatistics();
    }

    private void initViews() {
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileUucms = findViewById(R.id.tvProfileUucms);
        tvTotalBorrows = findViewById(R.id.tvTotalBorrows);
        tvActiveBorrows = findViewById(R.id.tvActiveBorrows);
        tvAttendanceCount = findViewById(R.id.tvAttendanceCount);
        tvInfoUucms = findViewById(R.id.tvInfoUucms);
        tvRegisteredDate = findViewById(R.id.tvRegisteredDate);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadProfileData() {
        String name = preferenceManager.getUserName();
        String uucms = preferenceManager.getUucmsId();
        String userId = preferenceManager.getUserId();

        tvProfileName.setText(name);
        tvProfileUucms.setText(uucms);
        tvInfoUucms.setText(uucms);

        firebaseManager.getDb().collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long registeredAt = documentSnapshot.getLong("registeredAt");
                        if (registeredAt != null && registeredAt > 0) {
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                            tvRegisteredDate.setText(sdf.format(new Date(registeredAt)));
                        } else {
                            tvRegisteredDate.setText("Not registered");
                        }
                    }
                });
    }

    private void loadStatistics() {
        String studentId = preferenceManager.getUserId();

        firebaseManager.getDb().collection("borrowRecords")
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int total = queryDocumentSnapshots.size();
                    int active = 0;

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String status = doc.getString("status");
                        if ("borrowed".equals(status)) {
                            active++;
                        }
                    }

                    tvTotalBorrows.setText(String.valueOf(total));
                    tvActiveBorrows.setText(String.valueOf(active));
                });

        firebaseManager.getDb().collection("attendanceRequests")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tvAttendanceCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                });
    }
}