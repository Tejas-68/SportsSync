package com.project.sportssync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoginActivity extends AppCompatActivity {

    private EditText etUucms, etName, etPassword, etCode;
    private Button btnLoginStudent, btnLoginAdmin;
    private RadioGroup rgUserType;
    private RadioButton rbStudent, rbAdmin;
    private ProgressBar progressBar;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUucms = findViewById(R.id.etUucms);
        etName = findViewById(R.id.etName);
        etPassword = findViewById(R.id.etPassword);
        etCode = findViewById(R.id.etCode);
        btnLoginStudent = findViewById(R.id.btnLogin);
        btnLoginAdmin = findViewById(R.id.btnLoginAdmin);
        rgUserType = findViewById(R.id.rgUserType);
        rbStudent = findViewById(R.id.rbStudent);
        rbAdmin = findViewById(R.id.rbAdmin);
        progressBar = findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();

        checkSavedLogin();

        // Default → Student login visible
        rgUserType.check(R.id.rbStudent);
        etCode.setVisibility(View.VISIBLE);
        etName.setVisibility(View.VISIBLE);
        btnLoginAdmin.setVisibility(View.GONE);

        rgUserType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbStudent) {
                etCode.setVisibility(View.VISIBLE);
                etName.setVisibility(View.VISIBLE);
                btnLoginStudent.setVisibility(View.VISIBLE);
                btnLoginAdmin.setVisibility(View.GONE);
            } else {
                etCode.setVisibility(View.GONE);
                etName.setVisibility(View.GONE);
                btnLoginStudent.setVisibility(View.GONE);
                btnLoginAdmin.setVisibility(View.VISIBLE);
            }
        });

        btnLoginStudent.setOnClickListener(v -> attemptStudentLogin());
        btnLoginAdmin.setOnClickListener(v -> attemptAdminLogin());
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            btnLoginStudent.setEnabled(false);
            btnLoginAdmin.setEnabled(false);
            btnLoginStudent.setText("Logging in...");
            btnLoginAdmin.setText("Logging in...");
        } else {
            progressBar.setVisibility(View.GONE);
            btnLoginStudent.setEnabled(true);
            btnLoginAdmin.setEnabled(true);
            btnLoginStudent.setText("Student Login");
            btnLoginAdmin.setText("Admin Login");
        }
    }

    private void checkSavedLogin() {
        SharedPreferences prefs = getSharedPreferences("SportsSyncPrefs", MODE_PRIVATE);
        String role = prefs.getString("role", null);
        String userId = prefs.getString("userId", null);
        String uucms = prefs.getString("uucms", null);

        if (role != null && userId != null) {
            if (role.equals("student")) {
                Intent serviceIntent = new Intent(this, AutoLogoutService.class);
                try {
                    startService(serviceIntent);
                } catch (Exception ignored) {}
                
                Intent i = new Intent(this, StudentDashboardActivity.class);
                i.putExtra("userId", userId);
                i.putExtra("uucms", uucms);
                startActivity(i);
            } else {
                Intent i = new Intent(this, PtDashboardActivity.class);
                i.putExtra("adminId", userId);
                startActivity(i);
            }
            finish();
        }
    }

    private void attemptStudentLogin() {
        String uucms = etUucms.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String code = etCode.getText().toString().trim();

        if (TextUtils.isEmpty(uucms) || TextUtils.isEmpty(name) || TextUtils.isEmpty(password) || TextUtils.isEmpty(code)) {
            Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        db.collection("settings").document("app")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String loginCode = doc.getString("loginCode");
                        if (loginCode == null || !loginCode.equals(code)) {
                            showLoading(false);
                            Toast.makeText(this, "Invalid login code. Ask admin.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        checkStudent(uucms, name, password);
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "App settings not configured. Contact admin.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void checkStudent(String uucms, String name, String password) {
        db.collection("users")
                .whereEqualTo("uucms", uucms)
                .get()
                .addOnSuccessListener((QuerySnapshot snap) -> {
                    if (!snap.isEmpty()) {
                        DocumentSnapshot userDoc = snap.getDocuments().get(0);
                        String storedPassword = userDoc.getString("password");
                        Boolean approved = userDoc.getBoolean("approved");

                        if (storedPassword != null && storedPassword.equals(password)) {
                            if (Boolean.TRUE.equals(approved)) {
                                saveStudentLogin(userDoc.getId(), uucms);
                                openStudent(userDoc.getId(), uucms); // ✅ pass both values
                            } else {
                                showLoading(false);
                                createOrNotifyApproval(userDoc.getId(), uucms);
                            }
                        } else {
                            showLoading(false);
                            Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        createNewStudentAndRequest(uucms, name, password);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void createNewStudentAndRequest(String uucms, String name, String password) {
        String newUserId = uucms; // ✅ use UUCMS as doc ID
        Map<String, Object> user = new HashMap<>();
        user.put("uucms", uucms);
        user.put("name", name);
        user.put("password", password);
        user.put("role", "student");
        user.put("approved", false);
        user.put("createdAt", Timestamp.now());

        db.collection("users").document(newUserId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    createApprovalRequest(newUserId, uucms);
                    showLoading(false);
                    Toast.makeText(this, "Account created. Waiting admin approval.", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to create user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void createOrNotifyApproval(String userId, String uucms) {
        // ✅ check if already has pending request
        db.collection("approval_requests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        createApprovalRequest(userId, uucms);
                    }
                });

        Toast.makeText(this, "You are not approved yet. Admin will approve soon.", Toast.LENGTH_LONG).show();
    }

    private void createApprovalRequest(String userId, String uucms) {
        String reqId = UUID.randomUUID().toString();
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("uucms", uucms);
        req.put("status", "pending");
        req.put("timestamp", Timestamp.now());

        db.collection("approval_requests").document(reqId).set(req);
    }

    private void saveStudentLogin(String userId, String uucms) {
        SharedPreferences prefs = getSharedPreferences("SportsSyncPrefs", MODE_PRIVATE);
        prefs.edit()
                .putString("role", "student")
                .putString("userId", userId)
                .putString("uucms", uucms)
                .apply();
    }

    private void attemptAdminLogin() {
        String adminId = etUucms.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(adminId) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter admin ID & password", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        db.collection("admins")
                .whereEqualTo("adminId", adminId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        DocumentSnapshot adminDoc = snap.getDocuments().get(0);
                        String storedPassword = adminDoc.getString("password");

                        if (storedPassword != null && storedPassword.equals(password)) {
                            saveAdminLogin(adminId);
                            openAdmin(adminId);
                        } else {
                            showLoading(false);
                            Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(this, "Admin not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void saveAdminLogin(String adminId) {
        SharedPreferences prefs = getSharedPreferences("SportsSyncPrefs", MODE_PRIVATE);
        prefs.edit()
                .putString("role", "admin")
                .putString("userId", adminId)
                .apply();
    }

    private void openStudent(String userId, String uucms) {
        Intent serviceIntent = new Intent(this, AutoLogoutService.class);
        try {
            startService(serviceIntent);
        } catch (Exception ignored) {}
        
        Intent i = new Intent(this, StudentDashboardActivity.class);
        i.putExtra("userId", userId);
        i.putExtra("uucms", uucms);
        startActivity(i);
        finish();
    }

    private void openAdmin(String adminId) {
        Intent i = new Intent(this, PtDashboardActivity.class);
        i.putExtra("adminId", adminId);
        startActivity(i);
        finish();
    }
}
