package com.project.sportssync;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "sportssync_notifications";
    private static final String CHANNEL_NAME = "SportsSync Notifications";
    
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            
            sendNotification(title, body, remoteMessage.getData().get("type"));
        }
        
        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            String type = remoteMessage.getData().get("type");
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            
            if (title != null && body != null) {
                sendNotification(title, body, type);
            }
        }
    }
    
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        
        // Save token to Firestore for current user
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            String userId = sessionManager.getUserId();
            updateFCMToken(userId, token);
        }
    }
    
    private void updateFCMToken(String userId, String token) {
        // Update FCM token in Firestore
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> 
                    android.util.Log.d("FCM", "Token updated successfully"))
                .addOnFailureListener(e -> 
                    android.util.Log.e("FCM", "Failed to update token", e));
    }
    
    private void sendNotification(String title, String messageBody, String type) {
        Intent intent;
        
        // Determine which activity to open based on notification type
        SessionManager sessionManager = new SessionManager(this);
        String role = sessionManager.getRole();
        
        if ("student".equals(role)) {
            intent = new Intent(this, StudentDashboardActivity.class);
        } else {
            intent = new Intent(this, PtDashboardActivity.class);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 
                0, 
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody));
        
        NotificationManager notificationManager = 
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for attendance and equipment requests");
            channel.enableVibration(true);
            notificationManager.createNotificationChannel(channel);
        }
        
        notificationManager.notify(0, notificationBuilder.build());
    }
}
