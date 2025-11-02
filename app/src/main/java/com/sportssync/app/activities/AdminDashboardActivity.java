package com.sportssync.app.activities;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sportssync.app.R;
import com.sportssync.app.activities.adapters.AttendanceRequestAdapter;
import com.sportssync.app.activities.adapters.ReturnRequestAdapter;
import com.sportssync.app.activities.models.AttendanceRequest;
import com.sportssync.app.activities.models.BorrowRecord;
import com.sportssync.app.activities.models.Equipment;
import com.sportssync.app.activities.models.ReturnRequest;
import com.sportssync.app.activities.models.Sport;
import com.sportssync.app.activities.utils.ExcelExporter;
import com.sportssync.app.activities.utils.FirebaseManager;
import com.sportssync.app.activities.utils.NotificationScheduler;
import com.sportssync.app.activities.utils.PreferenceManager;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_REQUEST = 101;

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvActiveSportsCount;
    private TextView tvBorrowedCount;
    private RecyclerView rvAttendanceRequests;
    private RecyclerView rvReturnRequests;
    private MaterialButton btnApproveAll;
    private MaterialButton btnRejectAll;
    private PreferenceManager preferenceManager;
    private FirebaseManager firebaseManager;
    private AttendanceRequestAdapter attendanceAdapter;
    private ReturnRequestAdapter returnAdapter;
    private List<AttendanceRequest> attendanceList;
    private List<ReturnRequest> returnList;
    private boolean isLoadingAttendance = false;
    private boolean isLoadingReturns = false;
    private String selectedPeriod = "today";
    private int developerClickCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        preferenceManager = new PreferenceManager(this);
        firebaseManager = FirebaseManager.getInstance();
        attendanceList = new ArrayList<>();
        returnList = new ArrayList<>();

        initViews();
        setupBottomNavigation();
        setupRecyclerViews();
        setupClickListeners();
        setupSwipeRefresh();
        loadDashboardData();
        initializeDefaultData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvActiveSportsCount = findViewById(R.id.tvActiveSportsCount);
        tvBorrowedCount = findViewById(R.id.tvBorrowedCount);
        rvAttendanceRequests = findViewById(R.id.rvAttendanceRequests);
        rvReturnRequests = findViewById(R.id.rvReturnRequests);
        btnApproveAll = findViewById(R.id.btnApproveAll);
        btnRejectAll = findViewById(R.id.btnRejectAll);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_dashboard) {
                return true;
            } else if (itemId == R.id.nav_sports) {
                startActivity(new Intent(this, ManageSportsActivity.class));
                return true;
            } else if (itemId == R.id.nav_export) {
                showExportDialog();
                return true;
            } else if (itemId == R.id.nav_settings) {
                showSettingsOptions();
                return true;
            }
            return false;
        });
    }

    private void showSettingsOptions() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Settings")
                .setItems(new String[]{"Change Attendance QR", "Developer Info", "Logout"}, (dialog, which) -> {
                    if (which == 0) {
                        showChangeQRDialog();
                    } else if (which == 1) {
                        showDeveloperInfo();
                    } else if (which == 2) {
                        logout();
                    }
                })
                .show();
    }

    private void showDeveloperInfo() {
        developerClickCount++;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_sport, null);
        TextView tvInfo = new TextView(this);
        tvInfo.setText("Developed by\n\nTejas N C");
        tvInfo.setTextSize(16);
        tvInfo.setGravity(android.view.Gravity.CENTER);
        tvInfo.setPadding(48, 48, 48, 48);
        tvInfo.setTextColor(getResources().getColor(R.color.text_primary_light));

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(tvInfo)
                .setPositiveButton("Close", null)
                .create();

        tvInfo.setOnClickListener(v -> {
            if (developerClickCount >= 7) {
                dialog.dismiss();
                showPasswordResetQR();
                developerClickCount = 0;
            }
        });

        dialog.show();
    }

    private void showPasswordResetQR() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Password Reset QR")
                .setMessage("QR Code: RESET_PASSWORD_SPORTS_SYNC\n\nGenerate QR with this text for students to reset password")
                .setPositiveButton("Copy", (d, w) -> {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Reset QR", "RESET_PASSWORD_SPORTS_SYNC");
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void setupRecyclerViews() {
        attendanceAdapter = new AttendanceRequestAdapter(attendanceList, new AttendanceRequestAdapter.OnActionClickListener() {
            @Override
            public void onApprove(AttendanceRequest request) {
                approveAttendance(request);
            }

            @Override
            public void onReject(AttendanceRequest request) {
                rejectAttendance(request);
            }
        });
        rvAttendanceRequests.setLayoutManager(new LinearLayoutManager(this));
        rvAttendanceRequests.setAdapter(attendanceAdapter);

        returnAdapter = new ReturnRequestAdapter(returnList, new ReturnRequestAdapter.OnActionClickListener() {
            @Override
            public void onApprove(ReturnRequest request) {
                approveReturn(request);
            }

            @Override
            public void onReject(ReturnRequest request) {
                rejectReturn(request);
            }
        });
        rvReturnRequests.setLayoutManager(new LinearLayoutManager(this));
        rvReturnRequests.setAdapter(returnAdapter);
    }

    private void setupClickListeners() {
        btnApproveAll.setOnClickListener(v -> approveAllAttendance());
        btnRejectAll.setOnClickListener(v -> rejectAllAttendance());
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener(() -> {
            loadDashboardData();
            swipeRefresh.setRefreshing(false);
        });
    }

    private void loadDashboardData() {
        loadActiveSportsCount();
        loadBorrowedCount();
        loadAttendanceRequests();
        loadReturnRequests();
    }

    private void loadActiveSportsCount() {
        firebaseManager.getDb().collection("sports")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tvActiveSportsCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                });
    }

    private void loadBorrowedCount() {
        firebaseManager.getDb().collection("borrowRecords")
                .whereEqualTo("status", "borrowed")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tvBorrowedCount.setText(String.valueOf(queryDocumentSnapshots.size()));
                });
    }

    private void loadAttendanceRequests() {
        if (isLoadingAttendance) return;
        isLoadingAttendance = true;

        firebaseManager.getDb().collection("attendanceRequests")
                .orderBy("requestedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attendanceList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        AttendanceRequest request = document.toObject(AttendanceRequest.class);
                        attendanceList.add(request);
                    }
                    attendanceAdapter.notifyDataSetChanged();
                    isLoadingAttendance = false;
                })
                .addOnFailureListener(e -> {
                    isLoadingAttendance = false;
                    Toast.makeText(this, "Failed to load attendance requests", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadReturnRequests() {
        if (isLoadingReturns) return;
        isLoadingReturns = true;

        firebaseManager.getDb().collection("returnRequests")
                .orderBy("requestedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    returnList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        ReturnRequest request = document.toObject(ReturnRequest.class);
                        returnList.add(request);
                    }
                    returnAdapter.notifyDataSetChanged();
                    isLoadingReturns = false;
                })
                .addOnFailureListener(e -> {
                    isLoadingReturns = false;
                    Toast.makeText(this, "Failed to load return requests", Toast.LENGTH_SHORT).show();
                });
    }

    private void approveAttendance(AttendanceRequest request) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "approved");
        updates.put("respondedAt", System.currentTimeMillis());
        updates.put("respondedBy", preferenceManager.getUserName());

        firebaseManager.getDb().collection("attendanceRequests").document(request.getRequestId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Attendance approved", Toast.LENGTH_SHORT).show();
                    loadAttendanceRequests();
                });
    }

    private void rejectAttendance(AttendanceRequest request) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "rejected");
        updates.put("respondedAt", System.currentTimeMillis());
        updates.put("respondedBy", preferenceManager.getUserName());

        firebaseManager.getDb().collection("attendanceRequests").document(request.getRequestId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Attendance rejected", Toast.LENGTH_SHORT).show();
                    loadAttendanceRequests();
                });
    }

    private void approveAllAttendance() {
        for (AttendanceRequest request : attendanceList) {
            if (request.getStatus().equals("pending")) {
                approveAttendance(request);
            }
        }
    }

    private void rejectAllAttendance() {
        for (AttendanceRequest request : attendanceList) {
            if (request.getStatus().equals("pending")) {
                rejectAttendance(request);
            }
        }
    }

    private void approveReturn(ReturnRequest request) {
        Map<String, Object> returnUpdates = new HashMap<>();
        returnUpdates.put("status", "approved");
        returnUpdates.put("respondedAt", System.currentTimeMillis());

        firebaseManager.getDb().collection("returnRequests").document(request.getRequestId())
                .update(returnUpdates)
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> recordUpdates = new HashMap<>();
                    recordUpdates.put("status", "returned");
                    recordUpdates.put("returnedAt", System.currentTimeMillis());

                    firebaseManager.getDb().collection("borrowRecords").document(request.getRecordId())
                            .update(recordUpdates)
                            .addOnSuccessListener(aVoid2 -> {
                                NotificationScheduler scheduler = new NotificationScheduler(this);
                                scheduler.cancelReturnReminder(request.getRecordId());

                                firebaseManager.getDb().collection("borrowRecords").document(request.getRecordId())
                                        .get()
                                        .addOnSuccessListener(documentSnapshot -> {
                                            if (documentSnapshot.exists()) {
                                                BorrowRecord record = documentSnapshot.toObject(BorrowRecord.class);
                                                if (record != null) {
                                                    updateEquipmentQuantityOnReturn(record.getSportId(),
                                                            record.getEquipmentId(), record.getQuantity());
                                                }
                                            }
                                        });

                                Toast.makeText(this, "Return approved", Toast.LENGTH_SHORT).show();
                                loadReturnRequests();
                                loadBorrowedCount();
                            });
                });
    }

    private void rejectReturn(ReturnRequest request) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "rejected");
        updates.put("respondedAt", System.currentTimeMillis());

        firebaseManager.getDb().collection("returnRequests").document(request.getRequestId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Return rejected", Toast.LENGTH_SHORT).show();
                    loadReturnRequests();
                });
    }

    private void updateEquipmentQuantityOnReturn(String sportId, String equipmentId, int quantity) {
        firebaseManager.getDb().collection("sports").document(sportId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Sport sport = documentSnapshot.toObject(Sport.class);
                        if (sport != null) {
                            for (Equipment eq : sport.getEquipmentList()) {
                                if (eq.getEquipmentId().equals(equipmentId)) {
                                    eq.setAvailableQuantity(eq.getAvailableQuantity() + quantity);
                                    break;
                                }
                            }
                            firebaseManager.getDb().collection("sports").document(sportId)
                                    .set(sport);
                        }
                    }
                });
    }

    private void initializeDefaultData() {
        firebaseManager.getDb().collection("sports")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        createDefaultSports();
                    }
                });

        firebaseManager.getDb().collection("admins")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        createDefaultAdmin();
                    }
                });

        firebaseManager.getDb().collection("qrCodes").document("attendance")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Map<String, Object> qrData = new HashMap<>();
                        qrData.put("code", "ATTENDANCE_" + System.currentTimeMillis());
                        qrData.put("createdAt", System.currentTimeMillis());
                        firebaseManager.getDb().collection("qrCodes").document("attendance")
                                .set(qrData);
                    }
                });
    }

    private void createDefaultSports() {
        String[] sportNames = {"Cricket", "Table Tennis", "Badminton", "Chess", "Volleyball", "Throwball"};

        for (String sportName : sportNames) {
            String sportId = firebaseManager.getDb().collection("sports").document().getId();
            Sport sport = new Sport(sportId, sportName);

            List<Equipment> equipmentList = new ArrayList<>();
            if (sportName.equals("Cricket")) {
                equipmentList.add(new Equipment("eq1", "Cricket Bat", 10));
                equipmentList.add(new Equipment("eq2", "Cricket Ball", 20));
            } else if (sportName.equals("Table Tennis")) {
                equipmentList.add(new Equipment("eq3", "TT Racket", 15));
                equipmentList.add(new Equipment("eq4", "TT Ball", 30));
            } else if (sportName.equals("Badminton")) {
                equipmentList.add(new Equipment("eq5", "Badminton Racket", 12));
                equipmentList.add(new Equipment("eq6", "Shuttlecock", 25));
            } else if (sportName.equals("Chess")) {
                equipmentList.add(new Equipment("eq7", "Chess Board", 8));
            } else if (sportName.equals("Volleyball")) {
                equipmentList.add(new Equipment("eq8", "Volleyball", 10));
            } else if (sportName.equals("Throwball")) {
                equipmentList.add(new Equipment("eq9", "Throwball", 8));
            }

            sport.setEquipmentList(equipmentList);

            Map<String, Object> sportData = new HashMap<>();
            sportData.put("sportId", sport.getSportId());
            sportData.put("sportName", sport.getSportName());
            sportData.put("equipmentList", equipmentList);
            sportData.put("isActive", sport.isActive());
            sportData.put("createdAt", sport.getCreatedAt());

            firebaseManager.getDb().collection("sports").document(sportId)
                    .set(sportData);
        }
    }

    private void createDefaultAdmin() {
        Map<String, Object> adminData = new HashMap<>();
        adminData.put("name", "Admin");
        adminData.put("password", "admin123");
        adminData.put("createdAt", System.currentTimeMillis());

        firebaseManager.getDb().collection("admins").document()
                .set(adminData);
    }

    private void showChangeQRDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_sport, null);
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Current Attendance QR Code");
        tvTitle.setTextSize(18);
        tvTitle.setPadding(48, 32, 48, 16);
        tvTitle.setTextColor(getResources().getColor(R.color.text_primary_light));

        TextView tvCurrentCode = new TextView(this);
        tvCurrentCode.setTextSize(14);
        tvCurrentCode.setPadding(48, 16, 48, 32);
        tvCurrentCode.setTextColor(getResources().getColor(R.color.primary));
        tvCurrentCode.setTextIsSelectable(true);

        firebaseManager.getDb().collection("qrCodes").document("attendance")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String currentCode = documentSnapshot.getString("code");
                        tvCurrentCode.setText(currentCode != null ? currentCode : "No code generated yet");
                    }
                });

        tvCurrentCode.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("QR Code", tvCurrentCode.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Code copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setCustomTitle(tvTitle)
                .setView(tvCurrentCode)
                .setPositiveButton("Generate New", (d, which) -> {
                    String newCode = "ATTENDANCE_" + System.currentTimeMillis();
                    Map<String, Object> qrData = new HashMap<>();
                    qrData.put("code", newCode);
                    qrData.put("createdAt", System.currentTimeMillis());

                    firebaseManager.getDb().collection("qrCodes").document("attendance")
                            .set(qrData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "QR Code updated successfully", Toast.LENGTH_SHORT).show();
                                showChangeQRDialog();
                            });
                })
                .setNegativeButton("Close", null)
                .create();

        dialog.show();
    }

    private void showExportDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_export_attendance, null);
        RadioGroup rgPeriod = dialogView.findViewById(R.id.rgPeriod);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        rgPeriod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbToday) {
                selectedPeriod = "today";
            } else if (checkedId == R.id.rbWeekly) {
                selectedPeriod = "weekly";
            } else if (checkedId == R.id.rbMonthly) {
                selectedPeriod = "monthly";
            } else if (checkedId == R.id.rbAll) {
                selectedPeriod = "all";
            }
        });

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnExport).setOnClickListener(v -> {
            dialog.dismiss();
            checkStoragePermissionAndExport();
        });

        dialog.show();
    }

    private void checkStoragePermissionAndExport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_REQUEST);
            } else {
                exportAttendance();
            }
        } else {
            exportAttendance();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportAttendance();
            } else {
                Toast.makeText(this, "Storage permission required to export", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void exportAttendance() {
        long startTime = getStartTimeForPeriod(selectedPeriod);

        firebaseManager.getDb().collection("attendanceRequests")
                .whereGreaterThanOrEqualTo("requestedAt", startTime)
                .orderBy("requestedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AttendanceRequest> attendanceList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        AttendanceRequest request = document.toObject(AttendanceRequest.class);
                        attendanceList.add(request);
                    }

                    if (attendanceList.isEmpty()) {
                        Toast.makeText(this, "No attendance records found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    ExcelExporter exporter = new ExcelExporter(this);
                    exporter.exportAttendance(attendanceList, selectedPeriod);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch attendance", Toast.LENGTH_SHORT).show();
                });
    }

    private long getStartTimeForPeriod(String period) {
        Calendar calendar = Calendar.getInstance();

        switch (period) {
            case "today":
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                return calendar.getTimeInMillis();

            case "weekly":
                calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                return calendar.getTimeInMillis();

            case "monthly":
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                return calendar.getTimeInMillis();

            default:
                return 0;
        }
    }

    private void logout() {
        preferenceManager.clearData();
        firebaseManager.getAuth().signOut();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}