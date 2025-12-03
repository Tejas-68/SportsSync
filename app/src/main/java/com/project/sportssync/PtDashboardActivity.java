package com.project.sportssync;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.util.Calendar;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PtDashboardActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recyclerRequests;
    private RequestAdapter requestAdapter;
    private List<RequestModel> requestList, filteredList;
    private SessionManager sessionManager;
    private SwipeRefreshLayout swipeRefresh;
    private androidx.appcompat.widget.SearchView searchView;
    private com.google.android.material.chip.Chip chipAll, chipAttendance, chipReturn;
    private String currentFilter = "all"; // "all", "attendance", "return"
    private TextView tvStudentsPresentCount, tvBorrowedEquipmentCount;

    private EditText etSearchAchievement;
    private Button btnSearchAchievement, btnExportExcel, btnGenerateQR, btnManageApprovals;
    private LinearLayout llAchievements;
    private ProgressBar progressBarRequests;
    private BackupManager backupManager;

    private List<Map<String, Object>> attendanceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pt_dashboard);

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        backupManager = new BackupManager(this);

        recyclerRequests = findViewById(R.id.recyclerRequests);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        searchView = findViewById(R.id.searchView);
        chipAll = findViewById(R.id.chipAll);
        chipAttendance = findViewById(R.id.chipAttendance);
        chipReturn = findViewById(R.id.chipReturn);
        progressBarRequests = findViewById(R.id.progressBarRequests);

        requestList = new ArrayList<>();
        filteredList = new ArrayList<>();
        recyclerRequests.setLayoutManager(new LinearLayoutManager(this));

        requestAdapter = new RequestAdapter(filteredList, this::onApproveClicked, this::onDenyClicked);
        recyclerRequests.setAdapter(requestAdapter);

        // Setup search
        if (searchView != null) {
            searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    filterRequests(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    filterRequests(newText);
                    return true;
                }
            });
        }

        // Setup filter chips
        if (chipAll != null) {
            chipAll.setOnCheckedChangeListener((button, isChecked) -> {
                if (isChecked) {
                    currentFilter = "all";
                    if (chipAttendance != null) chipAttendance.setChecked(false);
                    if (chipReturn != null) chipReturn.setChecked(false);
                    if (searchView != null) filterRequests(searchView.getQuery().toString());
                    else filterRequests("");
                }
            });
        }

        if (chipAttendance != null) {
            chipAttendance.setOnCheckedChangeListener((button, isChecked) -> {
                if (isChecked) {
                    currentFilter = "attendance";
                    if (chipAll != null) chipAll.setChecked(false);
                    if (chipReturn != null) chipReturn.setChecked(false);
                    if (searchView != null) filterRequests(searchView.getQuery().toString());
                    else filterRequests("");
                }
            });
        }

        if (chipReturn != null) {
            chipReturn.setOnCheckedChangeListener((button, isChecked) -> {
                if (isChecked) {
                    currentFilter = "return";
                    if (chipAll != null) chipAll.setChecked(false);
                    if (chipAttendance != null) chipAttendance.setChecked(false);
                    if (searchView != null) filterRequests(searchView.getQuery().toString());
                    else filterRequests("");
                }
            });
        }

        tvStudentsPresentCount = findViewById(R.id.tvStudentsPresentCount);
        tvBorrowedEquipmentCount = findViewById(R.id.tvBorrowedEquipmentCount);

        swipeRefresh.setOnRefreshListener(this::loadRequests);

        etSearchAchievement = findViewById(R.id.etSearchAchievement);
        btnSearchAchievement = findViewById(R.id.btnSearchAchievement);
        llAchievements = findViewById(R.id.llAchievements);
        btnExportExcel = findViewById(R.id.btnExportExcel);
        btnGenerateQR = findViewById(R.id.btnGenerateQR);
        btnManageApprovals = findViewById(R.id.btnManageApprovals);
        Button btnManageSports = findViewById(R.id.btnManageSports);

        btnSearchAchievement.setOnClickListener(v -> searchAchievements());
        btnExportExcel.setOnClickListener(v -> exportAttendanceToExcel(attendanceList, "attendance_report"));
        btnGenerateQR.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRGeneratorActivity.class);
            startActivity(intent);
        });
        btnManageApprovals.setOnClickListener(v -> {
            Intent intent = new Intent(this, PTActivity.class);
            startActivity(intent);
        });
        btnManageSports.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageSportsActivity.class);
            startActivity(intent);
        });
        
        findViewById(R.id.btnResetPassword).setOnClickListener(v -> showGenerateResetCodeDialog());

        loadRequests();

        // Setup Bottom Navigation
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_scan) {
                Intent intent = new Intent(this, QRGeneratorActivity.class);
                startActivity(intent);
                return false;
            } else if (itemId == R.id.nav_sports) {
                Intent intent = new Intent(this, ManageSportsActivity.class);
                startActivity(intent);
                return false;
            } else if (itemId == R.id.nav_requests) {
                Intent intent = new Intent(this, PTActivity.class);
                startActivity(intent);
                return false;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_home);
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
        } else if (item.getItemId() == R.id.action_recover_data) {
            showRecoverDataDialog();
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

    private void showRecoverDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Recover Data")
                .setMessage("Are you sure you want to recover attendance data from the backup file? This will overwrite the current local data.")
                .setPositiveButton("Recover", (dialog, which) -> {
                    boolean success = backupManager.restoreFromRecovery();
                    if (success) {
                        Toast.makeText(this, "Data recovered successfully!", Toast.LENGTH_SHORT).show();
                        loadStudentsPresentCount(); // Refresh count from local
                    } else {
                        Toast.makeText(this, "No backup file found to recover.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (borrowedItemsListener != null) {
            borrowedItemsListener.remove();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh stats and requests when returning to dashboard
        loadRequests();
    }

    private void filterRequests(String query) {
        filteredList.clear();
        
        for (RequestModel request : requestList) {
            boolean matchesSearch = false;
            boolean matchesType = false;
            
            // Check search query
            if (query == null || query.isEmpty()) {
                matchesSearch = true;
            } else {
                String lowerQuery = query.toLowerCase();
                String uucms = request.getUucms() != null ? request.getUucms().toLowerCase() : "";
                String name = request.getStudentName() != null ? request.getStudentName().toLowerCase() : "";
                
                matchesSearch = uucms.contains(lowerQuery) || name.contains(lowerQuery);
            }
            
            // Check type filter
            if ("all".equals(currentFilter)) {
                matchesType = true;
            } else if ("attendance".equals(currentFilter)) {
                matchesType = request.getType() == null || "attendance".equals(request.getType());
            } else if ("return".equals(currentFilter)) {
                matchesType = "return".equals(request.getType());
            }
            
            // Add if matches both criteria
            if (matchesSearch && matchesType) {
                filteredList.add(request);
            }
        }
        
        requestAdapter.notifyDataSetChanged();
    }

    private void loadRequests() {
        if (progressBarRequests != null) {
            progressBarRequests.setVisibility(View.VISIBLE);
        }
        recyclerRequests.setVisibility(View.GONE);
        
        requestList.clear();
        
        // First, fetch all user IDs we'll need
        java.util.Set<String> userIds = new java.util.HashSet<>();
        
        // Load attendance requests
        db.collection("attendanceRequests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Collect user IDs
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String userId = doc.getString("userId");
                        if (userId != null) {
                            userIds.add(userId);
                        }
                    }
                    
                    // Load return requests and collect more user IDs
                    db.collection("borrowRequests")
                        .whereEqualTo("status", "return_pending")
                        .get()
                        .addOnSuccessListener(returnQuerySnapshot -> {
                            // Collect user IDs from return requests
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : returnQuerySnapshot) {
                                String userId = doc.getString("userId");
                                if (userId != null) {
                                    userIds.add(userId);
                                }
                            }
                            
                            // Now batch fetch all student names
                            if (userIds.isEmpty()) {
                                // No requests, just finish loading
                                processRequests(querySnapshot, returnQuerySnapshot, new java.util.HashMap<>());
                            } else {
                                fetchStudentNames(userIds, studentNames -> {
                                    processRequests(querySnapshot, returnQuerySnapshot, studentNames);
                                });
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (progressBarRequests != null) {
                                progressBarRequests.setVisibility(View.GONE);
                            }
                            swipeRefresh.setRefreshing(false);
                            
                            // Log the actual error
                            android.util.Log.e("PtDashboard", "Failed to load return requests", e);
                            
                            // Show detailed error message
                            String errorMsg = "Failed to load return requests";
                            if (e.getMessage() != null) {
                                if (e.getMessage().contains("index")) {
                                    errorMsg = "Database index required. Check Logcat for link to create it.";
                                } else if (e.getMessage().contains("permission") || e.getMessage().contains("PERMISSION_DENIED")) {
                                    errorMsg = "Permission denied. Check Firestore security rules.";
                                } else {
                                    errorMsg = "Error: " + e.getMessage();
                                }
                            }
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        });
                })
                .addOnFailureListener(e -> {
                    if (progressBarRequests != null) {
                        progressBarRequests.setVisibility(View.GONE);
                    }
                    swipeRefresh.setRefreshing(false);
                    
                    // Log the actual error
                    android.util.Log.e("PtDashboard", "Failed to load attendance requests", e);
                    
                    // Show detailed error message
                    String errorMsg = "Failed to load attendance requests";
                    if (e.getMessage() != null) {
                        if (e.getMessage().contains("index")) {
                            errorMsg = "Database index required. Check Logcat for link to create it.";
                        } else if (e.getMessage().contains("permission") || e.getMessage().contains("PERMISSION_DENIED")) {
                            errorMsg = "Permission denied. Check Firestore security rules.";
                        } else {
                            errorMsg = "Error: " + e.getMessage();
                        }
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                });
    }
    
    /**
     * Batch fetch student names to avoid N+1 query problem
     */
    private void fetchStudentNames(java.util.Set<String> userIds, java.util.function.Consumer<java.util.Map<String, String>> callback) {
        java.util.Map<String, String> studentNames = new java.util.HashMap<>();
        java.util.concurrent.atomic.AtomicInteger remaining = new java.util.concurrent.atomic.AtomicInteger(userIds.size());
        
        for (String userId : userIds) {
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (userDoc.exists()) {
                            String name = userDoc.getString("name");
                            if (name != null) {
                                studentNames.put(userId, name);
                            }
                        }
                        
                        // Check if all names are fetched
                        if (remaining.decrementAndGet() == 0) {
                            callback.accept(studentNames);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Still decrement counter even on failure
                        if (remaining.decrementAndGet() == 0) {
                            callback.accept(studentNames);
                        }
                    });
        }
    }
    
    /**
     * Process requests after all student names are fetched
     */
    private void processRequests(
            com.google.firebase.firestore.QuerySnapshot attendanceSnapshot,
            com.google.firebase.firestore.QuerySnapshot returnSnapshot,
            java.util.Map<String, String> studentNames) {
        
        requestList.clear();
        
        // Process attendance requests with timestamp for sorting
        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : attendanceSnapshot) {
            RequestModel model = new RequestModel();
            model.setRequestId(doc.getId());
            model.setUserId(doc.getString("userId"));
            model.setUucms(doc.getString("uucms"));
            model.setSport(doc.getString("sport"));
            model.setStatus(doc.getString("status"));
            model.setType("attendance");
            model.setRequestedAt(doc.getTimestamp("requestedAt"));
            
            // Set student name from cached map
            String userId = doc.getString("userId");
            if (userId != null && studentNames.containsKey(userId)) {
                model.setStudentName(studentNames.get(userId));
            }
            
            requestList.add(model);
        }
        
        // Process return requests
        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : returnSnapshot) {
            BorrowRequest borrowReq = doc.toObject(BorrowRequest.class);
            
            RequestModel model = new RequestModel();
            model.setRequestId(doc.getId());
            model.setUserId(borrowReq.getUserId());
            model.setUucms(borrowReq.getUucms());
            model.setSport(borrowReq.getSport());
            model.setStatus("return_pending");
            model.setType("return");
            model.setSportId(doc.getString("sportId"));
            model.setEquipmentList(borrowReq.getEquipment());
            model.setRequestedAt(borrowReq.getBorrowedAt());
            
            // Set student name from cached map
            String userId = borrowReq.getUserId();
            if (userId != null && studentNames.containsKey(userId)) {
                model.setStudentName(studentNames.get(userId));
            }
            
            requestList.add(model);
        }
        
        // Sort combined list by timestamp (newest first) in memory
        requestList.sort((r1, r2) -> {
            Timestamp t1 = r1.getRequestedAt();
            Timestamp t2 = r2.getRequestedAt();
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            // Descending order (newest first)
            return t2.compareTo(t1);
        });
        
        // Apply current filter - only notify adapter ONCE
        if (searchView != null) {
            filterRequests(searchView.getQuery().toString());
        } else {
            filterRequests("");
        }
        swipeRefresh.setRefreshing(false);
        if (progressBarRequests != null) {
            progressBarRequests.setVisibility(View.GONE);
        }
        
        if (requestList.isEmpty()) {
            recyclerRequests.setVisibility(View.GONE);
        } else {
            recyclerRequests.setVisibility(View.VISIBLE);
        }
        
        // Load stats
        loadStudentsPresentCount();
        setupRealtimeStats();
    }



    private void loadStudentsPresentCount() {
        // Use local backup for count to reduce Firebase reads
        List<AttendanceRecord> records = backupManager.getAttendanceHistory();
        
        // Filter for today's records
        int count = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = sdf.format(new Date());
        
        for (AttendanceRecord record : records) {
            // Assuming timestamp in record is stored as full date string, check if it contains today's date
            // Or if we stored it as just date. Let's check how we save it.
            // We will save it as full timestamp string.
            if (record.getTimestamp() != null && record.getTimestamp().startsWith(today)) {
                count++;
            }
        }
        tvStudentsPresentCount.setText(String.valueOf(count));
    }

    private com.google.firebase.firestore.ListenerRegistration borrowedItemsListener;

    private void setupRealtimeStats() {
        // Real-time listener for borrowed items count
        // Include both "borrowed" and "return_pending" because items are still borrowed until PT approves return
        borrowedItemsListener = db.collection("borrowRequests")
                .whereIn("status", java.util.Arrays.asList("borrowed", "return_pending"))
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        android.util.Log.e("PtDashboard", "Listen failed.", e);
                        return;
                    }

                    if (querySnapshot != null) {
                        int totalItems = 0;
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                            BorrowRequest req = doc.toObject(BorrowRequest.class);
                            if (req.getEquipment() != null) {
                                for (BorrowRequest.BorrowedEquipment eq : req.getEquipment()) {
                                    totalItems += eq.getQuantity();
                                }
                            }
                        }
                        tvBorrowedEquipmentCount.setText(String.valueOf(totalItems));
                    }
                });
    }

    private void onApproveClicked(RequestModel request) {
        if ("return".equals(request.getType())) {
            // Handle return approval
            approveReturn(request);
        } else {
            // Handle attendance approval
            db.collection("attendanceRequests").document(request.getRequestId())
                    .update("status", "approved")
                    .addOnSuccessListener(aVoid -> {
                        com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                                R.string.approved, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                        
                        // Send notification to student
                        if (request.getUserId() != null && request.getSport() != null) {
                            NotificationHelper.sendAttendanceApprovedNotification(
                                    request.getUserId(), 
                                    request.getSport()
                            );
                        }
                        
                        // Save to local backup
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        String timestamp = sdf.format(new Date());
                        AttendanceRecord record = new AttendanceRecord(
                            request.getUucms(),
                            request.getStudentName(),
                            request.getSport(),
                            "approved",
                            timestamp
                        );
                        backupManager.saveAttendance(record);
                        
                        loadRequests();
                        loadStudentsPresentCount(); // Refresh present count
                    });
        }
    }

    private void approveReturn(RequestModel request) {
        // Validate request data
        if (request.getEquipmentList() == null || request.getEquipmentList().isEmpty()) {
            Toast.makeText(this, R.string.error_no_equipment_data, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (request.getSportId() == null) {
            Toast.makeText(this, R.string.error_sport_missing, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Use Firestore Transaction to ensure atomicity
        // Both status update and equipment restoration will succeed or fail together
        db.runTransaction((com.google.firebase.firestore.Transaction transaction) -> {
            // Reference to the borrow request document
            com.google.firebase.firestore.DocumentReference borrowRequestRef = 
                    db.collection("borrowRequests").document(request.getRequestId());
            
            // Reference to the sport document
            com.google.firebase.firestore.DocumentReference sportRef = 
                    db.collection("sports").document(request.getSportId());
            
            // Read the current sport document (required in transaction)
            com.google.firebase.firestore.DocumentSnapshot sportSnapshot = transaction.get(sportRef);
            
            if (!sportSnapshot.exists()) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "Sport document not found",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }
            
            // Step 1: Update borrow request status to "returned"
            transaction.update(borrowRequestRef, 
                    "status", "returned", 
                    "returnedAt", com.google.firebase.Timestamp.now());
            
            // Step 2: Restore equipment quantities atomically
            Map<String, Object> updates = new HashMap<>();
            for (BorrowRequest.BorrowedEquipment eq : request.getEquipmentList()) {
                String key = eq.getName().toLowerCase().replace(" ", "_");
                // Use FieldValue.increment to atomically add returned quantity
                updates.put("equipment." + key + ".availableQuantity", 
                           com.google.firebase.firestore.FieldValue.increment(eq.getQuantity()));
            }
            
            transaction.update(sportRef, updates);
            
            // Transaction will commit both updates atomically
            return null;
        }).addOnSuccessListener(aVoid -> {
            // Both operations succeeded
            android.util.Log.d("PtDashboard", "Return approved and equipment restored atomically");
            
            // Send notification to student
            if (request.getUserId() != null && request.getSport() != null) {
                NotificationHelper.sendEquipmentReturnApprovedNotification(
                        request.getUserId(), 
                        request.getSport()
                );
            }
            
            // Build success message
            StringBuilder message = new StringBuilder("Return approved. Restored: ");
            for (BorrowRequest.BorrowedEquipment eq : request.getEquipmentList()) {
                message.append(eq.getQuantity()).append(" ").append(eq.getName()).append(", ");
            }
            Toast.makeText(this, message.toString(), Toast.LENGTH_SHORT).show();
            
            loadRequests();
        }).addOnFailureListener(e -> {
            // Transaction failed - neither operation was applied
            android.util.Log.e("PtDashboard", "Transaction failed - no changes applied", e);
            Toast.makeText(this, "Error: Failed to approve return. No changes made. " + e.getMessage(), 
                    Toast.LENGTH_LONG).show();
        });
    }
    
    /**
     * @deprecated This method is no longer used. Equipment restoration is now handled
     * atomically within the approveReturn transaction.
     */
    @Deprecated
    private void restoreEquipmentQuantities(RequestModel request) {
        // This method is kept for reference but is no longer called
        // Equipment restoration is now part of the atomic transaction in approveReturn()
        
        // Build a map of atomic increment operations
        Map<String, Object> updates = new HashMap<>();
        
        for (BorrowRequest.BorrowedEquipment eq : request.getEquipmentList()) {
            String key = eq.getName().toLowerCase().replace(" ", "_");
            // Use FieldValue.increment to atomically add returned quantity
            updates.put("equipment." + key + ".availableQuantity", 
                       com.google.firebase.firestore.FieldValue.increment(eq.getQuantity()));
        }
        
        // Atomically update all equipment quantities in a single operation
        db.collection("sports").document(request.getSportId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("PtDashboard", "Successfully restored equipment quantities");
                    // Build success message
                    StringBuilder message = new StringBuilder("Restored: ");
                    for (BorrowRequest.BorrowedEquipment eq : request.getEquipmentList()) {
                        message.append(eq.getQuantity()).append(" ").append(eq.getName()).append(", ");
                    }
                    Toast.makeText(this, message.toString(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("PtDashboard", "Failed to restore equipment quantities", e);
                    com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                            R.string.error_equipment_restore, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
                });
    }

    private void onDenyClicked(RequestModel request) {
        String collection = "return".equals(request.getType()) ? "borrowRequests" : "attendanceRequests";
        
        db.collection(collection).document(request.getRequestId()) // Changed from request.getId() to request.getRequestId()
                .update("status", "denied")
                .addOnSuccessListener(aVoid -> {
                    com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                            R.string.denied, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                    
                    // Send notification to student
                    if ("attendance".equals(request.getType()) && request.getUserId() != null && request.getSport() != null) {
                        NotificationHelper.sendAttendanceDeniedNotification(
                                request.getUserId(), 
                                request.getSport()
                        );
                    }
                    
                    loadRequests();
                });
    }

    private void searchAchievements() {
        String queryText = etSearchAchievement.getText().toString().trim();
        if (queryText.isEmpty()) {
            Toast.makeText(this, R.string.search_hint, Toast.LENGTH_SHORT).show();
            return;
        }

        llAchievements.removeAllViews();

        // Try searching by UUCMS first (convert to uppercase)
        db.collection("achievements")
                .whereEqualTo("uucms", queryText.toUpperCase())
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        for (DocumentSnapshot doc : snap) {
                            addAchievementView(doc);
                        }
                    } else {
                        db.collection("achievements")
                                .whereEqualTo("studentName", queryText)
                                .get()
                                .addOnSuccessListener(nameSnap -> {
                                    if (!nameSnap.isEmpty()) {
                                        for (DocumentSnapshot doc : nameSnap) {
                                            addAchievementView(doc);
                                        }
                                    } else {
                                        Toast.makeText(this, R.string.no_achievements_found, Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void exportAttendanceToExcel(List<Map<String, Object>> attendanceList, String filename) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendance");

        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("UUCMS ID");
        header.createCell(1).setCellValue("Sport");
        header.createCell(2).setCellValue("Status");
        header.createCell(3).setCellValue("Requested At");
        header.createCell(4).setCellValue("Exit Time");

        int rowIdx = 1;
        for (Map<String, Object> row : attendanceList) {
            Row excelRow = sheet.createRow(rowIdx++);
            excelRow.createCell(0).setCellValue(row.get("uucms") != null ? row.get("uucms").toString() : "");
            excelRow.createCell(1).setCellValue(row.get("sport") != null ? row.get("sport").toString() : "");
            excelRow.createCell(2).setCellValue(row.get("status") != null ? row.get("status").toString() : "");
            excelRow.createCell(3).setCellValue(row.get("requestedAt") != null ? row.get("requestedAt").toString() : "");
            excelRow.createCell(4).setCellValue(row.get("exitTime") != null ? row.get("exitTime").toString() : "");
        }

        try {
            File dir = new File(getExternalFilesDir(null), "exports");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, filename + ".xlsx");
            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.close();
            workbook.close();

            Toast.makeText(this, "Excel exported: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

            Intent intentShare = new Intent(Intent.ACTION_SEND);
            intentShare.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            intentShare.putExtra(Intent.EXTRA_STREAM, uri);
            intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intentShare, "Share Excel"));
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



    private void addAchievementView(DocumentSnapshot doc) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        TextView tv = new TextView(this);
        String info = "🏆 " + doc.getString("title") +
                "\n👤 " + doc.getString("studentName") +
                " (" + doc.getString("uucms") + ")";
        tv.setText(info);

        Button btnDelete = new Button(this);
        btnDelete.setText("Delete");

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete this achievement?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        db.collection("achievements").document(doc.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, R.string.achievement_deleted, Toast.LENGTH_SHORT).show();
                                    llAchievements.removeView(layout);
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                );
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        layout.addView(tv);
        layout.addView(btnDelete);
        llAchievements.addView(layout);
    }

    private void showGenerateResetCodeDialog() {
        // Let's use a simple EditText in AlertDialog for simplicity
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Enter Student UUCMS");
        input.setInputType(android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        
        // Add padding
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new android.app.AlertDialog.Builder(this)
                .setTitle("Generate Reset Code")
                .setMessage("Enter the UUCMS of the student who forgot their password.")
                .setView(input)
                .setPositiveButton("Generate", (dialog, which) -> {
                    String uucms = input.getText().toString().trim().toUpperCase();
                    if (!uucms.isEmpty()) {
                        generateAndSaveResetCode(uucms);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void generateAndSaveResetCode(String uucms) {
        // 1. Find user
        db.collection("users")
                .whereEqualTo("uucms", uucms)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                                "User not found with UUCMS: " + uucms, com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    String docId = querySnapshot.getDocuments().get(0).getId();
                    
                    // 2. Generate Code
                    String code = String.format("%06d", new java.util.Random().nextInt(999999));
                    
                    // 3. Save to Firestore
                    db.collection("users").document(docId)
                            .update("resetCode", code)
                            .addOnSuccessListener(aVoid -> {
                                // 4. Show to Admin
                                new android.app.AlertDialog.Builder(this)
                                        .setTitle("Reset Code Generated")
                                        .setMessage("Share this code with the student:\n\n" + code + "\n\nThey can use this code in the 'Forgot Password' screen to reset their account.")
                                        .setPositiveButton("Done", null)
                                        .show();
                            })
                            .addOnFailureListener(e -> 
                                com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                                        "Failed to save code: " + e.getMessage(), com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show());
                })
                .addOnFailureListener(e -> 
                    com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), 
                            "Error finding user: " + e.getMessage(), com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show());
    }
}