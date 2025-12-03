package com.project.sportssync;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText etUucms, etPassword, etName;
    private FirebaseFirestore db;
    private Button btnLoginStudent, btnLoginAdmin;
    private ImageView ivAppLogo;
    private android.widget.ProgressBar progressBarLogin;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Setup global exception handler to catch crashes
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            android.util.Log.e("CRASH_REPORT", "FATAL EXCEPTION: " + e.getMessage(), e);
            e.printStackTrace();
            // Optional: Write to a file if possible, or just rely on Logcat
        });
        
        sessionManager = new SessionManager(this);
        
        if (sessionManager.isLoggedIn()) {
            navigateToDashboard();
            return;
        }
        
        setContentView(R.layout.activity_login);

        etUucms = findViewById(R.id.etUucms);
        etPassword = findViewById(R.id.etPassword);
        etName = findViewById(R.id.etName);
        btnLoginStudent = findViewById(R.id.btnLogin);
        btnLoginAdmin = findViewById(R.id.btnLoginAdmin);
        ivAppLogo = findViewById(R.id.ivAppLogo);
        progressBarLogin = findViewById(R.id.progressBarLogin);

        db = FirebaseFirestore.getInstance();

        Animation logoAnimation = AnimationUtils.loadAnimation(this, R.anim.logo_bounce);
        Animation cardAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale);
        
        ivAppLogo.startAnimation(logoAnimation);
        findViewById(R.id.loginCard).startAnimation(cardAnimation);

        btnLoginStudent.setOnClickListener(v -> attemptStudentLogin());
        btnLoginAdmin.setOnClickListener(v -> attemptAdminLogin());
        
        // Forgot password enabled
        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }
    
    private void showForgotPasswordDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Please contact your PT or Admin to reset your password.\n\n" +
                        "For security reasons, password resets must be done by an administrator.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void attemptStudentLogin() {
        String uucms = etUucms.getText().toString().trim().toUpperCase();
        String password = etPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();

        // Validate UUCMS
        if (!ValidationUtils.isValidUUCMS(uucms)) {
            Toast.makeText(this, ValidationUtils.getUUCMSErrorMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate Password
        if (!ValidationUtils.isValidPassword(password)) {
            Toast.makeText(this, ValidationUtils.getPasswordErrorMessage(), Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate Name (Required for registration context)
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!ValidationUtils.isValidName(name)) {
            Toast.makeText(this, ValidationUtils.getNameErrorMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Direct Firestore Login (No Firebase Auth)
        db.collection("users")
                .whereEqualTo("uucms", uucms)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        // User found
                        DocumentSnapshot userDoc = snap.getDocuments().get(0);
                        String storedPassword = userDoc.getString("password");
                        Boolean approved = userDoc.getBoolean("approved");
                        String storedName = userDoc.getString("name");
                        
                        // Verify Password
                        if (storedPassword != null && storedPassword.equals(password)) {
                            // Check Approval
                            if (approved != null && approved) {
                                String userId = userDoc.getId();
                                String finalName = storedName != null ? storedName : name;
                                
                                // Save FCM token
                                saveFCMToken(userId);
                                
                                // Login Success
                                sessionManager.createLoginSession(userId, uucms, "student", finalName);
                                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                                openStudentDashboard(userId, uucms, finalName);
                            } else {
                                setLoading(false);
                                Toast.makeText(this, "Account pending PT approval", Toast.LENGTH_LONG).show();
                            }
                        } else {
                            setLoading(false);
                            Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        setLoading(false);
                        // User not found - Offer Registration
                        new android.app.AlertDialog.Builder(this)
                                .setTitle("Account Not Found")
                                .setMessage("No account found with UUCMS: " + uucms + "\n\n" +
                                        "Would you like to register a new account?")
                                .setPositiveButton("Register", (dialog, which) -> {
                                    registerNewStudent(uucms, password, name);
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void registerNewStudent(String uucms, String password, String name) {
        // Double check if user exists (to be safe)
        db.collection("users")
                .whereEqualTo("uucms", uucms)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        Toast.makeText(this, "Account already exists!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Create new user document
                        Map<String, Object> user = new HashMap<>();
                        user.put("uucms", uucms);
                        user.put("password", password); // Storing password in Firestore
                        user.put("name", name);
                        user.put("role", "student");
                        user.put("approved", false);
                        user.put("createdAt", Timestamp.now());
                        user.put("email", uucms.toLowerCase() + "@sportssync.app"); // Keep email field for reference
                        user.put("fcmToken", "");
                        // firebaseUid field is removed as we don't use Auth anymore

                        db.collection("users").add(user)
                                .addOnSuccessListener(ref -> {
                                    // Create approval request
                                    Map<String, Object> approvalReq = new HashMap<>();
                                    approvalReq.put("userId", ref.getId());
                                    approvalReq.put("uucms", uucms);
                                    approvalReq.put("name", name);
                                    approvalReq.put("status", "pending");
                                    approvalReq.put("timestamp", Timestamp.now());

                                    db.collection("approval_requests").add(approvalReq);
                                    
                                    Toast.makeText(this, "Registration successful! Waiting for PT approval.", Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e -> 
                                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                );
                    }
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
    
    /**
     * Save FCM token to Firestore for push notifications
     */
    private void saveFCMToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        db.collection("users")
                                .document(userId)
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> 
                                    android.util.Log.d("FCM", "Token saved successfully"))
                                .addOnFailureListener(e -> 
                                    android.util.Log.e("FCM", "Failed to save token", e));
                    }
                });
    }

    private void attemptAdminLogin() {
        String adminId = etUucms.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (!ValidationUtils.isValidAdminId(adminId)) {
            Toast.makeText(this, "Invalid admin ID format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Admin login with plain-text password comparison (Reverted as requested)
        // Note: This requires Firestore rules to allow public read on 'admins' collection
        db.collection("admins")
                .whereEqualTo("adminId", adminId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        DocumentSnapshot adminDoc = snap.getDocuments().get(0);
                        String storedPassword = adminDoc.getString("password");

                        if (storedPassword != null) {
                            // Plain-text password comparison
                            boolean passwordValid = storedPassword.equals(password);
                            
                            if (passwordValid) {
                                sessionManager.createLoginSession(adminId, adminId, "admin");
                                Toast.makeText(this, "Admin login successful", Toast.LENGTH_SHORT).show();
                                openAdmin(adminId, adminId);
                            } else {
                                setLoading(false);
                                Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            setLoading(false);
                            Toast.makeText(this, "Admin account configuration error", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Admin not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Login failed. Please check internet or security rules.", Toast.LENGTH_LONG).show();
                });
    }

    private void navigateToDashboard() {
        String role = sessionManager.getRole();
        String userId = sessionManager.getUserId();
        String uucms = sessionManager.getUucms();
        String name = sessionManager.getName();

        if ("admin".equals(role)) {
            openAdmin(userId, uucms);
        } else {
            openStudentDashboard(userId, uucms, name);
        }
        finish();
    }

    private void openStudentDashboard(String userId, String uucms, String name) {
        Intent intent = new Intent(this, StudentDashboardActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("uucms", uucms);
        intent.putExtra("name", name);
        startActivity(intent);
        finish();
    }

    private void openAdmin(String userId, String adminId) {
        Intent intent = new Intent(this, PtDashboardActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("adminId", adminId);
        startActivity(intent);
        finish();
    }
    
    
    /**
     * @deprecated SECURITY VULNERABILITY - This method has been removed.
     * Allowing users to delete accounts by UUCMS alone is a security risk.
     * Users must contact admin for account deletion/reset.
     * 
     * Previous implementation allowed deletion of all users with matching UUCMS,
     * which could be exploited if an attacker knows a target's UUCMS.
     */
    @Deprecated
    private void deleteAndRecreateAccount(String email, String newPassword, String uucms, String name) {
        // This method is intentionally disabled for security reasons
        // Users must contact admin for account issues
        Toast.makeText(this, "For security reasons, please contact admin for account assistance", 
                Toast.LENGTH_LONG).show();
    }
    
    
    private void showFirebaseBlockedDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Firebase Security Block")
                .setMessage("Firebase has temporarily blocked requests from this device due to unusual activity.\n\n" +
                        "This is a security measure to protect the app.\n\n" +
                        "What to do:\n" +
                        "1. Wait a few minutes and try again\n" +
                        "2. Check your internet connection\n" +
                        "3. If the problem persists, contact the app administrator\n\n" +
                        "For developers: Ensure SHA fingerprints are properly configured in Firebase Console.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void setLoading(boolean isLoading) {
        if (progressBarLogin != null) {
            progressBarLogin.setVisibility(isLoading ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        if (btnLoginStudent != null) btnLoginStudent.setEnabled(!isLoading);
        if (btnLoginAdmin != null) btnLoginAdmin.setEnabled(!isLoading);
        if (etUucms != null) etUucms.setEnabled(!isLoading);
        if (etPassword != null) etPassword.setEnabled(!isLoading);
        if (etName != null) etName.setEnabled(!isLoading);
    }
}