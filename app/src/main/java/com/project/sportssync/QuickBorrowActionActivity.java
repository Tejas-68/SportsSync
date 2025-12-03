package com.project.sportssync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuickBorrowActionActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // No setContentView because this is a transparent activity

        db = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        int appWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        performBorrow(appWidgetId);
    }

    private void performBorrow(int appWidgetId) {
        SharedPreferences prefs = getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE);
        String sportId = prefs.getString("widget_" + appWidgetId + "_sportId", null);
        String sportName = prefs.getString("widget_" + appWidgetId + "_sport", null);
        String equipmentKey = prefs.getString("widget_" + appWidgetId + "_equipmentKey", null);
        String equipmentName = prefs.getString("widget_" + appWidgetId + "_equipment", null);

        if (sportId == null || equipmentKey == null) {
            Toast.makeText(this, "Widget configuration error", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Toast.makeText(this, "Borrowing " + equipmentName + "...", Toast.LENGTH_SHORT).show();

        String userId = sessionManager.getUserId();
        String uucms = sessionManager.getUucms();
        String studentName = sessionManager.getName();

        // Calculate deadline timestamp (End of today)
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Timestamp borrowedUntil = new Timestamp(calendar.getTime());

        List<BorrowRequest.BorrowedEquipment> selectedItems = new ArrayList<>();
        selectedItems.add(new BorrowRequest.BorrowedEquipment(equipmentName, 1));

        Map<String, Object> requestData = new HashMap<>();
        requestData.put("userId", userId);
        requestData.put("uucms", uucms);
        requestData.put("sport", sportName);
        requestData.put("sportId", sportId);
        requestData.put("equipment", selectedItems);
        requestData.put("status", "borrowed");
        requestData.put("borrowedAt", Timestamp.now());
        requestData.put("borrowedUntil", borrowedUntil);
        requestData.put("penaltyPoints", 0);
        requestData.put("reminderSent", false);
        requestData.put("type", "borrow");
        
        if (studentName != null && !studentName.isEmpty()) {
            requestData.put("studentName", studentName);
        }

        // Atomic update
        Map<String, Object> updates = new HashMap<>();
        updates.put("equipment." + equipmentKey + ".availableQuantity", FieldValue.increment(-1));

        db.collection("sports").document(sportId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    db.collection("borrowRequests")
                            .add(requestData)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(this, "Successfully borrowed " + equipmentName + "!", Toast.LENGTH_LONG).show();
                                scheduleReturnReminder(userId);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // Rollback
                                Map<String, Object> rollback = new HashMap<>();
                                rollback.put("equipment." + equipmentKey + ".availableQuantity", FieldValue.increment(1));
                                db.collection("sports").document(sportId).update(rollback);
                                Toast.makeText(this, "Failed to create record", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Item not available or error occurred", Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void scheduleReturnReminder(String userId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReturnReminderReceiver.class);
        intent.putExtra("userId", userId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 17);
        calendar.set(Calendar.MINUTE, 30);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() > System.currentTimeMillis()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        }
    }
}
