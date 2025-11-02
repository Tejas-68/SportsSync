package com.sportssync.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sportssync.app.R;
import com.sportssync.app.activities.adapters.BorrowedEquipmentAdapter;
import com.sportssync.app.activities.adapters.EquipmentSelectAdapter;
import com.sportssync.app.activities.adapters.SportSelectAdapter;
import com.sportssync.app.activities.models.BorrowRecord;
import com.sportssync.app.activities.models.Equipment;
import com.sportssync.app.activities.models.ReturnRequest;
import com.sportssync.app.activities.models.Sport;
import com.sportssync.app.activities.utils.FirebaseManager;
import com.sportssync.app.activities.utils.NotificationHelper;
import com.sportssync.app.activities.utils.NotificationScheduler;
import com.sportssync.app.activities.utils.PreferenceManager;
import com.sportssync.app.activities.utils.RealtimeUpdateService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentDashboardActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 102;

    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;
    private RecyclerView rvBorrowedEquipment;
    private TextView tvNoEquipment;
    private TextView tvWelcome;
    private TextView tvUserName;
    private PreferenceManager preferenceManager;
    private FirebaseManager firebaseManager;
    private NotificationHelper notificationHelper;
    private RealtimeUpdateService realtimeUpdateService;
    private BorrowedEquipmentAdapter borrowedAdapter;
    private List<BorrowRecord> borrowedList;
    private ActivityResultLauncher<Intent> qrScannerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        preferenceManager = new PreferenceManager(this);
        firebaseManager = FirebaseManager.getInstance();
        notificationHelper = new NotificationHelper(this);
        realtimeUpdateService = new RealtimeUpdateService();
        borrowedList = new ArrayList<>();

        initViews();
        setupBottomNavigation();
        setupRecyclerView();
        setupQRScannerLauncher();
        loadBorrowedEquipment();
        checkRegistrationStatus();
        checkNotificationPermission();
        setupRealtimeUpdates();

        tvUserName.setText(preferenceManager.getUserName());
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        rvBorrowedEquipment = findViewById(R.id.rvBorrowedEquipment);
        tvNoEquipment = findViewById(R.id.tvNoEquipment);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvUserName = findViewById(R.id.tvUserName);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_borrow) {
                handleBorrowClick();
                return true;
            } else if (itemId == R.id.nav_attendance) {
                handleAttendanceClick();
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void handleBorrowClick() {
        if (!preferenceManager.isRegistered()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("First Time Setup")
                    .setMessage("Welcome to SportsSync!\n\nTo get started:\n1. Scan the REGISTRATION QR code provided by admin\n2. After registration, you can borrow sports equipment\n\nReady to scan?")
                    .setPositiveButton("Scan Now", (dialog, which) -> {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.CAMERA},
                                    CAMERA_PERMISSION_REQUEST);
                        } else {
                            openQRScanner("registration");
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        CAMERA_PERMISSION_REQUEST);
            } else {
                showSportSelectionDialog();
            }
        }
    }

    private void handleAttendanceClick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            openQRScanner("attendance");
        }
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST);
            }
        }
    }

    private void setupRecyclerView() {
        borrowedAdapter = new BorrowedEquipmentAdapter(borrowedList, this::handleReturnRequest);
        rvBorrowedEquipment.setLayoutManager(new LinearLayoutManager(this));
        rvBorrowedEquipment.setAdapter(borrowedAdapter);
    }

    private void setupQRScannerLauncher() {
        qrScannerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String qrData = result.getData().getStringExtra("qrData");
                        handleQRResult(qrData);
                    }
                }
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (preferenceManager.isRegistered()) {
                    showSportSelectionDialog();
                } else {
                    openQRScanner("registration");
                }
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupRealtimeUpdates() {
        String studentId = preferenceManager.getUserId();

        realtimeUpdateService.startListeningForAttendanceUpdates(studentId, status -> {
            runOnUiThread(() -> {
                notificationHelper.showAttendanceResponse(status);
                if (status.equals("approved")) {
                    Toast.makeText(this, "Your attendance has been approved!", Toast.LENGTH_SHORT).show();
                } else if (status.equals("rejected")) {
                    Toast.makeText(this, "Your attendance request was rejected", Toast.LENGTH_SHORT).show();
                }
            });
        });

        realtimeUpdateService.startListeningForReturnUpdates(studentId, status -> {
            runOnUiThread(() -> {
                if (status.equals("approved")) {
                    Toast.makeText(this, "Equipment return approved", Toast.LENGTH_SHORT).show();
                    loadBorrowedEquipment();
                } else if (status.equals("rejected")) {
                    Toast.makeText(this, "Return request rejected", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void checkRegistrationStatus() {
        String userId = preferenceManager.getUserId();
        firebaseManager.getDb().collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        boolean isRegistered = documentSnapshot.getBoolean("isRegistered") != null ?
                                documentSnapshot.getBoolean("isRegistered") : false;
                        preferenceManager.setRegistered(isRegistered);
                    }
                });
    }

    private void openQRScanner(String scanType) {
        Intent intent = new Intent(this, QRScannerActivity.class);
        intent.putExtra("scanType", scanType);
        qrScannerLauncher.launch(intent);
    }

    private void handleQRResult(String qrData) {
        if (qrData.equals("REGISTER_SPORTS_SYNC")) {
            registerStudent();
        } else if (qrData.startsWith("ATTENDANCE_")) {
            submitAttendanceRequest();
        }
    }

    private void registerStudent() {
        String userId = preferenceManager.getUserId();
        Map<String, Object> updates = new HashMap<>();
        updates.put("isRegistered", true);
        updates.put("registeredAt", System.currentTimeMillis());

        firebaseManager.getDb().collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    preferenceManager.setRegistered(true);
                    Toast.makeText(this, R.string.registration_success, Toast.LENGTH_SHORT).show();
                    showSportSelectionDialog();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void showSportSelectionDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_sport, null);
        RecyclerView rvSports = dialogView.findViewById(R.id.rvSports);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        loadSports(rvSports, dialog);
        dialog.show();
    }

    private void loadSports(RecyclerView recyclerView, androidx.appcompat.app.AlertDialog dialog) {
        firebaseManager.getDb().collection("sports")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Sport> sportsList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Sport sport = document.toObject(Sport.class);
                        sportsList.add(sport);
                    }

                    SportSelectAdapter adapter = new SportSelectAdapter(sportsList, sport -> {
                        dialog.dismiss();
                        showEquipmentSelectionDialog(sport);
                    });
                    recyclerView.setLayoutManager(new LinearLayoutManager(this));
                    recyclerView.setAdapter(adapter);
                });
    }

    private void showEquipmentSelectionDialog(Sport sport) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_select_equipment, null);
        TextView tvSportName = dialogView.findViewById(R.id.tvSportName);
        RecyclerView rvEquipment = dialogView.findViewById(R.id.rvEquipment);

        tvSportName.setText(sport.getSportName());

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        loadEquipment(sport, rvEquipment, dialog);
        dialog.show();
    }

    private void loadEquipment(Sport sport, RecyclerView recyclerView, androidx.appcompat.app.AlertDialog dialog) {
        List<Equipment> equipmentList = sport.getEquipmentList();

        EquipmentSelectAdapter adapter = new EquipmentSelectAdapter(
                equipmentList,
                (equipment, quantity) -> {
                    dialog.dismiss();
                    borrowEquipment(sport, equipment, quantity);
                }
        );
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void borrowEquipment(Sport sport, Equipment equipment, int quantity) {
        String recordId = firebaseManager.getDb().collection("borrowRecords").document().getId();

        long returnBy = System.currentTimeMillis() + (17 * 60 * 60 * 1000);

        BorrowRecord record = new BorrowRecord(
                recordId,
                preferenceManager.getUserId(),
                preferenceManager.getUserName(),
                sport.getSportId(),
                sport.getSportName(),
                equipment.getEquipmentId(),
                equipment.getEquipmentName(),
                quantity,
                returnBy
        );

        Map<String, Object> recordData = new HashMap<>();
        recordData.put("recordId", record.getRecordId());
        recordData.put("studentId", record.getStudentId());
        recordData.put("studentName", record.getStudentName());
        recordData.put("sportId", record.getSportId());
        recordData.put("sportName", record.getSportName());
        recordData.put("equipmentId", record.getEquipmentId());
        recordData.put("equipmentName", record.getEquipmentName());
        recordData.put("quantity", record.getQuantity());
        recordData.put("borrowedAt", record.getBorrowedAt());
        recordData.put("returnBy", record.getReturnBy());
        recordData.put("status", record.getStatus());
        recordData.put("returnedAt", record.getReturnedAt());

        firebaseManager.getDb().collection("borrowRecords").document(recordId)
                .set(recordData)
                .addOnSuccessListener(aVoid -> {
                    updateEquipmentQuantity(sport.getSportId(), equipment.getEquipmentId(), quantity, false);

                    NotificationScheduler scheduler = new NotificationScheduler(this);
                    scheduler.scheduleReturnReminder(recordId, equipment.getEquipmentName(), returnBy);

                    Toast.makeText(this, R.string.equipment_borrowed, Toast.LENGTH_SHORT).show();
                    notificationHelper.showEquipmentBorrowed(equipment.getEquipmentName());
                    loadBorrowedEquipment();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to borrow equipment", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEquipmentQuantity(String sportId, String equipmentId, int quantity, boolean isReturn) {
        firebaseManager.getDb().collection("sports").document(sportId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Sport sport = documentSnapshot.toObject(Sport.class);
                        if (sport != null) {
                            for (Equipment eq : sport.getEquipmentList()) {
                                if (eq.getEquipmentId().equals(equipmentId)) {
                                    if (isReturn) {
                                        eq.setAvailableQuantity(eq.getAvailableQuantity() + quantity);
                                    } else {
                                        eq.setAvailableQuantity(eq.getAvailableQuantity() - quantity);
                                    }
                                    break;
                                }
                            }
                            firebaseManager.getDb().collection("sports").document(sportId)
                                    .set(sport);
                        }
                    }
                });
    }

    private void loadBorrowedEquipment() {
        String studentId = preferenceManager.getUserId();
        firebaseManager.getDb().collection("borrowRecords")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", "borrowed")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    borrowedList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        BorrowRecord record = document.toObject(BorrowRecord.class);
                        borrowedList.add(record);
                    }
                    borrowedAdapter.notifyDataSetChanged();

                    if (borrowedList.isEmpty()) {
                        tvNoEquipment.setVisibility(View.VISIBLE);
                        rvBorrowedEquipment.setVisibility(View.GONE);
                    } else {
                        tvNoEquipment.setVisibility(View.GONE);
                        rvBorrowedEquipment.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void handleReturnRequest(BorrowRecord record) {
        String requestId = firebaseManager.getDb().collection("returnRequests").document().getId();

        ReturnRequest returnRequest = new ReturnRequest(
                requestId,
                record.getRecordId(),
                record.getStudentId(),
                record.getStudentName(),
                record.getEquipmentName(),
                record.getQuantity()
        );

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("requestId", returnRequest.getRequestId());
        requestData.put("recordId", returnRequest.getRecordId());
        requestData.put("studentId", returnRequest.getStudentId());
        requestData.put("studentName", returnRequest.getStudentName());
        requestData.put("equipmentName", returnRequest.getEquipmentName());
        requestData.put("quantity", returnRequest.getQuantity());
        requestData.put("requestedAt", returnRequest.getRequestedAt());
        requestData.put("status", returnRequest.getStatus());
        requestData.put("respondedAt", returnRequest.getRespondedAt());

        firebaseManager.getDb().collection("returnRequests").document(requestId)
                .set(requestData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.return_request_sent, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to send return request", Toast.LENGTH_SHORT).show();
                });
    }

    private void submitAttendanceRequest() {
        String requestId = firebaseManager.getDb().collection("attendanceRequests").document().getId();

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("requestId", requestId);
        requestData.put("studentId", preferenceManager.getUserId());
        requestData.put("studentName", preferenceManager.getUserName());
        requestData.put("uucmsId", preferenceManager.getUucmsId());
        requestData.put("requestedAt", System.currentTimeMillis());
        requestData.put("status", "pending");
        requestData.put("respondedAt", 0);
        requestData.put("respondedBy", "");

        firebaseManager.getDb().collection("attendanceRequests").document(requestId)
                .set(requestData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.attendance_marked, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit attendance", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realtimeUpdateService != null) {
            realtimeUpdateService.stopListening();
        }
    }
}