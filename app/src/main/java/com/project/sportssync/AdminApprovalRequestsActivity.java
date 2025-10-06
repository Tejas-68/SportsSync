package com.project.sportssync;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminApprovalRequestsActivity extends AppCompatActivity implements ApprovalRequestsAdapter.ApprovalActionListener {

    private RecyclerView recyclerView;
    private ApprovalRequestsAdapter adapter;
    private Button btnSelectAll, btnApproveSelected, btnRejectSelected, btnRefresh;
    private ProgressBar progressBar;

    private final List<ApprovalRequest> requests = new ArrayList<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_approval_requests);

        recyclerView = findViewById(R.id.recyclerRequests);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnApproveSelected = findViewById(R.id.btnApproveSelected);
        btnRejectSelected = findViewById(R.id.btnRejectSelected);
        btnRefresh = findViewById(R.id.btnRefresh);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ApprovalRequestsAdapter(requests, this);
        recyclerView.setAdapter(adapter);

        btnSelectAll.setOnClickListener(v -> toggleSelectAll());
        btnApproveSelected.setOnClickListener(v -> bulkUpdate(true));
        btnRejectSelected.setOnClickListener(v -> bulkUpdate(false));
        btnRefresh.setOnClickListener(v -> loadRequests());

        loadRequests();
    }

    private void loadRequests() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("approval_requests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(this::onRequestsLoaded)
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void onRequestsLoaded(QuerySnapshot snap) {
        requests.clear();
        for (DocumentSnapshot d : snap.getDocuments()) {
            ApprovalRequest r = new ApprovalRequest();
            r.id = d.getId();
            r.userId = d.getString("userId");
            r.uucms = d.getString("uucms");
            r.status = d.getString("status");
            requests.add(r);
        }
        adapter.notifyDataSetChanged();
        progressBar.setVisibility(View.GONE);
    }

    private void toggleSelectAll() {
        boolean selectAll = !adapter.areAllSelected();
        adapter.setAllSelected(selectAll);
    }

    private void bulkUpdate(boolean approve) {
        List<ApprovalRequest> selected = adapter.getSelected();
        if (selected.isEmpty()) {
            Toast.makeText(this, "No items selected", Toast.LENGTH_SHORT).show();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);

        List<com.google.android.gms.tasks.Task<Void>> tasks = new ArrayList<>();
        for (ApprovalRequest r : selected) {
            tasks.add(applyDecision(r, approve));
        }
        Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, approve ? "Approved selected" : "Rejected selected", Toast.LENGTH_SHORT).show();
            loadRequests();
        });
    }

    private com.google.android.gms.tasks.Task<Void> applyDecision(@NonNull ApprovalRequest r, boolean approve) {
        if (approve) {
            // 1) set user.approved = true
            // 2) set approval_requests.status = approved
            Map<String, Object> userUpdate = new HashMap<>();
            userUpdate.put("approved", true);
            return db.collection("users").document(r.userId)
                    .update(userUpdate)
                    .continueWithTask(t -> db.collection("approval_requests").document(r.id)
                            .update("status", "approved"));
        } else {
            return db.collection("approval_requests").document(r.id)
                    .update("status", "rejected");
        }
    }

    @Override
    public void onApproveClicked(int position) {
        ApprovalRequest r = requests.get(position);
        progressBar.setVisibility(View.VISIBLE);
        applyDecision(r, true).addOnCompleteListener(t -> {
            progressBar.setVisibility(View.GONE);
            if (t.isSuccessful()) {
                Toast.makeText(this, "Approved", Toast.LENGTH_SHORT).show();
                loadRequests();
            } else {
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRejectClicked(int position) {
        ApprovalRequest r = requests.get(position);
        progressBar.setVisibility(View.VISIBLE);
        applyDecision(r, false).addOnCompleteListener(t -> {
            progressBar.setVisibility(View.GONE);
            if (t.isSuccessful()) {
                Toast.makeText(this, "Rejected", Toast.LENGTH_SHORT).show();
                loadRequests();
            } else {
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class ApprovalRequest {
        public String id;
        public String userId;
        public String uucms;
        public String status;
        public boolean selected = false;
    }
}


