package com.project.sportssync;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentAnalyticsActivity extends AppCompatActivity {
    
    private TextView tvDaysAttended, tvFavoriteSport, tvCurrentStreak, tvAttendancePercentage;
    private TextView tvTotalSessions, tvThisMonth, tvThisWeek;
    private ProgressBar progressBar;
    private View layoutStats;
    
    private FirebaseFirestore db;
    private String userId;
    private SessionManager sessionManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_analytics);
        
        // Initialize Firebase and session
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();
        
        // Initialize views
        tvDaysAttended = findViewById(R.id.tvDaysAttended);
        tvFavoriteSport = findViewById(R.id.tvFavoriteSport);
        tvCurrentStreak = findViewById(R.id.tvCurrentStreak);
        tvAttendancePercentage = findViewById(R.id.tvAttendancePercentage);
        tvTotalSessions = findViewById(R.id.tvTotalSessions);
        tvThisMonth = findViewById(R.id.tvThisMonth);
        tvThisWeek = findViewById(R.id.tvThisWeek);
        progressBar = findViewById(R.id.progressBar);
        layoutStats = findViewById(R.id.layoutStats);
        
        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        layoutStats.setVisibility(View.GONE);
        
        // Load analytics data
        loadAnalytics();
    }
    
    private void loadAnalytics() {
        // Query all approved attendance records for this user
        db.collection("attendanceRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> attendanceRecords = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        try {
                            Map<String, Object> record = new HashMap<>();
                            record.put("sport", doc.getString("sport"));
                            record.put("requestedAt", doc.getTimestamp("requestedAt"));
                            record.put("exitTime", doc.getTimestamp("exitTime"));
                            
                            // Only add if requestedAt is valid
                            if (record.get("requestedAt") != null) {
                                attendanceRecords.add(record);
                            }
                        } catch (Exception e) {
                            // Skip documents with invalid timestamp fields
                            android.util.Log.w("StudentAnalytics", "Skipping document: " + doc.getId());
                        }
                    }
                    
                    // Calculate all statistics
                    calculateStatistics(attendanceRecords);
                    
                    // Hide loading, show stats
                    progressBar.setVisibility(View.GONE);
                    layoutStats.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load analytics: " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                });
    }
    
    private void calculateStatistics(List<Map<String, Object>> records) {
        if (records.isEmpty()) {
            // No attendance records
            tvDaysAttended.setText("0");
            tvFavoriteSport.setText("None");
            tvCurrentStreak.setText("0");
            tvAttendancePercentage.setText("0%");
            tvTotalSessions.setText("0");
            tvThisMonth.setText("0");
            tvThisWeek.setText("0");
            return;
        }
        
        // 1. Calculate total unique days attended
        int totalDays = calculateTotalDays(records);
        tvDaysAttended.setText(String.valueOf(totalDays));
        
        // 2. Calculate favorite sport
        String favoriteSport = calculateFavoriteSport(records);
        tvFavoriteSport.setText(favoriteSport);
        
        // 3. Calculate current streak
        int streak = calculateStreak(records);
        tvCurrentStreak.setText(String.valueOf(streak));
        
        // 4. Calculate attendance percentage (days attended vs days since first attendance)
        double percentage = calculateAttendancePercentage(records, totalDays);
        tvAttendancePercentage.setText(String.format(Locale.getDefault(), "%.1f%%", percentage));
        
        // 5. Total sessions (can attend multiple sports in one day)
        tvTotalSessions.setText(String.valueOf(records.size()));
        
        // 6. This month attendance
        int thisMonth = calculateThisMonth(records);
        tvThisMonth.setText(String.valueOf(thisMonth));
        
        // 7. This week attendance
        int thisWeek = calculateThisWeek(records);
        tvThisWeek.setText(String.valueOf(thisWeek));
    }
    
    private int calculateTotalDays(List<Map<String, Object>> records) {
        List<String> uniqueDates = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        for (Map<String, Object> record : records) {
            Timestamp timestamp = (Timestamp) record.get("requestedAt");
            if (timestamp != null) {
                String dateStr = dateFormat.format(timestamp.toDate());
                if (!uniqueDates.contains(dateStr)) {
                    uniqueDates.add(dateStr);
                }
            }
        }
        
        return uniqueDates.size();
    }
    
    private String calculateFavoriteSport(List<Map<String, Object>> records) {
        Map<String, Integer> sportCount = new HashMap<>();
        
        for (Map<String, Object> record : records) {
            String sport = (String) record.get("sport");
            if (sport != null && !sport.isEmpty()) {
                sportCount.put(sport, sportCount.getOrDefault(sport, 0) + 1);
            }
        }
        
        if (sportCount.isEmpty()) {
            return "None";
        }
        
        // Find sport with max count
        String favoriteSport = "None";
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : sportCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                favoriteSport = entry.getKey();
            }
        }
        
        return favoriteSport;
    }
    
    private int calculateStreak(List<Map<String, Object>> records) {
        if (records.isEmpty()) {
            return 0;
        }
        
        // Get unique dates and sort them
        List<Date> uniqueDates = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        for (Map<String, Object> record : records) {
            Timestamp timestamp = (Timestamp) record.get("requestedAt");
            if (timestamp != null) {
                try {
                    String dateStr = dateFormat.format(timestamp.toDate());
                    Date date = dateFormat.parse(dateStr);
                    if (date != null && !uniqueDates.contains(date)) {
                        uniqueDates.add(date);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        if (uniqueDates.isEmpty()) {
            return 0;
        }
        
        Collections.sort(uniqueDates, Collections.reverseOrder());
        
        // Check if streak is current (includes today or yesterday)
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        yesterday.set(Calendar.HOUR_OF_DAY, 0);
        yesterday.set(Calendar.MINUTE, 0);
        yesterday.set(Calendar.SECOND, 0);
        yesterday.set(Calendar.MILLISECOND, 0);
        
        Date latestDate = uniqueDates.get(0);
        
        // If latest attendance is not today or yesterday, streak is broken
        if (latestDate.before(yesterday.getTime())) {
            return 0;
        }
        
        // Count consecutive days
        int streak = 1;
        Calendar cal = Calendar.getInstance();
        cal.setTime(latestDate);
        
        for (int i = 1; i < uniqueDates.size(); i++) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
            Date expectedDate = cal.getTime();
            Date actualDate = uniqueDates.get(i);
            
            // Compare dates (ignoring time)
            if (isSameDay(expectedDate, actualDate)) {
                streak++;
            } else {
                break;
            }
        }
        
        return streak;
    }
    
    private boolean isSameDay(Date date1, Date date2) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return fmt.format(date1).equals(fmt.format(date2));
    }
    
    private double calculateAttendancePercentage(List<Map<String, Object>> records, int totalDays) {
        if (records.isEmpty() || totalDays == 0) {
            return 0.0;
        }
        
        // Find first and last attendance date
        Date firstDate = null;
        Date lastDate = null;
        
        for (Map<String, Object> record : records) {
            Timestamp timestamp = (Timestamp) record.get("requestedAt");
            if (timestamp != null) {
                Date date = timestamp.toDate();
                if (firstDate == null || date.before(firstDate)) {
                    firstDate = date;
                }
                if (lastDate == null || date.after(lastDate)) {
                    lastDate = date;
                }
            }
        }
        
        if (firstDate == null || lastDate == null) {
            return 0.0;
        }
        
        // Calculate days between first and last attendance
        long diffInMillis = lastDate.getTime() - firstDate.getTime();
        long daysBetween = (diffInMillis / (1000 * 60 * 60 * 24)) + 1; // +1 to include both days
        
        if (daysBetween <= 0) {
            return 100.0; // Only one day of attendance
        }
        
        return (totalDays * 100.0) / daysBetween;
    }
    
    private int calculateThisMonth(List<Map<String, Object>> records) {
        Calendar thisMonth = Calendar.getInstance();
        int currentMonth = thisMonth.get(Calendar.MONTH);
        int currentYear = thisMonth.get(Calendar.YEAR);
        
        int count = 0;
        for (Map<String, Object> record : records) {
            Timestamp timestamp = (Timestamp) record.get("requestedAt");
            if (timestamp != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(timestamp.toDate());
                if (cal.get(Calendar.MONTH) == currentMonth && 
                    cal.get(Calendar.YEAR) == currentYear) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    private int calculateThisWeek(List<Map<String, Object>> records) {
        Calendar thisWeek = Calendar.getInstance();
        int currentWeek = thisWeek.get(Calendar.WEEK_OF_YEAR);
        int currentYear = thisWeek.get(Calendar.YEAR);
        
        int count = 0;
        for (Map<String, Object> record : records) {
            Timestamp timestamp = (Timestamp) record.get("requestedAt");
            if (timestamp != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(timestamp.toDate());
                if (cal.get(Calendar.WEEK_OF_YEAR) == currentWeek && 
                    cal.get(Calendar.YEAR) == currentYear) {
                    count++;
                }
            }
        }
        
        return count;
    }
}
