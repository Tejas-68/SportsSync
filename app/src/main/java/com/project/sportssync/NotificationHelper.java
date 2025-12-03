package com.project.sportssync;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for creating in-app notifications
 * Works on Firebase free tier - notifications appear when students open the app
 */
public class NotificationHelper {
    
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    
    /**
     * Send notification when attendance request is approved
     */
    public static void sendAttendanceApprovedNotification(String userId, String sport) {
        String title = "Attendance Approved ✓";
        String body = "Your attendance for " + sport + " has been approved!";
        createInAppNotification(userId, title, body, "attendance_approved");
    }
    
    /**
     * Send notification when attendance request is denied
     */
    public static void sendAttendanceDeniedNotification(String userId, String sport) {
        String title = "Attendance Denied";
        String body = "Your attendance request for " + sport + " was denied.";
        createInAppNotification(userId, title, body, "attendance_denied");
    }
    
    /**
     * Send notification when equipment return is approved
     */
    public static void sendEquipmentReturnApprovedNotification(String userId, String equipmentName) {
        String title = "Equipment Return Approved ✓";
        String body = "Your return of " + equipmentName + " has been approved!";
        createInAppNotification(userId, title, body, "equipment_approved");
    }
    
    /**
     * Send notification when equipment return is denied
     */
    public static void sendEquipmentReturnDeniedNotification(String userId, String equipmentName) {
        String title = "Equipment Return Denied";
        String body = "Your return request for " + equipmentName + " was denied.";
        createInAppNotification(userId, title, body, "equipment_denied");
    }
    
    /**
     * Send notification to PT when new request is received
     */
    public static void sendNewRequestNotificationToPT(String ptUserId, String requestType, String studentName) {
        String title = "New " + requestType + " Request";
        String body = studentName + " has submitted a " + requestType + " request.";
        createInAppNotification(ptUserId, title, body, "new_request");
    }

    /**
     * Send return reminder notification
     */
    public static void sendReturnReminderNotification(android.content.Context context) {
        android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        
        String channelId = "return_reminders";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId, "Return Reminders", android.app.NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
        
        android.app.Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new android.app.Notification.Builder(context, channelId);
        } else {
            builder = new android.app.Notification.Builder(context);
        }
        
        android.content.Intent intent = new android.content.Intent(context, StudentDashboardActivity.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                context, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        
        builder.setContentTitle("Return Equipment Reminder")
                .setContentText("Please return your borrowed sports equipment by 5:30 PM today.")
                .setSmallIcon(R.drawable.ic_notification) // Ensure this icon exists, fallback to a default if not sure
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
                
        notificationManager.notify(1001, builder.build());
    }
    
    /**
     * Create an in-app notification in Firestore
     * Students will see these when they open the app
     */
    private static void createInAppNotification(String userId, String title, String body, String type) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("body", body);
        notification.put("type", type);
        notification.put("read", false);
        notification.put("createdAt", Timestamp.now());
        
        db.collection("notifications")
                .add(notification)
                .addOnSuccessListener(ref -> 
                    android.util.Log.d("NotificationHelper", "Notification created: " + title))
                .addOnFailureListener(e -> 
                    android.util.Log.e("NotificationHelper", "Failed to create notification", e));
    }
}
