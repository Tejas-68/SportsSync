package com.project.sportssync;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StudentProfileFragment extends AppCompatActivity {

    private EditText edtName, edtPassword, edtConfirmPassword;
    private ExpandableHeightListView lvHistory;
    private Button btnSave;
    private ImageView btnEdit;
    private View editLayout;
    private View lvAchievements;
    private String userId, uucms;
    private FirebaseFirestore db;

    private TextView tvStudentName, tvStudentUucms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        btnEdit = findViewById(R.id.btnEdit);

        edtName = findViewById(R.id.edtName);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnSave = findViewById(R.id.btnSave);
        editLayout = findViewById(R.id.editLayout);

        lvHistory = findViewById(R.id.lvHistory);
        lvAchievements = findViewById(R.id.lvAchievements);

        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentUucms = findViewById(R.id.tvStudentUucms);

        db = FirebaseFirestore.getInstance();

        SharedPreferences prefs = com.project.sportssync.security.SecurePrefs.get(this);
        userId = prefs.getString("userId", null);
        uucms = prefs.getString("uucms", null);

        if (uucms != null) {
            uucms = uucms.trim();
            tvStudentUucms.setText(uucms);
        }

        loadUserDetails();
        loadHistory();
        loadAchievements();

        btnEdit.setOnClickListener(v -> toggleEdit(true));
        btnSave.setOnClickListener(v -> saveProfile());

        TextView txtHistoryHeader = findViewById(R.id.txtHistoryHeader);
        txtHistoryHeader.setOnClickListener(v -> {
            if (lvHistory.getVisibility() == View.GONE) {
                lvHistory.setVisibility(View.VISIBLE);
                txtHistoryHeader.setText("Attendance History ▲");
            } else {
                lvHistory.setVisibility(View.GONE);
                txtHistoryHeader.setText("Attendance History ▼");
            }
        });
    }

    private void loadUserDetails() {
        if (userId == null) return;

        DocumentReference userRef = db.collection("users").document(userId);
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String password = documentSnapshot.getString("password");
                String studentName = documentSnapshot.getString("name");

                if (studentName != null && !studentName.isEmpty()) {
                    tvStudentName.setText(studentName);
                    edtName.setText(studentName);
                }

                // Do not show stored password; clear field for security
                edtPassword.setText("");
            } else {
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadHistory() {
        db.collection("attendanceRequests")
                .whereEqualTo("uucms", uucms)
                .orderBy("requestedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener((QuerySnapshot snap) -> {
                    if (snap.isEmpty()) {
                        Toast.makeText(this, "No attendance records found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ArrayList<String> historyList = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a dd MMM yyyy", Locale.getDefault());

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String sport = doc.getString("sport");
                        String status = doc.getString("status");

                        String entry = doc.getTimestamp("requestedAt") != null
                                ? sdf.format(doc.getTimestamp("requestedAt").toDate())
                                : "N/A";

                        String exit = doc.getTimestamp("exitTime") != null
                                ? sdf.format(doc.getTimestamp("exitTime").toDate())
                                : "-";

                        String logEntry = "Sport: " + sport +
                                "\nEntry: " + entry +
                                "\nExit: " + exit +
                                "\nStatus: " + status;

                        historyList.add(logEntry);
                    }

                    HistoryAdapter adapter = new HistoryAdapter(this, historyList);
                    lvHistory.setAdapter(adapter);
                    lvHistory.setExpanded(true);
                    lvHistory.invalidateViews();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching attendance", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private void loadAchievements() {
        db.collection("achievements")
                .whereEqualTo("studentUucms", uucms)
                .get()
                .addOnSuccessListener((QuerySnapshot snap) -> {
                    ArrayList<String> achList = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String title = doc.getString("title");
                        String pos = doc.getString("position");
                        String desc = doc.getString("description");
                        achList.add(title + " | " + pos + " | " + desc);
                    }
                    ((android.widget.ListView) lvAchievements).setAdapter(
                            new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, achList));
                });
    }

    private void toggleEdit(boolean show) {
        editLayout.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void saveProfile() {
        String newName = edtName.getText().toString().trim();
        String newPassword = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        if (newName.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference userRef = db.collection("users").document(userId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", newName);
        updates.put("password", com.project.sportssync.security.PasswordHasher.hash(newPassword));

        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    toggleEdit(false);
                    tvStudentName.setText(newName);
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
