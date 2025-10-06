package com.project.sportssync;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class AdminActiveStudentsFragment extends Fragment {

    private FirebaseFirestore db;
    private RecyclerView recyclerActiveStudents;
    private EditText etFilterUucms, etFilterSport;
    private Spinner spinnerFilterStatus;
    private ProgressBar progressBar;
    private List<RequestModel> allStudents = new ArrayList<>();
    private List<RequestModel> filteredStudents = new ArrayList<>();
    private CurrentLoginAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_active_students, container, false);
        
        db = FirebaseFirestore.getInstance();
        initViews(view);
        setupFilters();
        loadActiveStudents();
        
        return view;
    }

    private void initViews(View view) {
        recyclerActiveStudents = view.findViewById(R.id.recyclerActiveStudents);
        etFilterUucms = view.findViewById(R.id.etFilterUucms);
        etFilterSport = view.findViewById(R.id.etFilterSport);
        spinnerFilterStatus = view.findViewById(R.id.spinnerFilterStatus);
        progressBar = view.findViewById(R.id.progressBar);
        
        recyclerActiveStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Setup status filter spinner
        String[] statusOptions = {"All", "Approved", "Exited"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                getContext(), 
                android.R.layout.simple_spinner_item, 
                statusOptions
        );
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterStatus.setAdapter(statusAdapter);
    }

    private void setupFilters() {
        etFilterUucms.setOnEditorActionListener((v, actionId, event) -> {
            applyFilters();
            return true;
        });
        
        etFilterSport.setOnEditorActionListener((v, actionId, event) -> {
            applyFilters();
            return true;
        });
        
        spinnerFilterStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void loadActiveStudents() {
        progressBar.setVisibility(View.VISIBLE);
        String today = getCurrentDate();
        
        db.collection("attendanceRequests")
                .whereEqualTo("date", today)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allStudents.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        allStudents.add(new RequestModel(
                                doc.getId(),
                                doc.getString("uucms"),
                                doc.getString("sport"),
                                doc.getString("status")
                        ));
                    }
                    progressBar.setVisibility(View.GONE);
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to load students: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void applyFilters() {
        filteredStudents.clear();
        
        String filterUucms = etFilterUucms.getText().toString().trim().toLowerCase();
        String filterSport = etFilterSport.getText().toString().trim().toLowerCase();
        String filterStatus = spinnerFilterStatus.getSelectedItem().toString();
        
        for (RequestModel student : allStudents) {
            boolean matchesUucms = TextUtils.isEmpty(filterUucms) || 
                    student.getUucms().toLowerCase().contains(filterUucms);
            boolean matchesSport = TextUtils.isEmpty(filterSport) || 
                    student.getSport().toLowerCase().contains(filterSport);
            boolean matchesStatus = "All".equals(filterStatus) || 
                    filterStatus.equalsIgnoreCase(student.getStatus());
            
            if (matchesUucms && matchesSport && matchesStatus) {
                filteredStudents.add(student);
            }
        }
        
        updateAdapter();
    }

    private void updateAdapter() {
        // Separate pending and active students
        List<RequestModel> pendingStudents = new ArrayList<>();
        List<RequestModel> activeStudents = new ArrayList<>();
        
        for (RequestModel student : filteredStudents) {
            if ("pending".equals(student.getStatus())) {
                pendingStudents.add(student);
            } else if ("approved".equals(student.getStatus())) {
                activeStudents.add(student);
            }
        }
        
        // Create a combined list with pending first, then active
        List<RequestModel> combinedList = new ArrayList<>();
        combinedList.addAll(pendingStudents);
        combinedList.addAll(activeStudents);
        
        adapter = new CurrentLoginAdapter(
                combinedList,
                student -> {
                    if ("pending".equals(student.getStatus())) {
                        db.collection("attendanceRequests").document(student.getId())
                                .update("status", "approved")
                                .addOnSuccessListener(aVoid -> {
                                    createAttendanceRecord(student);
                                    Toast.makeText(getContext(), "Request approved", Toast.LENGTH_SHORT).show();
                                    loadActiveStudents();
                                });
                    } else if ("approved".equals(student.getStatus())) {
                        // Mark as exited
                        db.collection("attendanceRequests").document(student.getId())
                                .update(new java.util.HashMap<String, Object>() {{
                                    put("status", "exited");
                                    put("exitTime", Timestamp.now());
                                }})
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Student marked as exited", Toast.LENGTH_SHORT).show();
                                    loadActiveStudents();
                                });
                    }
                }
        );
        recyclerActiveStudents.setAdapter(adapter);
    }
    
    private void createAttendanceRecord(RequestModel request) {
        db.collection("attendanceRequests").document(request.getId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userId = documentSnapshot.getString("userId");
                        String date = documentSnapshot.getString("date");
                        String sport = documentSnapshot.getString("sport");
                        
                        if (userId != null && date != null) {
                            Map<String, Object> attendance = new HashMap<>();
                            attendance.put("userId", userId);
                            attendance.put("date", date);
                            attendance.put("sport", sport);
                            attendance.put("status", "present");
                            attendance.put("timestamp", Timestamp.now());
                            
                            db.collection("attendance").document(userId + "_" + date)
                                    .set(attendance)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getContext(), "Attendance recorded", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(getContext(), "Failed to record attendance: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to get request details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}
