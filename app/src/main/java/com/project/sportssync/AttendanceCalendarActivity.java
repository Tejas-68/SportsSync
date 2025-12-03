package com.project.sportssync;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AttendanceCalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView tvMonthStats, tvDaysAttended, tvAttendancePercentage, tvSelectedDate;
    private ProgressBar progressBar;
    
    private String userId;
    private FirebaseFirestore db;
    
    // Store attendance dates by status for click listener
    private Map<String, String> attendanceDates; // date -> status (approved/pending/denied)
    private Set<String> approvedDates;
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private SimpleDateFormat selectedDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_calendar);

        userId = getIntent().getStringExtra("userId");
        
        calendarView = findViewById(R.id.calendarView);
        tvMonthStats = findViewById(R.id.tvMonthStats);
        tvDaysAttended = findViewById(R.id.tvDaysAttended);
        tvAttendancePercentage = findViewById(R.id.tvAttendancePercentage);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        progressBar = findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();
        attendanceDates = new HashMap<>();
        approvedDates = new HashSet<>();

        // Set calendar listeners
        calendarView.setOnDayClickListener(eventDay -> {
            Calendar clickedDay = eventDay.getCalendar();
            String dateKey = dateFormat.format(clickedDay.getTime());
            
            String status = attendanceDates.get(dateKey);
            if (status != null) {
                String statusText = status.equals("approved") ? "✓ Attended" :
                                  status.equals("pending") ? "⏳ Pending" : "✗ Denied";
                tvSelectedDate.setText(selectedDateFormat.format(clickedDay.getTime()) + " - " + statusText);
                tvSelectedDate.setVisibility(View.VISIBLE);
            } else {
                tvSelectedDate.setText(selectedDateFormat.format(clickedDay.getTime()) + " - No activity");
                tvSelectedDate.setVisibility(View.VISIBLE);
            }
        });

        // Listen for month changes to update stats
        calendarView.setOnForwardPageChangeListener(this::updateMonthlyStats);
        calendarView.setOnPreviousPageChangeListener(this::updateMonthlyStats);

        loadAttendanceData();
    }

    private void loadAttendanceData() {
        // 1. Load from local backup immediately
        BackupManager backupManager = new BackupManager(this);
        List<AttendanceRecord> localRecords = backupManager.getAttendanceHistory();
        
        if (localRecords != null && !localRecords.isEmpty()) {
            updateUIWithRecords(localRecords);
        }
        
        progressBar.setVisibility(View.VISIBLE);
        
        // 2. Fetch from Firebase in background
        db.collection("attendanceRequests")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<AttendanceRecord> firebaseRecords = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Timestamp timestamp = doc.getTimestamp("requestedAt");
                            String status = doc.getString("status");
                            String uucms = doc.getString("uucms");
                            String sport = doc.getString("sport");
                            String studentName = doc.getString("studentName"); // Might be null if not stored
                            
                            if (timestamp != null && status != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                                String timeStr = sdf.format(timestamp.toDate());
                                
                                AttendanceRecord record = new AttendanceRecord();
                                record.setUucms(uucms);
                                record.setStudentName(studentName);
                                record.setSport(sport);
                                record.setStatus(status);
                                record.setTimestamp(timeStr);
                                
                                firebaseRecords.add(record);
                            }
                        } catch (Exception e) {
                            android.util.Log.w("AttendanceCalendar", "Skipping document: " + doc.getId());
                        }
                    }
                    
                    // 3. Update local backup and UI
                    if (!firebaseRecords.isEmpty()) {
                        backupManager.saveAttendanceList(firebaseRecords);
                        updateUIWithRecords(firebaseRecords);
                    }
                    
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    // If local data exists, we don't need to show an error, just maybe a toast
                    if (localRecords == null || localRecords.isEmpty()) {
                        Toast.makeText(this, "Error loading attendance data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateUIWithRecords(List<AttendanceRecord> records) {
        attendanceDates.clear();
        approvedDates.clear();
        List<EventDay> events = new ArrayList<>();
        
        for (AttendanceRecord record : records) {
            try {
                String status = record.getStatus();
                String timestampStr = record.getTimestamp();
                
                if (timestampStr != null && status != null) {
                    // Parse timestamp string back to Calendar
                    // Format in XML is "yyyy-MM-dd HH:mm:ss" usually, but let's be careful
                    // The BackupManager writes what we give it. 
                    // In onApproveClicked (PtDashboard), we save "yyyy-MM-dd HH:mm:ss".
                    // In loadAttendanceData above, we format to "yyyy-MM-dd HH:mm:ss".
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(sdf.parse(timestampStr));
                    
                    String dateKey = dateFormat.format(calendar.getTime());
                    
                    // Check if we already have a status for this date
                    String currentStatus = attendanceDates.get(dateKey);
                    
                    boolean shouldUpdate = false;
                    if (currentStatus == null) {
                        shouldUpdate = true;
                    } else if ("approved".equals(status)) {
                        shouldUpdate = true;
                    } else if ("pending".equals(status) && !"approved".equals(currentStatus)) {
                        shouldUpdate = true;
                    }
                    
                    if (shouldUpdate) {
                        attendanceDates.put(dateKey, status);
                        
                        // Add event dot
                        int drawableRes = 0;
                        if ("approved".equals(status)) {
                            approvedDates.add(dateKey);
                            drawableRes = R.drawable.bg_status_dot_green;
                        } else if ("pending".equals(status)) {
                            drawableRes = R.drawable.bg_status_dot_yellow;
                        } else if ("denied".equals(status)) {
                            drawableRes = R.drawable.bg_status_dot_red;
                        }
                        
                        if (drawableRes != 0) {
                            events.add(new EventDay(calendar, drawableRes));
                        }
                    }
                }
            } catch (Exception e) {
                // Log but continue
                android.util.Log.w("AttendanceCalendar", "Error parsing record: " + e.getMessage());
            }
        }
        
        calendarView.setEvents(events);
        updateMonthlyStats();
    }

    private void updateMonthlyStats() {
        // Get the currently visible month from the calendar view
        Calendar currentPageDate = calendarView.getCurrentPageDate();
        
        int currentMonth = currentPageDate.get(Calendar.MONTH);
        int currentYear = currentPageDate.get(Calendar.YEAR);
        
        // Count days in current month
        int daysInMonth = currentPageDate.getActualMaximum(Calendar.DAY_OF_MONTH);
        int approvedInMonth = 0;
        
        for (String dateKey : approvedDates) {
            try {
                Calendar dateCal = Calendar.getInstance();
                dateCal.setTime(dateFormat.parse(dateKey));
                
                if (dateCal.get(Calendar.MONTH) == currentMonth && 
                    dateCal.get(Calendar.YEAR) == currentYear) {
                    approvedInMonth++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Update UI
        tvMonthStats.setText(displayFormat.format(currentPageDate.getTime()));
        tvDaysAttended.setText(String.valueOf(approvedInMonth));
        
        int percentage = (int) ((approvedInMonth * 100.0) / daysInMonth);
        tvAttendancePercentage.setText(percentage + "%");
    }
}
