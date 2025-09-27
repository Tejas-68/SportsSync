package com.project.sportssync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String userId;
    private String uucms;
    private String role;
    private LinearLayout llAttendanceList;
    private androidx.appcompat.widget.AppCompatButton btnExportAttendance;
    private List<Map<String, Object>> attendanceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");
        role = getIntent().getStringExtra("role");

        SharedPreferences prefs = com.project.sportssync.security.SecurePrefs.get(this);
        uucms = prefs.getString("uucms", null);

        llAttendanceList = findViewById(R.id.llAttendanceList);
        btnExportAttendance = findViewById(R.id.btnExportAttendance);

        loadAttendance();

        btnExportAttendance.setOnClickListener(v -> {
            exportAttendanceToCsv(attendanceList, "attendance_export");
        });

    }


//QR feature
    private void startQrScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan QR Code");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
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
            }
        }
    }
//verifying the qr
    private void validateQr(String qrValue) {
        db.collection("settings").document("app")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String currentQr = doc.getString("qrCode");
                        if (currentQr != null && currentQr.equals(qrValue)) {
                            showSportSelectionDialog(qrValue);
                        } else {
                            Toast.makeText(this, "Invalid QR. Ask PT for latest QR.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, "App QR not set. Contact admin.", Toast.LENGTH_LONG).show();
                    }
                });
    }
//after scan option to select the sports and if none then select none
    private void showSportSelectionDialog(String qrValue) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Sport");

        final Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"None", "Football", "Cricket", "Basketball", "Badminton", "Tennis"} // add more as needed
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
            if (selectedSport.equals("None")) {
                Toast.makeText(this, "Please select a sport to continue", Toast.LENGTH_LONG).show();
                return;
            }
            sendEntryRequest(qrValue, selectedSport);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void sendEntryRequest(String qrValue, String sport) {
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
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Entry request sent", Toast.LENGTH_LONG).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // ATTENDANCE EXPORT

    private void loadAttendance() {
        db.collection("attendance").whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(qs -> {
                    llAttendanceList.removeAllViews();
                    attendanceList.clear();
                    for (QueryDocumentSnapshot doc : qs) {
                        String date = doc.getString("date");
                        String status = doc.getString("status");
                        Map<String, Object> row = new HashMap<>();
                        row.put("date", date);
                        row.put("status", status);
                        row.put("userId", userId);
                        attendanceList.add(row);

                        TextView tv = new TextView(this);
                        tv.setText(date + " - " + status);
                        llAttendanceList.addView(tv);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void exportAttendanceToCsv(List<Map<String, Object>> attendanceList, String filename) {
        StringBuilder sb = new StringBuilder();
        sb.append("Date,Status,UserId\n");
        for (Map<String, Object> row : attendanceList) {
            String date = row.get("date") != null ? row.get("date").toString() : "";
            String status = row.get("status") != null ? row.get("status").toString() : "";
            String uid = row.get("userId") != null ? row.get("userId").toString() : "";
            sb.append(date).append(",").append(status).append(",").append(uid).append("\n");
        }
        try {
            File dir = new File(getExternalFilesDir(null), "exports");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, filename + ".csv");
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(sb.toString().getBytes());
            fos.close();

            Intent intentShare = new Intent(Intent.ACTION_SEND);
            intentShare.setType("text/csv");
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            intentShare.putExtra(Intent.EXTRA_STREAM, uri);
            intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intentShare, "Share CSV"));
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
