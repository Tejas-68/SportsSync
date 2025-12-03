package com.project.sportssync;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, tvNoSports;
    private Button btnScanQr, btnExit, btnProfile, btnMyBorrowedItems, btnEditProfile, btnAnalytics, btnAchievements, btnNotifications;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvBorrowedCount, tvAttendanceStatus, tvNotificationBadge;
    private com.google.android.material.card.MaterialCardView cardAttendance, cardBorrowedItems, cardReturnItem;
    private Button btnReturnItem;
    private ProgressBar progressBarSports;
    private RecyclerView recyclerSports;

    private String userId, uucms;
    private FirebaseFirestore db;
    private SessionManager sessionManager;

    // ✅ Activity Result Launcher for QR scan
    private final ActivityResultLauncher<Intent> qrScannerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        IntentResult qrResult = IntentIntegrator.parseActivityResult(
                                IntentIntegrator.REQUEST_CODE, 
                                result.getResultCode(), 
                                result.getData()
                        );
                        
                        if (qrResult != null) {
                            if (qrResult.getContents() != null) {
                                // Successfully scanned QR code
                                sendEntryRequest(qrResult.getContents());
                            } else {
                                // User cancelled the scan
                                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                                        "Scan cancelled", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                            }
                        } else {
                            // Error parsing result
                            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                                    "Error scanning QR code", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                        }
                    });

    private List<SportModel> sportList;
    private SportSimpleAdapter sportAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        userId = getIntent().getStringExtra("userId");
        uucms = getIntent().getStringExtra("uucms");
        String name = getIntent().getStringExtra("name");

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Fallback to SessionManager if intent extras are missing (e.g. from Widget)
        if (userId == null || userId.isEmpty()) {
            if (sessionManager.isLoggedIn()) {
                userId = sessionManager.getUserId();
                uucms = sessionManager.getUucms();
                name = sessionManager.getName();
            } else {
                // Not logged in, redirect to login
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
        }

        // Initialize Views
        tvWelcome = findViewById(R.id.tvWelcome);
        if (name != null && !name.isEmpty()) {
            tvWelcome.setText("Welcome back, " + name + "!");
        } else {
            tvWelcome.setText("Welcome back, " + uucms + "!");
        }
        
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvBorrowedCount = findViewById(R.id.tvBorrowedCount);
        tvAttendanceStatus = findViewById(R.id.tvAttendanceStatus);
        cardAttendance = findViewById(R.id.cardAttendance);
        cardBorrowedItems = findViewById(R.id.cardBorrowedItems);
        cardReturnItem = findViewById(R.id.cardReturnItem);
        btnReturnItem = findViewById(R.id.btnReturnItem);
        progressBarSports = findViewById(R.id.progressBarSports);
        recyclerSports = findViewById(R.id.recyclerSports);
        tvNoSports = findViewById(R.id.tvNoSports);
        Button btnCalendar = findViewById(R.id.btnCalendar);
        
        // Initialize buttons that are referenced later
        btnScanQr = findViewById(R.id.btnScanQr);
        btnExit = findViewById(R.id.btnExit);
        btnProfile = findViewById(R.id.btnProfile);
        btnMyBorrowedItems = findViewById(R.id.btnMyBorrowedItems);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnAnalytics = findViewById(R.id.btnAnalytics);
        btnAchievements = findViewById(R.id.btnAchievements);
        btnNotifications = findViewById(R.id.btnNotifications);
        
        // Make borrowed card clickable
        cardBorrowedItems.setOnClickListener(v -> {
            Intent i = new Intent(this, MyBorrowedItemsActivity.class);
            i.putExtra("userId", userId);
            startActivity(i);
        });



        recyclerSports.setLayoutManager(new LinearLayoutManager(this));
        sportList = new ArrayList<>();
        sportAdapter = new SportSimpleAdapter(sportList, this, userId, uucms);
        recyclerSports.setAdapter(sportAdapter);

        loadSports();

        swipeRefresh.setOnRefreshListener(this::loadSports);

        btnScanQr.setOnClickListener(v -> startQrScanner());
        btnExit.setOnClickListener(v -> logExit());
        btnProfile.setOnClickListener(v -> {
            Intent i = new Intent(this, EditProfileActivity.class);
            startActivity(i);
        });
        btnMyBorrowedItems.setOnClickListener(v -> {
            Intent i = new Intent(this, MyBorrowedItemsActivity.class);
            i.putExtra("userId", userId);
            startActivity(i);
        });

        btnEditProfile.setOnClickListener(v -> {
            Intent i = new Intent(this, EditProfileActivity.class);
            startActivity(i);
        });

        btnAnalytics.setOnClickListener(v -> {
            Intent i = new Intent(this, StudentAnalyticsActivity.class);
            startActivity(i);
        });

        btnCalendar.setOnClickListener(v -> {
            Intent i = new Intent(this, AttendanceCalendarActivity.class);
            i.putExtra("userId", userId);
            startActivity(i);
        });
        
        btnAchievements.setOnClickListener(v -> {
            Intent i = new Intent(this, AchievementsActivity.class);
            i.putExtra("userId", userId);
            startActivity(i);
        });


        
        btnNotifications.setOnClickListener(v -> {
            Intent i = new Intent(this, NotificationsActivity.class);
            i.putExtra("userId", userId);
            startActivity(i);
        });
        
        // Load unread notification count
        loadUnreadNotificationCount();

        // Setup Bottom Navigation
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_calendar) {
                Intent i = new Intent(this, AttendanceCalendarActivity.class);
                i.putExtra("userId", userId);
                startActivity(i);
                return false; // Don't select, as we are leaving the activity
            } else if (itemId == R.id.nav_scan) {
                startQrScanner();
                return false;
            } else if (itemId == R.id.nav_profile) {
                Intent i = new Intent(this, EditProfileActivity.class);
                startActivity(i);
                return false;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_home);

        // Handle Widget Intent
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && "com.project.sportssync.ACTION_SCAN_QR".equals(intent.getAction())) {
            // Wait for activity to be fully created/resumed before scanning
            findViewById(android.R.id.content).postDelayed(this::startQrScanner, 500);
        }
    }

    private void loadSports() {
        if (progressBarSports != null) {
            progressBarSports.setVisibility(View.VISIBLE);
        }
        recyclerSports.setVisibility(View.GONE);
        tvNoSports.setVisibility(View.GONE);

        db.collection("sports")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    sportList.clear();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        SportModel sport = doc.toObject(SportModel.class);
                        sport.setId(doc.getId());
                        sportList.add(sport);
                    }
                    sportAdapter.notifyDataSetChanged();
                    
                    if (progressBarSports != null) {
                        progressBarSports.setVisibility(View.GONE);
                    }
                    if (sportList.isEmpty()) {
                        tvNoSports.setVisibility(View.VISIBLE);
                        recyclerSports.setVisibility(View.GONE);
                    } else {
                        tvNoSports.setVisibility(View.GONE);
                        recyclerSports.setVisibility(View.VISIBLE);
                    }
                    swipeRefresh.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    if (progressBarSports != null) {
                        progressBarSports.setVisibility(View.GONE);
                    }
                    swipeRefresh.setRefreshing(false);
                    Toast.makeText(this, "Failed to load sports", Toast.LENGTH_SHORT).show();
                });
        
        // Load stats
        loadBorrowedItemsCount();
        loadTodayAttendance();
    }

    private void loadBorrowedItemsCount() {
        db.collection("borrowRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "borrowed")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    tvBorrowedCount.setText(String.valueOf(count));
                    
                    // Show/Hide Return Card
                    if (count > 0) {
                        cardReturnItem.setVisibility(View.VISIBLE);
                        // Store the active requests for return action
                        final List<BorrowRequest> activeRequests = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                            BorrowRequest req = doc.toObject(BorrowRequest.class);
                            req.setId(doc.getId());
                            activeRequests.add(req);
                        }
                        
                        btnReturnItem.setOnClickListener(v -> showReturnConfirmationDialog(activeRequests));
                    } else {
                        cardReturnItem.setVisibility(View.GONE);
                    }
                });
    }

    private void showReturnConfirmationDialog(List<BorrowRequest> activeRequests) {
        new AlertDialog.Builder(this)
                .setTitle("Return Items")
                .setMessage("Do you want to send a return request for all borrowed items?")
                .setPositiveButton("Yes, Return All", (dialog, which) -> {
                    // Batch update all active requests to return_pending
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (BorrowRequest req : activeRequests) {
                        com.google.firebase.firestore.DocumentReference ref = db.collection("borrowRequests").document(req.getId());
                        batch.update(ref, "status", "return_pending");
                    }
                    
                    batch.commit()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Return request sent successfully!", Toast.LENGTH_LONG).show();
                                loadBorrowedItemsCount(); // Refresh UI
                            })
                            .addOnFailureListener(e -> 
                                Toast.makeText(this, "Failed to send return request: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadTodayAttendance() {
        // Get today's date (start of day)
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        Timestamp startOfDay = new Timestamp(calendar.getTime());

        // PERFORMANCE OPTIMIZATION: Query only today's records using date range
        // This requires a composite index: (userId ASC, requestedAt ASC)
        // Firebase will provide a link to create the index when you first run this
        db.collection("attendanceRequests")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("requestedAt", startOfDay)
                .orderBy("requestedAt", com.google.firebase.firestore.Query.Direction.ASCENDING) // Ensure explicit ordering
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Get the first (and should be only) record from today
                    String todayStatus = null;
                    
                    if (!querySnapshot.isEmpty()) {
                        // Take the LAST document (most recent) as query is sorted by requestedAt ASC
                        com.google.firebase.firestore.DocumentSnapshot doc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
                        todayStatus = doc.getString("status");
                    }
                    
                    if (todayStatus != null) {
                        if ("approved".equals(todayStatus)) {
                            // Present - Green
                            tvAttendanceStatus.setText(R.string.present);
                            tvAttendanceStatus.setTextColor(getResources().getColor(R.color.attendance_present_text, null));
                            cardAttendance.setCardBackgroundColor(getResources().getColor(R.color.attendance_present_bg, null));
                        } else if ("pending".equals(todayStatus)) {
                            // Pending - Orange
                            tvAttendanceStatus.setText(R.string.pending);
                            tvAttendanceStatus.setTextColor(getResources().getColor(R.color.attendance_pending_text, null));
                            cardAttendance.setCardBackgroundColor(getResources().getColor(R.color.attendance_pending_bg, null));
                        } else {
                            // Denied - Red
                            tvAttendanceStatus.setText(R.string.denied);
                            tvAttendanceStatus.setTextColor(getResources().getColor(R.color.attendance_denied_text, null));
                            cardAttendance.setCardBackgroundColor(getResources().getColor(R.color.attendance_denied_bg, null));
                        }
                    } else {
                        // Absent - Red
                        tvAttendanceStatus.setText(R.string.absent);
                        tvAttendanceStatus.setTextColor(getResources().getColor(R.color.attendance_absent_text, null));
                        cardAttendance.setCardBackgroundColor(getResources().getColor(R.color.attendance_absent_bg, null));
                    }
                });
    }
    
    private void loadUnreadNotificationCount() {
        NotificationsActivity.getUnreadCount(userId, count -> {
            runOnUiThread(() -> {
                if (count > 0) {
                    btnNotifications.setText("Notifications (" + count + ")");
                } else {
                    btnNotifications.setText("Notifications");
                }
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh stats when returning to dashboard
        loadBorrowedItemsCount();
        loadTodayAttendance();
        loadUnreadNotificationCount();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            showLogoutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void startQrScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Scan PT QR Code");
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setCaptureActivity(PortraitCaptureActivity.class);
        integrator.setOrientationLocked(true);
        // ✅ Launch via Activity Result API
        qrScannerLauncher.launch(integrator.createScanIntent());
    }

    private void sendEntryRequest(String qrCode) {
        // Check if already has attendance for today
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        Timestamp startOfDay = new Timestamp(calendar.getTime());
        
        // PERFORMANCE OPTIMIZATION: Query only today's records
        db.collection("attendanceRequests")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("requestedAt", startOfDay)
                .orderBy("requestedAt", com.google.firebase.firestore.Query.Direction.ASCENDING) // Ensure explicit ordering
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Check if any request exists from today
                    boolean hasAttendanceToday = !querySnapshot.isEmpty();
                    String todayStatus = null;
                    
                    if (hasAttendanceToday) {
                        // Get the latest status
                        todayStatus = querySnapshot.getDocuments().get(querySnapshot.size() - 1).getString("status");
                    }
                    
                    if (hasAttendanceToday) {
                        // Already has attendance for today
                        if ("approved".equals(todayStatus)) {
                            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                                    "✓ You are already marked present today!", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
                        } else if ("pending".equals(todayStatus)) {
                            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                                    "⏳ Your attendance request is pending approval", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
                        } else if ("denied".equals(todayStatus)) {
                            // Allow to request again if denied
                            new android.app.AlertDialog.Builder(this)
                                    .setTitle("Previous Request Denied")
                                    .setMessage("Your previous attendance request was denied. Would you like to request again?")
                                    .setPositiveButton("Request Again", (dialog, which) -> {
                                        createAttendanceRequest(qrCode);
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        } else {
                            com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                                    "You already have an attendance record for today", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
                        }
                    } else {
                        // No attendance today, create new request
                        createAttendanceRequest(qrCode);
                    }
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Error checking attendance: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
    
    private void createAttendanceRequest(String qrCode) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("uucms", uucms);
        request.put("sport", qrCode);
        request.put("status", "pending");
        request.put("requestedAt", Timestamp.now());
        
        // Add student name to eliminate N+1 query in PT dashboard
        String studentName = getIntent().getStringExtra("name");
        if (studentName != null && !studentName.isEmpty()) {
            request.put("studentName", studentName);
        }

        db.collection("attendanceRequests")
                .add(request)
                .addOnSuccessListener(ref -> {
                    com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                            "✓ Attendance request sent successfully!", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                    // Refresh attendance status
                    loadTodayAttendance();
                })
                .addOnFailureListener(e -> 
                    com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                            "Error: " + e.getMessage(), com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show());
    }

    private void logExit() {
        // Get today's date (start of day)
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        Timestamp startOfDay = new Timestamp(calendar.getTime());
        
        // PERFORMANCE OPTIMIZATION: Query only today's approved sessions
        // Requires composite index: (userId ASC, status ASC, requestedAt ASC)
        db.collection("attendanceRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "approved")
                .whereGreaterThanOrEqualTo("requestedAt", startOfDay)
                .orderBy("requestedAt", com.google.firebase.firestore.Query.Direction.ASCENDING) // Ensure explicit ordering
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Find today's session that hasn't been exited yet
                    String todayDocId = null;
                    
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot) {
                        // Check if already exited
                        Timestamp exitTime = doc.getTimestamp("exitTime");
                        if (exitTime == null) {
                            todayDocId = doc.getId();
                            break;
                        }
                    }
                    
                    if (todayDocId != null) {
                        // Add exit time, keep status as approved
                        db.collection("attendanceRequests").document(todayDocId)
                                .update("exitTime", Timestamp.now())
                                .addOnSuccessListener(aVoid ->
                                        com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                                                "✓ Exit logged successfully", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                                                "Failed: " + e.getMessage(), com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                                );
                    } else {
                        com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                                "No active session found for today!", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e ->
                        com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                                "Error: " + e.getMessage(), com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
                );
    }
}
