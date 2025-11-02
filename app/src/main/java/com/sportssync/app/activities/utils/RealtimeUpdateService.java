package com.sportssync.app.activities.utils;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.sportssync.app.activities.utils.FirebaseManager;

public class RealtimeUpdateService {

    private FirebaseManager firebaseManager;
    private ListenerRegistration attendanceListener;
    private ListenerRegistration returnListener;

    public interface OnAttendanceUpdateListener {
        void onAttendanceStatusChanged(String status);
    }

    public interface OnReturnUpdateListener {
        void onReturnStatusChanged(String status);
    }

    public RealtimeUpdateService() {
        firebaseManager = FirebaseManager.getInstance();
    }

    public void startListeningForAttendanceUpdates(String studentId, OnAttendanceUpdateListener listener) {
        attendanceListener = firebaseManager.getDb().collection("attendanceRequests")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String status = doc.getString("status");
                        if (status != null && !status.equals("pending")) {
                            listener.onAttendanceStatusChanged(status);
                        }
                    }
                });
    }

    public void startListeningForReturnUpdates(String studentId, OnReturnUpdateListener listener) {
        returnListener = firebaseManager.getDb().collection("returnRequests")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) {
                        return;
                    }

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String status = doc.getString("status");
                        if (status != null && !status.equals("pending")) {
                            listener.onReturnStatusChanged(status);
                        }
                    }
                });
    }

    public void stopListening() {
        if (attendanceListener != null) {
            attendanceListener.remove();
            attendanceListener = null;
        }
        if (returnListener != null) {
            returnListener.remove();
            returnListener = null;
        }
    }
}