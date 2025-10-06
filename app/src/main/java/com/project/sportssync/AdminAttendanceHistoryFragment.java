package com.project.sportssync;

import android.content.Intent;
import android.net.Uri;
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
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminAttendanceHistoryFragment extends Fragment {

    private FirebaseFirestore db;
    private ListView lvAttendanceHistory;
    private EditText etFilterUucms, etFilterSport, etFilterDate;
    private Spinner spinnerFilterStatus;
    private Button btnExportExcel, btnDeleteOldData;
    private List<Map<String, Object>> allAttendance = new ArrayList<>();
    private List<Map<String, Object>> filteredAttendance = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_attendance_history, container, false);

        db = FirebaseFirestore.getInstance();
        initViews(view);
        setupFilters();
        loadAttendanceHistory();

        return view;
    }

    private void initViews(View view) {
        lvAttendanceHistory = view.findViewById(R.id.lvAttendanceHistory);
        etFilterUucms = view.findViewById(R.id.etFilterUucms);
        etFilterSport = view.findViewById(R.id.etFilterSport);
        etFilterDate = view.findViewById(R.id.etFilterDate);
        spinnerFilterStatus = view.findViewById(R.id.spinnerFilterStatus);
        btnExportExcel = view.findViewById(R.id.btnExportExcel);
        btnDeleteOldData = view.findViewById(R.id.btnDeleteOldData);

        // Setup status filter spinner
        String[] statusOptions = {"All", "Present", "Absent"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                statusOptions
        );
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilterStatus.setAdapter(statusAdapter);
    }

    private void setupFilters() {
        btnExportExcel.setOnClickListener(v -> exportToExcel());
        btnDeleteOldData.setOnClickListener(v -> deleteOldData());

        etFilterUucms.setOnEditorActionListener((v, actionId, event) -> {
            applyFilters();
            return true;
        });

        etFilterSport.setOnEditorActionListener((v, actionId, event) -> {
            applyFilters();
            return true;
        });

        etFilterDate.setOnEditorActionListener((v, actionId, event) -> {
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

    private void loadAttendanceHistory() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        Date thirtyDaysAgo = calendar.getTime();

        db.collection("attendance")
                .whereGreaterThan("timestamp", new com.google.firebase.Timestamp(thirtyDaysAgo))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allAttendance.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> attendance = new HashMap<>();
                        attendance.put("id", doc.getId());
                        attendance.put("userId", doc.getString("userId"));
                        attendance.put("date", doc.getString("date"));
                        attendance.put("sport", doc.getString("sport"));
                        attendance.put("status", doc.getString("status"));
                        attendance.put("timestamp", doc.getTimestamp("timestamp"));
                        allAttendance.add(attendance);
                    }
                    applyFilters();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load attendance: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void applyFilters() {
        filteredAttendance.clear();

        String filterUucms = etFilterUucms.getText().toString().trim().toLowerCase();
        String filterSport = etFilterSport.getText().toString().trim().toLowerCase();
        String filterDate = etFilterDate.getText().toString().trim();
        String filterStatus = spinnerFilterStatus.getSelectedItem().toString();

        for (Map<String, Object> attendance : allAttendance) {
            boolean matchesUucms = TextUtils.isEmpty(filterUucms) ||
                    attendance.get("userId").toString().toLowerCase().contains(filterUucms);
            boolean matchesSport = TextUtils.isEmpty(filterSport) ||
                    attendance.get("sport").toString().toLowerCase().contains(filterSport);
            boolean matchesDate = TextUtils.isEmpty(filterDate) ||
                    attendance.get("date").toString().contains(filterDate);
            boolean matchesStatus = "All".equals(filterStatus) ||
                    filterStatus.equalsIgnoreCase(attendance.get("status").toString());

            if (matchesUucms && matchesSport && matchesDate && matchesStatus) {
                filteredAttendance.add(attendance);
            }
        }

        updateListView();
    }

    private void updateListView() {
        List<String> displayList = new ArrayList<>();
        for (Map<String, Object> attendance : filteredAttendance) {
            String display = String.format("%s - %s (%s) - %s",
                    attendance.get("userId"),
                    attendance.get("sport"),
                    attendance.get("date"),
                    attendance.get("status"));
            displayList.add(display);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                displayList
        );
        lvAttendanceHistory.setAdapter(adapter);
    }

    private void exportToExcel() {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Attendance History");

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"User ID", "Date", "Sport", "Status"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);

                // set column width wide
                sheet.setColumnWidth(i, 20 * 256);
            }

            // Data rows
            int rowNum = 1;
            for (Map<String, Object> attendance : filteredAttendance) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(attendance.get("userId").toString());
                row.createCell(1).setCellValue(attendance.get("date").toString());
                row.createCell(2).setCellValue(attendance.get("sport").toString());
                row.createCell(3).setCellValue(attendance.get("status").toString());
            }

            File dir = new File(requireContext().getExternalFilesDir(null), "exports");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "attendance_history_" + getCurrentDate() + ".xlsx");
            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            workbook.close();
            fos.close();

            Intent intentShare = new Intent(Intent.ACTION_SEND);
            intentShare.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            Uri uri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", file);
            intentShare.putExtra(Intent.EXTRA_STREAM, uri);
            intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intentShare, "Share Excel File"));

            Toast.makeText(requireContext(), "Excel file exported successfully", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void deleteOldData() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        Date thirtyDaysAgo = calendar.getTime();

        db.collection("attendance")
                .whereLessThan("timestamp", new com.google.firebase.Timestamp(thirtyDaysAgo))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = 0;
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        db.collection("attendance").document(doc.getId()).delete();
                        count++;
                    }
                    Toast.makeText(requireContext(), "Deleted " + count + " old records", Toast.LENGTH_LONG).show();
                    loadAttendanceHistory();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to delete old data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}