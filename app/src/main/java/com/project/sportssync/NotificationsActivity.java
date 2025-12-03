package com.project.sportssync;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity to display in-app notifications for students
 */
public class NotificationsActivity extends AppCompatActivity {
    
    private RecyclerView recyclerNotifications;
    private TextView tvNoNotifications;
    private FirebaseFirestore db;
    private String userId;
    private List<NotificationModel> notificationsList;
    private NotificationsAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        
        recyclerNotifications = findViewById(R.id.recyclerNotifications);
        tvNoNotifications = findViewById(R.id.tvNoNotifications);
        
        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");
        
        notificationsList = new ArrayList<>();
        adapter = new NotificationsAdapter(notificationsList, this::markAsRead);
        
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));
        recyclerNotifications.setAdapter(adapter);
        
        loadNotifications();
    }
    
    private void loadNotifications() {
        db.collection("notifications")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    notificationsList.clear();
                    querySnapshot.forEach(doc -> {
                        NotificationModel notification = doc.toObject(NotificationModel.class);
                        notification.setId(doc.getId());
                        notificationsList.add(notification);
                    });
                    adapter.notifyDataSetChanged();
                    
                    tvNoNotifications.setVisibility(notificationsList.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    android.widget.Toast.makeText(this, "Failed to load notifications", 
                            android.widget.Toast.LENGTH_SHORT).show();
                });
    }
    
    private void markAsRead(String notificationId) {
        db.collection("notifications")
                .document(notificationId)
                .update("read", true)
                .addOnSuccessListener(aVoid -> loadNotifications());
    }
    
    /**
     * Get count of unread notifications for a user
     */
    public static void getUnreadCount(String userId, UnreadCountCallback callback) {
        FirebaseFirestore.getInstance()
                .collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(querySnapshot -> callback.onCount(querySnapshot.size()))
                .addOnFailureListener(e -> callback.onCount(0));
    }
    
    interface UnreadCountCallback {
        void onCount(int count);
    }
}
