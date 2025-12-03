package com.project.sportssync;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EquipmentSelectionActivity extends AppCompatActivity {

    private TextView tvSportName, tvNoEquipment;
    private RecyclerView recyclerEquipment;
    private Button btnBorrowSelected;
    private LinearLayout llEquipmentList;
    private ProgressBar progressBarEquipment; // Added ProgressBar field
    
    private String sportId, sportName, userId, uucms, studentName;
    private FirebaseFirestore db;
    private List<EquipmentItemView> equipmentItems;
    private EquipmentSelectAdapter equipmentAdapter; // Keep original adapter type for now, as the snippet only changed the name, not the type.
    private Map<String, Integer> selectedQuantities; // Added selectedQuantities field

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment_selection);

        tvSportName = findViewById(R.id.tvSportName);
        tvNoEquipment = findViewById(R.id.tvNoEquipment);
        recyclerEquipment = findViewById(R.id.recyclerEquipment);
        btnBorrowSelected = findViewById(R.id.btnBorrowSelected);
        progressBarEquipment = findViewById(R.id.progressBarEquipment); // Initialized ProgressBar

        sportId = getIntent().getStringExtra("sportId");
        sportName = getIntent().getStringExtra("sportName");
        userId = getIntent().getStringExtra("userId");
        uucms = getIntent().getStringExtra("uucms");
        studentName = getIntent().getStringExtra("studentName"); // For PT dashboard optimization

        db = FirebaseFirestore.getInstance();
        equipmentItems = new ArrayList<>();

        tvSportName.setText(sportName + " Equipment");
        
        recyclerEquipment.setLayoutManager(new LinearLayoutManager(this));
        equipmentAdapter = new EquipmentSelectAdapter(equipmentItems);
        recyclerEquipment.setAdapter(equipmentAdapter);
        
        // Disable button initially
        btnBorrowSelected.setEnabled(false);
        
        // Listen for quantity changes
        equipmentAdapter.setQuantityChangeListener(this::updateBorrowButton);

        btnBorrowSelected.setOnClickListener(v -> createBorrowRequest());

        loadEquipment();
    }
    
    private void updateBorrowButton() {
        boolean hasSelection = false;
        for (EquipmentItemView item : equipmentItems) {
            if (item.getSelectedQuantity() > 0) {
                hasSelection = true;
                break;
            }
        }
        btnBorrowSelected.setEnabled(hasSelection);
    }

    private void loadEquipment() {
        // 1. Load from local cache immediately
        BackupManager backupManager = new BackupManager(this);
        List<SportModel> cachedSports = backupManager.getSportsCache();
        
        if (cachedSports != null) {
            for (SportModel sport : cachedSports) {
                if (sport.getId() != null && sport.getId().equals(sportId)) {
                    equipmentItems.clear();
                    Map<String, SportModel.EquipmentItem> equipmentMap = sport.getEquipment();
                    if (equipmentMap != null) {
                        for (Map.Entry<String, SportModel.EquipmentItem> entry : equipmentMap.entrySet()) {
                            SportModel.EquipmentItem item = entry.getValue();
                            equipmentItems.add(new EquipmentItemView(
                                    entry.getKey(),
                                    item.getName(),
                                    item.getTotalQuantity(),
                                    item.getAvailableQuantity()
                            ));
                        }
                    }
                    equipmentAdapter.notifyDataSetChanged();
                    
                    if (equipmentItems.isEmpty()) {
                        tvNoEquipment.setVisibility(View.VISIBLE);
                        recyclerEquipment.setVisibility(View.GONE);
                        btnBorrowSelected.setEnabled(false);
                    } else {
                        tvNoEquipment.setVisibility(View.GONE);
                        recyclerEquipment.setVisibility(View.VISIBLE);
                        btnBorrowSelected.setEnabled(true);
                    }
                    break;
                }
            }
        }

        if (progressBarEquipment != null) {
            progressBarEquipment.setVisibility(View.VISIBLE);
        }
        
        // 2. Fetch from Firebase in background
        db.collection("sports").document(sportId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> equipmentMap = (Map<String, Object>) documentSnapshot.get("equipment");
                        equipmentItems.clear();
                        
                        // Create SportModel to save to cache
                        SportModel currentSport = new SportModel();
                        currentSport.setId(sportId);
                        currentSport.setName(sportName);
                        
                        if (equipmentMap != null) {
                            for (Map.Entry<String, Object> entry : equipmentMap.entrySet()) {
                                Map<String, Object> itemData = (Map<String, Object>) entry.getValue();
                                String name = (String) itemData.get("name");
                                Long totalQty = (Long) itemData.get("totalQuantity");
                                Long availQty = (Long) itemData.get("availableQuantity");
                                
                                if (name != null && totalQty != null && availQty != null) {
                                    equipmentItems.add(new EquipmentItemView(
                                            entry.getKey(),
                                            name,
                                            totalQty.intValue(),
                                            availQty.intValue()
                                    ));
                                    
                                    // Add to SportModel for cache
                                    currentSport.addEquipment(entry.getKey(), new SportModel.EquipmentItem(
                                            name,
                                            totalQty.intValue(),
                                            availQty.intValue()
                                    ));
                                }
                            }
                        }
                        
                        equipmentAdapter.notifyDataSetChanged();
                        
                        // 3. Update local cache
                        List<SportModel> allSports = backupManager.getSportsCache();
                        if (allSports == null) {
                            allSports = new ArrayList<>();
                        }
                        
                        // Remove existing entry for this sport if present
                        for (int i = 0; i < allSports.size(); i++) {
                            if (allSports.get(i).getId() != null && allSports.get(i).getId().equals(sportId)) {
                                allSports.remove(i);
                                break;
                            }
                        }
                        // Add updated sport
                        allSports.add(currentSport);
                        backupManager.saveSportsCache(allSports);
                        
                        if (progressBarEquipment != null) {
                            progressBarEquipment.setVisibility(View.GONE);
                        }
                        if (equipmentItems.isEmpty()) {
                            tvNoEquipment.setVisibility(View.VISIBLE);
                            recyclerEquipment.setVisibility(View.GONE);
                            btnBorrowSelected.setEnabled(false);
                        } else {
                            tvNoEquipment.setVisibility(View.GONE);
                            recyclerEquipment.setVisibility(View.VISIBLE);
                            btnBorrowSelected.setEnabled(true);
                        }
                    } else {
                        if (progressBarEquipment != null) {
                            progressBarEquipment.setVisibility(View.GONE);
                        }
                        Toast.makeText(this, "Sport not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBarEquipment != null) {
                        progressBarEquipment.setVisibility(View.GONE);
                    }
                    // Only show error if we didn't load from cache
                    if (cachedSports == null) {
                        Toast.makeText(this, "Failed to load equipment", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void createBorrowRequest() {
        List<BorrowRequest.BorrowedEquipment> selectedItems = new ArrayList<>();
        
        for (EquipmentItemView item : equipmentItems) {
            if (item.getSelectedQuantity() > 0) {
                selectedItems.add(new BorrowRequest.BorrowedEquipment(
                        item.getName(),
                        item.getSelectedQuantity()
                ));
            }
        }

        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Please select at least one equipment", Toast.LENGTH_SHORT).show();
            return;
        }

        // Directly complete borrow request for today
        completeBorrowRequest(selectedItems);
    }
    
    private void completeBorrowRequest(List<BorrowRequest.BorrowedEquipment> selectedItems) {
        // Calculate deadline timestamp (End of today)
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
        calendar.set(java.util.Calendar.MINUTE, 59);
        calendar.set(java.util.Calendar.SECOND, 59);
        Timestamp borrowedUntil = new Timestamp(calendar.getTime());

        BorrowRequest request = new BorrowRequest(userId, uucms, sportName, selectedItems, "borrow");
        request.setStatus("borrowed"); // Directly borrowed
        request.setBorrowedUntil(borrowedUntil);
        
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("userId", request.getUserId());
        requestData.put("uucms", request.getUucms());
        requestData.put("sport", request.getSport());
        requestData.put("sportId", sportId); // Store sportId for returns
        requestData.put("equipment", selectedItems);
        requestData.put("status", "borrowed");
        requestData.put("borrowedAt", Timestamp.now());
        requestData.put("borrowedUntil", borrowedUntil);
        requestData.put("penaltyPoints", 0);
        requestData.put("reminderSent", false);
        requestData.put("type", "borrow");
        
        // Add student name to eliminate N+1 query in PT dashboard
        if (studentName != null && !studentName.isEmpty()) {
            requestData.put("studentName", studentName);
        }

        // Use atomic operations to prevent race conditions
        // Build a map of field updates for atomic decrement
        Map<String, Object> updates = new HashMap<>();
        for (EquipmentItemView item : equipmentItems) {
            if (item.getSelectedQuantity() > 0) {
                // Use FieldValue.increment with negative value to atomically decrement
                updates.put("equipment." + item.getKey() + ".availableQuantity", 
                           com.google.firebase.firestore.FieldValue.increment(-item.getSelectedQuantity()));
            }
        }

        // First, atomically update equipment quantities
        db.collection("sports").document(sportId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Only save borrow record if equipment update succeeded
                    db.collection("borrowRequests")
                            .add(requestData)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(this, 
                                        "Equipment borrowed! Please return by 5:30 PM today.", 
                                        Toast.LENGTH_LONG).show();
                                        
                                // Schedule return reminder
                                scheduleReturnReminder();
                                
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // Rollback: restore equipment quantities if borrow record creation fails
                                Map<String, Object> rollback = new HashMap<>();
                                for (EquipmentItemView item : equipmentItems) {
                                    if (item.getSelectedQuantity() > 0) {
                                        rollback.put("equipment." + item.getKey() + ".availableQuantity", 
                                                   com.google.firebase.firestore.FieldValue.increment(item.getSelectedQuantity()));
                                    }
                                }
                                db.collection("sports").document(sportId).update(rollback);
                                Toast.makeText(this, "Error creating borrow record. Please try again.", Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    // Equipment update failed - possibly insufficient quantity
                    Toast.makeText(this, "Unable to borrow equipment. It may have been borrowed by someone else.", 
                            Toast.LENGTH_LONG).show();
                });
    }

    private void scheduleReturnReminder() {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(android.content.Context.ALARM_SERVICE);
        android.content.Intent intent = new android.content.Intent(this, ReturnReminderReceiver.class);
        intent.putExtra("userId", userId);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                this, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        // Set time to 5:30 PM today
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 17);
        calendar.set(java.util.Calendar.MINUTE, 30);
        calendar.set(java.util.Calendar.SECOND, 0);

        // If it's already past 5:30 PM, don't schedule for today (or schedule for tomorrow if that was the logic, but here we just want today's reminder)
        if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        }
    }

    public static class EquipmentItemView {
        private String key;
        private String name;
        private int totalQuantity;
        private int availableQuantity;
        private int selectedQuantity;

        public EquipmentItemView(String key, String name, int totalQuantity, int availableQuantity) {
            this.key = key;
            this.name = name;
            this.totalQuantity = totalQuantity;
            this.availableQuantity = availableQuantity;
            this.selectedQuantity = 0;
        }

        public String getKey() { return key; }
        public String getName() { return name; }
        public int getTotalQuantity() { return totalQuantity; }
        public int getAvailableQuantity() { return availableQuantity; }
        public int getSelectedQuantity() { return selectedQuantity; }
        public void setSelectedQuantity(int quantity) { this.selectedQuantity = quantity; }
    }
}
