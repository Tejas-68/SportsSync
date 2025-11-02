package com.sportssync.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.sportssync.app.R;
import com.sportssync.app.adapters.SportManageAdapter;
import com.sportssync.app.activities.models.Equipment;
import com.sportssync.app.activities.models.Sport;
import com.sportssync.app.activities.utils.FirebaseManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.sportssync.app.activities.utils.LoadingDialog;

public class ManageSportsActivity extends AppCompatActivity {

    private RecyclerView rvSports;
    private MaterialButton btnAddSport;
    private SportManageAdapter adapter;
    private List<Sport> sportsList;
    private FirebaseManager firebaseManager;
    private LoadingDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_sports);

        firebaseManager = FirebaseManager.getInstance();
        sportsList = new ArrayList<>();
        loadingDialog = new LoadingDialog(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        loadSports();
    }

    private void initViews() {
        rvSports = findViewById(R.id.rvSports);
        btnAddSport = findViewById(R.id.btnAddSport);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new SportManageAdapter(sportsList, new SportManageAdapter.OnSportActionListener() {
            @Override
            public void onToggleActive(Sport sport, boolean isActive) {
                updateSportStatus(sport, isActive);
            }

            @Override
            public void onAddEquipment(Sport sport) {
                showAddEquipmentDialog(sport);
            }
        });
        rvSports.setLayoutManager(new LinearLayoutManager(this));
        rvSports.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnAddSport.setOnClickListener(v -> showAddSportDialog());
    }

    private void loadSports() {
        firebaseManager.getDb().collection("sports")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    sportsList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Sport sport = document.toObject(Sport.class);
                        sportsList.add(sport);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showAddSportDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_sport, null);
        TextInputEditText etSportName = dialogView.findViewById(R.id.etSportName);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String sportName = etSportName.getText().toString().trim();
            if (sportName.isEmpty()) {
                Toast.makeText(this, "Please enter sport name", Toast.LENGTH_SHORT).show();
                return;
            }

            loadingDialog.show("Adding sport...");

            String sportId = firebaseManager.getDb().collection("sports").document().getId();
            Sport sport = new Sport(sportId, sportName);

            Map<String, Object> sportData = new HashMap<>();
            sportData.put("sportId", sport.getSportId());
            sportData.put("sportName", sport.getSportName());
            sportData.put("equipmentList", new ArrayList<>());
            sportData.put("isActive", sport.isActive());
            sportData.put("createdAt", sport.getCreatedAt());

            firebaseManager.getDb().collection("sports").document(sportId)
                    .set(sportData)
                    .addOnSuccessListener(aVoid -> {
                        loadingDialog.dismiss();
                        Toast.makeText(this, "Sport added successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadSports();
                    })
                    .addOnFailureListener(e -> {
                        loadingDialog.dismiss();
                        Toast.makeText(this, "Failed to add sport", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void showAddEquipmentDialog(Sport sport) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_equipment, null);
        TextInputEditText etEquipmentName = dialogView.findViewById(R.id.etEquipmentName);
        TextInputEditText etQuantity = dialogView.findViewById(R.id.etQuantity);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .create();

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String equipmentName = etEquipmentName.getText().toString().trim();
            String quantityStr = etQuantity.getText().toString().trim();

            if (equipmentName.isEmpty() || quantityStr.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            loadingDialog.show("Adding equipment...");

            int quantity = Integer.parseInt(quantityStr);
            String equipmentId = "eq_" + System.currentTimeMillis();
            Equipment equipment = new Equipment(equipmentId, equipmentName, quantity);

            sport.getEquipmentList().add(equipment);

            Map<String, Object> sportData = new HashMap<>();
            sportData.put("sportId", sport.getSportId());
            sportData.put("sportName", sport.getSportName());
            sportData.put("equipmentList", sport.getEquipmentList());
            sportData.put("isActive", sport.isActive());
            sportData.put("createdAt", sport.getCreatedAt());

            firebaseManager.getDb().collection("sports").document(sport.getSportId())
                    .set(sportData)
                    .addOnSuccessListener(aVoid -> {
                        loadingDialog.dismiss();
                        Toast.makeText(this, "Equipment added successfully", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadSports();
                    })
                    .addOnFailureListener(e -> {
                        loadingDialog.dismiss();
                        Toast.makeText(this, "Failed to add equipment", Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    private void updateSportStatus(Sport sport, boolean isActive) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isActive", isActive);

        firebaseManager.getDb().collection("sports").document(sport.getSportId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    sport.setActive(isActive);
                    Toast.makeText(this, "Sport status updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                });
    }
}