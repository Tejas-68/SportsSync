package com.sportssync.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.sportssync.app.R;
import java.util.HashMap;
import java.util.Map;
import com.sportssync.app.activities.utils.FirebaseManager;
import com.sportssync.app.activities.utils.LoadingDialog;
import com.sportssync.app.activities.utils.PreferenceManager;
import com.sportssync.app.activities.utils.ValidationHelper;

public class LoginActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private View studentLoginLayout;
    private View adminLoginLayout;
    private TextInputEditText etStudentUucms;
    private TextInputEditText etStudentName;
    private TextInputEditText etAdminName;
    private TextInputEditText etAdminPassword;
    private PreferenceManager preferenceManager;
    private FirebaseManager firebaseManager;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        preferenceManager = new PreferenceManager(this);
        firebaseManager = FirebaseManager.getInstance();
        loadingDialog = new LoadingDialog(this);

        initViews();
        setupTabLayout();
        setupClickListeners();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        studentLoginLayout = findViewById(R.id.studentLoginLayout);
        adminLoginLayout = findViewById(R.id.adminLoginLayout);
        etStudentUucms = findViewById(R.id.etStudentUucms);
        etStudentName = findViewById(R.id.etStudentName);
        etAdminName = findViewById(R.id.etAdminName);
        etAdminPassword = findViewById(R.id.etAdminPassword);
    }

    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    studentLoginLayout.setVisibility(View.VISIBLE);
                    adminLoginLayout.setVisibility(View.GONE);
                } else {
                    studentLoginLayout.setVisibility(View.GONE);
                    adminLoginLayout.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupClickListeners() {
        findViewById(R.id.btnStudentLogin).setOnClickListener(v -> handleStudentLogin());
        findViewById(R.id.btnAdminLogin).setOnClickListener(v -> handleAdminLogin());
    }

    private void handleStudentLogin() {
        String uucms = etStudentUucms.getText().toString().trim();
        String name = etStudentName.getText().toString().trim();

        if (uucms.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, R.string.fields_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ValidationHelper.isValidUUCMS(uucms)) {
            Toast.makeText(this, R.string.invalid_uucms, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ValidationHelper.isValidName(name)) {
            Toast.makeText(this, "Invalid name", Toast.LENGTH_SHORT).show();
            return;
        }

        findViewById(R.id.btnStudentLogin).setEnabled(false);
        loadingDialog.show("Logging in...");

        firebaseManager.getDb().collection("users")
                .whereEqualTo("uucmsId", uucms)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot existingUser = queryDocumentSnapshots.getDocuments().get(0);
                        String existingName = existingUser.getString("name");

                        if (existingName != null && existingName.equalsIgnoreCase(name)) {
                            loginExistingStudent(existingUser);
                        } else {
                            loadingDialog.dismiss();
                            findViewById(R.id.btnStudentLogin).setEnabled(true);
                            Toast.makeText(this, "This UUCMS ID is registered with a different name", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        createNewStudentAccount(uucms, name);
                    }
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    findViewById(R.id.btnStudentLogin).setEnabled(true);
                    Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewStudentAccount(String uucms, String name) {
        loadingDialog.updateMessage("Creating account...");

        firebaseManager.getAuth().signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", uid);
                    userData.put("uucmsId", uucms);
                    userData.put("name", name);
                    userData.put("userType", "student");
                    userData.put("isRegistered", false);
                    userData.put("registeredAt", 0);

                    firebaseManager.getDb().collection("users").document(uid)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                loadingDialog.dismiss();
                                preferenceManager.saveUserData(uid, "student", name, uucms);
                                preferenceManager.setRegistered(false);
                                navigateToStudentDashboard();
                            })
                            .addOnFailureListener(e -> {
                                loadingDialog.dismiss();
                                findViewById(R.id.btnStudentLogin).setEnabled(true);
                                Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    findViewById(R.id.btnStudentLogin).setEnabled(true);
                    Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void loginExistingStudent(DocumentSnapshot document) {
        loadingDialog.updateMessage("Loading data...");

        String uid = document.getString("uid");
        String name = document.getString("name");
        String uucms = document.getString("uucmsId");
        boolean isRegistered = document.getBoolean("isRegistered") != null ?
                document.getBoolean("isRegistered") : false;

        preferenceManager.saveUserData(uid, "student", name, uucms);
        preferenceManager.setRegistered(isRegistered);

        loadingDialog.dismiss();
        navigateToStudentDashboard();
    }

    private void handleAdminLogin() {
        String name = etAdminName.getText().toString().trim();
        String password = etAdminPassword.getText().toString().trim();

        if (name.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.fields_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ValidationHelper.isValidPassword(password)) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        findViewById(R.id.btnAdminLogin).setEnabled(false);
        loadingDialog.show("Logging in...");

        firebaseManager.getDb().collection("admins")
                .whereEqualTo("name", name)
                .whereEqualTo("password", password)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loadingDialog.dismiss();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String uid = document.getId();
                        preferenceManager.saveUserData(uid, "admin", name, "");
                        navigateToAdminDashboard();
                    } else {
                        findViewById(R.id.btnAdminLogin).setEnabled(true);
                        Toast.makeText(this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    loadingDialog.dismiss();
                    findViewById(R.id.btnAdminLogin).setEnabled(true);
                    Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToStudentDashboard() {
        startActivity(new Intent(this, StudentDashboardActivity.class));
        finish();
    }

    private void navigateToAdminDashboard() {
        startActivity(new Intent(this, AdminDashboardActivity.class));
        finish();
    }
}