package com.project.sportssync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LogoutReceiver extends BroadcastReceiver {

    private FirebaseFirestore db;

    @Override
    public void onReceive(Context context, Intent intent) {
        db = FirebaseFirestore.getInstance();
        
        SharedPreferences prefs = com.project.sportssync.security.SecurePrefs.get(context);
        String userId = prefs.getString("userId", null);
        String role = prefs.getString("role", null);
        
        if (userId != null && "student".equals(role)) {
            logoutStudent(context, userId);
        }
        
        scheduleNextLogout(context);
    }

    private void logoutStudent(Context context, String userId) {
        String today = getCurrentDate();
        
        db.collection("attendanceRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", today)
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var doc : querySnapshot.getDocuments()) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("status", "exited");
                        updates.put("exitTime", Timestamp.now());
                        
                        db.collection("attendanceRequests").document(doc.getId())
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    
                                    SharedPreferences prefs = com.project.sportssync.security.SecurePrefs.get(context);
                                    prefs.edit().clear().apply();
                                    
                                    Intent loginIntent = new Intent(context, LoginActivity.class);
                                    loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    context.startActivity(loginIntent);
                                })
                                .addOnFailureListener(e -> {});
                    }
                })
                .addOnFailureListener(e -> {});
    }

    private void scheduleNextLogout(Context context) {
        Intent serviceIntent = new Intent(context, AutoLogoutService.class);
        context.startService(serviceIntent);
    }

    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}
