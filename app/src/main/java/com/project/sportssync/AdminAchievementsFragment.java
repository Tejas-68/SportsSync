package com.project.sportssync;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminAchievementsFragment extends Fragment {

    private FirebaseFirestore db;
    private ListView lvAchievements;
    private EditText etStudentName, etTitle, etDescription;
    private Spinner spinnerPosition;
    private Button btnAddAchievement;
    private List<String> achievementsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_achievements, container, false);
        
        db = FirebaseFirestore.getInstance();
        initViews(view);
        setupListeners();
        loadAchievements();
        
        return view;
    }

    private void initViews(View view) {
        lvAchievements = view.findViewById(R.id.lvAchievements);
        etStudentName = view.findViewById(R.id.etStudentName);
        etTitle = view.findViewById(R.id.etAchievementTitle);
        etDescription = view.findViewById(R.id.etAchievementDesc);
        spinnerPosition = view.findViewById(R.id.spinnerPosition);
        btnAddAchievement = view.findViewById(R.id.btnAddAchievement);
        
        String[] positions = {"1st", "2nd", "3rd"};
        ArrayAdapter<String> posAdapter = new ArrayAdapter<>(
                getContext(), 
                android.R.layout.simple_spinner_item, 
                positions
        );
        posAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPosition.setAdapter(posAdapter);
    }

    private void setupListeners() {
        btnAddAchievement.setOnClickListener(v -> addAchievement());
    }

    private void loadAchievements() {
        db.collection("achievements")
                .orderBy("addedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    achievementsList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String achievement = String.format("%s - %s (%s)\n%s",
                                doc.getString("studentName"),
                                doc.getString("title"),
                                doc.getString("position"),
                                doc.getString("description"));
                        achievementsList.add(achievement);
                    }

                    if (isAdded()) {
                        updateAchievementsList();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Failed to load achievements: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateAchievementsList() {
        if (isAdded()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_list_item_1,
                    achievementsList
            );
            lvAchievements.setAdapter(adapter);
        }
    }

    private void addAchievement() {
        String student = etStudentName.getText().toString().trim();
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        String position = spinnerPosition.getSelectedItem().toString();

        if (TextUtils.isEmpty(student) || TextUtils.isEmpty(title) || TextUtils.isEmpty(desc)) {
            Toast.makeText(getContext(), "Enter all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String achId = student + "_" + System.currentTimeMillis();
        Map<String, Object> ach = new HashMap<>();
        ach.put("studentName", student);
        ach.put("title", title);
        ach.put("description", desc);
        ach.put("position", position);
        ach.put("addedAt", Timestamp.now());

        db.collection("achievements").document(achId)
                .set(ach)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Achievement added", Toast.LENGTH_SHORT).show();
                    clearFields();
                    loadAchievements();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to add achievement: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void clearFields() {
        etStudentName.setText("");
        etTitle.setText("");
        etDescription.setText("");
        spinnerPosition.setSelection(0);
    }
}
