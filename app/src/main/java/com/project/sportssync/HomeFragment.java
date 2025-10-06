package com.project.sportssync;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView txtStatus;
    private ImageView imgSport;
    private Button btnExit;

    private FirebaseFirestore db;
    private String userId, uucms;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        txtStatus = view.findViewById(R.id.txtStatus);
        imgSport = view.findViewById(R.id.imgSport);
        btnExit = view.findViewById(R.id.btnExit);

        db = FirebaseFirestore.getInstance();

        SharedPreferences prefs = com.project.sportssync.security.SecurePrefs.get(requireContext());
        userId = prefs.getString("userId", null);
        uucms = prefs.getString("uucms", null);

        txtStatus.setText("Not Present");
        txtStatus.setTextColor(Color.RED);
        btnExit.setVisibility(View.GONE);
        imgSport.setVisibility(View.GONE);

        if (userId != null && !userId.isEmpty()) {
            checkCurrentStatus();
        }

        btnExit.setOnClickListener(v -> logExit());

        return view;
    }

    private void checkCurrentStatus() {
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(getContext(), "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("attendanceRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "approved")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);

                        String sport = doc.getString("sport");

                        txtStatus.setText("Present");
                        txtStatus.setTextColor(Color.parseColor("#4CAF50")); // green
                        btnExit.setVisibility(View.VISIBLE);

                        if (sport != null) {
                            imgSport.setVisibility(View.VISIBLE);
                            switch (sport.toLowerCase()) {
                                case "football":
                                    imgSport.setImageResource(R.drawable.football);
                                    break;
                                case "cricket":
                                    imgSport.setImageResource(R.drawable.cricket);
                                    break;
                                case "volleyball":
                                    imgSport.setImageResource(R.drawable.volleyball);
                                    break;
                                case "badminton":
                                    imgSport.setImageResource(R.drawable.badminton);
                                    break;
                                default:
                                    imgSport.setVisibility(View.GONE);
                                    break;
                            }
                        }
                    }
                });
    }

    private void logExit() {
        db.collection("attendanceRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "approved")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String docId = querySnapshot.getDocuments().get(0).getId();

                        Map<String, Object> exitMap = new HashMap<>();
                        exitMap.put("status", "exited");
                        exitMap.put("exitTime", Timestamp.now());

                        db.collection("attendanceRequests").document(docId)
                                .update(exitMap)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Exit logged successfully", Toast.LENGTH_SHORT).show();
                                    txtStatus.setText("Not Present");
                                    txtStatus.setTextColor(Color.RED);
                                    btnExit.setVisibility(View.GONE);
                                    imgSport.setVisibility(View.GONE);
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    } else {
                        Toast.makeText(getContext(), "No active session found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
