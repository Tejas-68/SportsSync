package com.project.sportssync;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class PTActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private EditText etNewCode;
    private Button btnUpdateCode;
    private LinearLayout llRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pt);

        db = FirebaseFirestore.getInstance();
        etNewCode = findViewById(R.id.etNewCode);
        btnUpdateCode = findViewById(R.id.btnUpdateCode);
        llRequests = findViewById(R.id.llRequests);

        btnUpdateCode.setOnClickListener(v -> {
            String newCode = etNewCode.getText().toString().trim();
            if (newCode.isEmpty()) {
                Toast.makeText(this, "Enter code", Toast.LENGTH_SHORT).show();
                return;
            }
            changeAppCode(newCode);
        });

        loadPendingRequests();
    }

    private void loadPendingRequests() {
        db.collection("approval_requests").whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener((QuerySnapshot qs) -> {
                    llRequests.removeAllViews();
                    LayoutInflater inflater = LayoutInflater.from(this);
                    for (DocumentSnapshot doc : qs.getDocuments()) {
                        View card = inflater.inflate(R.layout.request_card, llRequests, false);
                        TextView tv = card.findViewById(R.id.tvRequestUucms);
                        Button bApprove = card.findViewById(R.id.btnApprove);
                        Button bReject = card.findViewById(R.id.btnReject);

                        String uucms = doc.getString("uucms");
                        String reqId = doc.getId();
                        String userId = doc.getString("userId");

                        tv.setText(uucms != null ? uucms : "unknown");

                        bApprove.setOnClickListener(v -> approveRequest(reqId, userId, card));
                        bReject.setOnClickListener(v -> rejectRequest(reqId, card));

                        llRequests.addView(card);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void approveRequest(String requestId, String userId, View card) {
        db.collection("users").document(userId)
                .update("approved", true)
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> upd = new HashMap<>();
                    upd.put("status", "approved");
                    upd.put("handledAt", Timestamp.now());
                    db.collection("approval_requests").document(requestId).update(upd);
                    llRequests.removeView(card);
                    Toast.makeText(this, "Approved", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Approve failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void rejectRequest(String requestId, View card) {
        Map<String, Object> upd = new HashMap<>();
        upd.put("status", "rejected");
        upd.put("handledAt", Timestamp.now());
        db.collection("approval_requests").document(requestId).update(upd)
                .addOnSuccessListener(aVoid -> {
                    llRequests.removeView(card);
                    Toast.makeText(this, "Rejected", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Reject failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void changeAppCode(String newCode) {
        Map<String, Object> data = new HashMap<>();
        data.put("currentCode", newCode);
        data.put("codeUpdatedAt", Timestamp.now());

        db.collection("settings").document("app")
                .set(data)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Code updated", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
