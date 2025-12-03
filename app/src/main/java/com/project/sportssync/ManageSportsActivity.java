package com.project.sportssync;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class ManageSportsActivity extends AppCompatActivity {

    private LinearLayout llSportsList;
    private Button btnAddSport;
    private BackupManager backupManager;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_sports);

        llSportsList = findViewById(R.id.llSportsList);
        btnAddSport = findViewById(R.id.btnAddSport);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        db = FirebaseFirestore.getInstance();
        backupManager = new BackupManager(this);

        btnAddSport.setOnClickListener(v -> showAddSportDialog());
        
        // Try loading from cache first
        loadSportsFromCache();
        // Then fetch fresh data
        loadSportsFromFirebase();
    }

    private void loadSportsFromCache() {
        List<SportModel> cachedSports = backupManager.getSportsCache();
        if (cachedSports != null && !cachedSports.isEmpty()) {
            llSportsList.removeAllViews();
            tvEmptyState.setVisibility(View.GONE);
            for (SportModel sport : cachedSports) {
                addSportCard(sport);
            }
        }
    }

    private void loadSportsFromFirebase() {
        progressBar.setVisibility(View.VISIBLE);
        llSportsList.removeAllViews();
        tvEmptyState.setVisibility(View.GONE);
        
        db.collection("sports")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    
                    if (querySnapshot.isEmpty()) {
                        if (llSportsList.getChildCount() == 0) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                        }
                    } else {
                        List<SportModel> sportsList = new java.util.ArrayList<>();
                        llSportsList.removeAllViews(); // Clear cache view to avoid duplicates if we just append
                        
                        querySnapshot.forEach(doc -> {
                            SportModel sport = doc.toObject(SportModel.class);
                            sport.setId(doc.getId());
                            sportsList.add(sport);
                            addSportCard(sport);
                        });
                        
                        // Update cache
                        backupManager.saveSportsCache(sportsList);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    if (llSportsList.getChildCount() == 0) {
                        tvEmptyState.setVisibility(View.VISIBLE);
                        tvEmptyState.setText("Error loading sports");
                    }
                    Toast.makeText(this, "Error loading sports: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addSportCard(SportModel sport) {
        View card = LayoutInflater.from(this).inflate(R.layout.item_sport_card, llSportsList, false);
        
        TextView tvSportName = card.findViewById(R.id.tvSportName);
        TextView tvEquipmentCount = card.findViewById(R.id.tvEquipmentCount);
        Button btnAddEquipment = card.findViewById(R.id.btnAddEquipment);
        Button btnViewEquipment = card.findViewById(R.id.btnViewEquipment);
        Button btnDeleteSport = card.findViewById(R.id.btnDeleteSport);

        tvSportName.setText(sport.getName());
        int equipmentCount = sport.getEquipment() != null ? sport.getEquipment().size() : 0;
        tvEquipmentCount.setText(equipmentCount + " equipment items");

        btnAddEquipment.setOnClickListener(v -> showAddEquipmentDialog(sport));
        btnViewEquipment.setOnClickListener(v -> showEquipmentList(sport));
        btnDeleteSport.setOnClickListener(v -> confirmDeleteSport(sport));

        llSportsList.addView(card);
    }

    private void showAddSportDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Sport");

        final EditText input = new EditText(this);
        input.setHint("Sport Name (e.g., Basketball)");
        input.setPadding(50, 30, 50, 30);
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String sportName = input.getText().toString().trim();
            if (!sportName.isEmpty()) {
                addSport(sportName);
            } else {
                Toast.makeText(this, "Please enter sport name", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addSport(String sportName) {
        Map<String, Object> sport = new HashMap<>();
        sport.put("name", sportName);
        sport.put("equipment", new HashMap<String, Object>());

        db.collection("sports")
                .add(sport)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "Sport added successfully", Toast.LENGTH_SHORT).show();
                    loadSportsFromFirebase();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error adding sport", Toast.LENGTH_SHORT).show()
                );
    }

    private void showAddEquipmentDialog(SportModel sport) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Equipment to " + sport.getName());

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_equipment, null);
        EditText etEquipmentName = dialogView.findViewById(R.id.etEquipmentName);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);

        builder.setView(dialogView);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String equipmentName = etEquipmentName.getText().toString().trim();
            String quantityStr = etQuantity.getText().toString().trim();

            if (!equipmentName.isEmpty() && !quantityStr.isEmpty()) {
                int quantity = Integer.parseInt(quantityStr);
                addEquipment(sport, equipmentName, quantity);
            } else {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addEquipment(SportModel sport, String equipmentName, int quantity) {
        String key = equipmentName.toLowerCase().replace(" ", "_");
        
        Map<String, Object> equipmentItem = new HashMap<>();
        equipmentItem.put("name", equipmentName);
        equipmentItem.put("totalQuantity", quantity);
        equipmentItem.put("availableQuantity", quantity);

        db.collection("sports").document(sport.getId())
                .update("equipment." + key, equipmentItem)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Equipment added", Toast.LENGTH_SHORT).show();
                    loadSportsFromFirebase();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error adding equipment", Toast.LENGTH_SHORT).show()
                );
    }

    private void showEquipmentList(SportModel sport) {
        if (sport.getEquipment() == null || sport.getEquipment().isEmpty()) {
            Toast.makeText(this, "No equipment added yet", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(sport.getName() + " Equipment");

        StringBuilder message = new StringBuilder();
        for (Map.Entry<String, SportModel.EquipmentItem> entry : sport.getEquipment().entrySet()) {
            SportModel.EquipmentItem item = entry.getValue();
            message.append("• ").append(item.getName())
                    .append("\n  Total: ").append(item.getTotalQuantity())
                    .append(" | Available: ").append(item.getAvailableQuantity())
                    .append("\n\n");
        }

        builder.setMessage(message.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void confirmDeleteSport(SportModel sport) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Sport")
                .setMessage("Delete " + sport.getName() + " and all its equipment?")
                .setPositiveButton("Delete", (dialog, which) -> deleteSport(sport))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSport(SportModel sport) {
        // First, check if there are any active borrow requests for this sport
        db.collection("borrowRequests")
                .whereEqualTo("sportId", sport.getId())
                .whereEqualTo("status", "borrowed")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Active loans exist - prevent deletion
                        int activeLoans = querySnapshot.size();
                        new AlertDialog.Builder(this)
                                .setTitle("Cannot Delete Sport")
                                .setMessage("This sport has " + activeLoans + " active equipment loan(s). " +
                                           "Please wait for all equipment to be returned before deleting this sport.")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        // No active loans - safe to delete
                        db.collection("sports").document(sport.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Sport deleted", Toast.LENGTH_SHORT).show();
                                    loadSportsFromFirebase();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error deleting sport", Toast.LENGTH_SHORT).show()
                                );
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking active loans. Please try again.", Toast.LENGTH_LONG).show();
                });
    }
}
