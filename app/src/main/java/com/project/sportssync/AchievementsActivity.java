package com.project.sportssync;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AchievementsActivity extends AppCompatActivity {
    
    private RecyclerView recyclerAchievements;
    private ProgressBar progressBar;
    private AchievementAdapter adapter;
    
    private FirebaseFirestore db;
    private String userId;
    private SessionManager sessionManager;
    
    private List<Achievement> allAchievements;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);
        
        // Initialize Firebase and session
        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();
        
        // Initialize views
        recyclerAchievements = findViewById(R.id.recyclerAchievements);
        progressBar = findViewById(R.id.progressBar);
        
        // Setup RecyclerView
        recyclerAchievements.setLayoutManager(new GridLayoutManager(this, 2));
        allAchievements = new ArrayList<>();
        adapter = new AchievementAdapter(allAchievements);
        recyclerAchievements.setAdapter(adapter);
        
        // Define all achievements
        defineAchievements();
        
        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        
        // Load achievements
        loadAchievements();
    }
    
    private void defineAchievements() {
        allAchievements.clear();
        
        // 1. First Step - Attend first session
        allAchievements.add(new Achievement(
                "first_step",
                "First Step",
                "Attend your first session",
                "🎯",
                1,
                AchievementType.TOTAL_DAYS
        ));
        
        // 2. Week Warrior - 7 consecutive days
        allAchievements.add(new Achievement(
                "week_warrior",
                "Week Warrior",
                "7 day attendance streak",
                "⚡",
                7,
                AchievementType.STREAK
        ));
        
        // 3. Month Master - 30 days total
        allAchievements.add(new Achievement(
                "month_master",
                "Month Master",
                "Attend 30 days total",
                "📅",
                30,
                AchievementType.TOTAL_DAYS
        ));
        
        // 4. Sport Explorer - Try 3 different sports
        allAchievements.add(new Achievement(
                "sport_explorer",
                "Sport Explorer",
                "Try 3 different sports",
                "🌟",
                3,
                AchievementType.DIFFERENT_SPORTS
        ));
        
        // 5. Equipment Pro - Borrow 5 items
        allAchievements.add(new Achievement(
                "equipment_pro",
                "Equipment Pro",
                "Borrow 5 items",
                "🎒",
                5,
                AchievementType.EQUIPMENT_BORROWED
        ));
        
        // 6. Perfect Week - 100% attendance for a week
        allAchievements.add(new Achievement(
                "perfect_week",
                "Perfect Week",
                "7 consecutive days",
                "💯",
                7,
                AchievementType.STREAK
        ));
        
        // 7. Century Club - 100 days total
        allAchievements.add(new Achievement(
                "century_club",
                "Century Club",
                "Attend 100 days total",
                "💯",
                100,
                AchievementType.TOTAL_DAYS
        ));
        
        // 8. Multi-Sport Star - Attend all available sports
        allAchievements.add(new Achievement(
                "multi_sport_star",
                "Multi-Sport Star",
                "Try all sports",
                "⭐",
                5, // Assuming 5 sports
                AchievementType.DIFFERENT_SPORTS
        ));
        
        // 9. Early Bird - 10 sessions before 9 AM
        allAchievements.add(new Achievement(
                "early_bird",
                "Early Bird",
                "10 early check-ins",
                "🌅",
                10,
                AchievementType.EARLY_CHECKINS
        ));
        
        // 10. Streak Legend - 30 day streak
        allAchievements.add(new Achievement(
                "streak_legend",
                "Streak Legend",
                "30 day attendance streak",
                "🔥",
                30,
                AchievementType.STREAK
        ));
        
        // 11. Year Champion - 365 days total
        allAchievements.add(new Achievement(
                "year_champion",
                "Year Champion",
                "Attend 365 days total",
                "👑",
                365,
                AchievementType.TOTAL_DAYS
        ));
        
        // 12. Hall of Fame - Top 10 all-time attendance
        allAchievements.add(new Achievement(
                "hall_of_fame",
                "Hall of Fame",
                "Top 10 all-time",
                "🏆",
                10,
                AchievementType.TOP_RANK
        ));
    }
    
    private void loadAchievements() {
        // Load attendance data
        db.collection("attendanceRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(attendanceSnapshot -> {
                    List<Map<String, Object>> attendanceRecords = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot doc : attendanceSnapshot) {
                        Map<String, Object> record = new HashMap<>();
                        record.put("sport", doc.getString("sport"));
                        record.put("requestedAt", doc.getTimestamp("requestedAt"));
                        attendanceRecords.add(record);
                    }
                    
                    // Load equipment data
                    db.collection("borrowRequests")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(equipmentSnapshot -> {
                                int totalEquipmentBorrowed = equipmentSnapshot.size();
                                
                                // Calculate achievement progress
                                calculateAchievementProgress(attendanceRecords, totalEquipmentBorrowed);
                                
                                progressBar.setVisibility(View.GONE);
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Failed to load equipment data", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load achievements", Toast.LENGTH_LONG).show();
                });
    }
    
    private void calculateAchievementProgress(List<Map<String, Object>> attendanceRecords, int equipmentCount) {
        // Calculate stats
        int totalDays = calculateTotalDays(attendanceRecords);
        int currentStreak = calculateStreak(attendanceRecords);
        int differentSports = calculateDifferentSports(attendanceRecords);
        int earlyCheckins = calculateEarlyCheckins(attendanceRecords);
        
        // Update each achievement
        for (Achievement achievement : allAchievements) {
            int currentProgress = 0;
            
            switch (achievement.getType()) {
                case TOTAL_DAYS:
                    currentProgress = totalDays;
                    break;
                case STREAK:
                    currentProgress = currentStreak;
                    break;
                case DIFFERENT_SPORTS:
                    currentProgress = differentSports;
                    break;
                case EQUIPMENT_BORROWED:
                    currentProgress = equipmentCount;
                    break;
                case EARLY_CHECKINS:
                    currentProgress = earlyCheckins;
                    break;
                case TOP_RANK:
                    currentProgress = 0;
                    break;
            }
            
            achievement.setCurrentProgress(currentProgress);
            achievement.setUnlocked(currentProgress >= achievement.getRequiredProgress());
        }
    }
    
    private int calculateTotalDays(List<Map<String, Object>> records) {
        Set<String> uniqueDates = new HashSet<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        for (Map<String, Object> record : records) {
            Timestamp timestamp = (Timestamp) record.get("requestedAt");
            if (timestamp != null) {
                uniqueDates.add(dateFormat.format(timestamp.toDate()));
            }
        }
        
        return uniqueDates.size();
    }
    
    private int calculateStreak(List<Map<String, Object>> records) {
        if (records.isEmpty()) return 0;
        
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
        
        if (uniqueDates.isEmpty()) return 0;
        
        Collections.sort(uniqueDates, Collections.reverseOrder());
        
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
        if (latestDate.before(yesterday.getTime())) return 0;
        
        int streak = 1;
        Calendar cal = Calendar.getInstance();
        cal.setTime(latestDate);
        
        for (int i = 1; i < uniqueDates.size(); i++) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
            Date expectedDate = cal.getTime();
            Date actualDate = uniqueDates.get(i);
            
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            if (fmt.format(expectedDate).equals(fmt.format(actualDate))) {
                streak++;
            } else {
                break;
            }
        }
        
        return streak;
    }
    
    private int calculateDifferentSports(List<Map<String, Object>> records) {
        Set<String> uniqueSports = new HashSet<>();
        
        for (Map<String, Object> record : records) {
            String sport = (String) record.get("sport");
            if (sport != null && !sport.isEmpty()) {
                uniqueSports.add(sport);
            }
        }
        
        return uniqueSports.size();
    }
    
    private int calculateEarlyCheckins(List<Map<String, Object>> records) {
        int count = 0;
        Calendar cal = Calendar.getInstance();
        
        for (Map<String, Object> record : records) {
            Timestamp timestamp = (Timestamp) record.get("requestedAt");
            if (timestamp != null) {
                cal.setTime(timestamp.toDate());
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                if (hour < 9) {
                    count++;
                }
            }
        }
        
        return count;
    }
    
    // Achievement Type Enum
    enum AchievementType {
        TOTAL_DAYS,
        STREAK,
        DIFFERENT_SPORTS,
        EQUIPMENT_BORROWED,
        EARLY_CHECKINS,
        TOP_RANK
    }
    
    // Achievement Model Class
    static class Achievement {
        private String id;
        private String title;
        private String description;
        private String icon;
        private int requiredProgress;
        private int currentProgress;
        private boolean unlocked;
        private AchievementType type;
        
        public Achievement(String id, String title, String description, String icon, 
                         int requiredProgress, AchievementType type) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.requiredProgress = requiredProgress;
            this.type = type;
            this.currentProgress = 0;
            this.unlocked = false;
        }
        
        // Getters and setters
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getIcon() { return icon; }
        public int getRequiredProgress() { return requiredProgress; }
        public int getCurrentProgress() { return currentProgress; }
        public boolean isUnlocked() { return unlocked; }
        public AchievementType getType() { return type; }
        
        public void setCurrentProgress(int progress) { this.currentProgress = progress; }
        public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
        
        public int getProgressPercentage() {
            if (requiredProgress == 0) return 0;
            return Math.min(100, (currentProgress * 100) / requiredProgress);
        }
    }
}
