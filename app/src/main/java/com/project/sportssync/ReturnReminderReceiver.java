package com.project.sportssync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.firestore.FirebaseFirestore;

public class ReturnReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String userId = intent.getStringExtra("userId");
        if (userId == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Check if user still has borrowed items
        db.collection("borrowRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "borrowed")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // User has active borrows, send notification
                        NotificationHelper.sendReturnReminderNotification(context);
                    }
                });
    }
}
