package com.project.sportssync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class QrScanActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userId;
    private String uucms;
    private String lastScannedQr = null;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        db = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.progressBar);

        SharedPreferences prefs = com.project.sportssync.security.SecurePrefs.get(this);
        userId = prefs.getString("userId", null);
        uucms = prefs.getString("uucms", null);

        if (userId == null || uucms == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            prefs.edit().clear().apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        startQrScanner();
    }

    private void startQrScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan QR Code");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.setCaptureActivity(CustomCaptureActivity.class);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String qrValue = result.getContents();
                validateQr(qrValue);
            } else {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            finish();
        }
    }

    private void validateQr(String qrValue) {
        progressBar.setVisibility(View.VISIBLE);
        checkTodayAttendance(qrValue);
    }

    private void checkTodayAttendance(String qrValue) {
        String today = getCurrentDate();

        db.collection("attendance")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Already present for today", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    checkPendingRequest(qrValue);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to check attendance: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void checkPendingRequest(String qrValue) {
        String today = getCurrentDate();

        db.collection("attendanceRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", today)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "You already have a pending request for today", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    checkApprovedRequest(qrValue);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to check requests: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void checkApprovedRequest(String qrValue) {
        String today = getCurrentDate();

        db.collection("attendanceRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", today)
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Already present for today", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    validateQrCode(qrValue);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to check approved requests: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void validateQrCode(String qrValue) {
        db.collection("settings").document("app")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String currentQr = doc.getString("qrCode");
                        if (currentQr != null && currentQr.equals(qrValue)) {
                            lastScannedQr = qrValue;
                            progressBar.setVisibility(View.GONE);
                            showSportSelectionDialog(qrValue);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Invalid QR. Ask PT for latest QR.", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "App QR not set. Contact admin.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to validate QR: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void showSportSelectionDialog(String qrValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Sport");

        final Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"None", "Football", "Cricket", "Basketball", "Badminton", "Tennis"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        LinearLayout container = new LinearLayout(this);
        container.setPadding(50, 20, 50, 20);
        container.addView(spinner, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        builder.setView(container);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            String selectedSport = spinner.getSelectedItem().toString();
            if ("None".equals(selectedSport)) {
                Toast.makeText(this, "Please select a sport to continue", Toast.LENGTH_LONG).show();
                showSportSelectionDialog(qrValue);
                return;
            }
            sendEntryRequest(qrValue, selectedSport);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
            finish();
        });

        builder.setOnCancelListener(dialog -> finish());
        builder.show();
    }

    private void sendEntryRequest(String qrValue, String sport) {
        if (userId == null || uucms == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            SharedPreferences prefs = com.project.sportssync.security.SecurePrefs.get(this);
            prefs.edit().clear().apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        String reqId = UUID.randomUUID().toString();
        String today = getCurrentDate();
        Map<String, Object> req = new HashMap<>();
        req.put("userId", userId);
        req.put("uucms", uucms);
        req.put("sport", sport);
        req.put("qrId", qrValue);
        req.put("status", "pending");
        req.put("date", today);
        req.put("requestedAt", Timestamp.now());

        db.collection("attendanceRequests").document(reqId)
                .set(req)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Entry request sent", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to send request: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }
}