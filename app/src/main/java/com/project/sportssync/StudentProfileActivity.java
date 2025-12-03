package com.project.sportssync;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StudentProfileActivity extends AppCompatActivity {


    private FirebaseFirestore db;
    private String userId, uucms;
    private LinearLayout llAttendance, llAchievements;
    private TextView tvNoAttendance, tvNoAchievements;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");
        uucms = getIntent().getStringExtra("uucms");

        TextView tvHeader = findViewById(R.id.tvProfileHeader);
        llAttendance = findViewById(R.id.llAttendance);
        llAchievements = findViewById(R.id.llAchievements);
        tvNoAttendance = findViewById(R.id.tvNoAttendance);
        tvNoAchievements = findViewById(R.id.tvNoAchievements);

        tvHeader.setText("Profile - " + uucms);

        loadAttendance();
        loadAchievements();
    }

    private void loadAttendance() {
        db.collection("attendanceRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(qs -> {
                    llAttendance.removeAllViews();
                    if (qs.isEmpty()) {
                        tvNoAttendance.setVisibility(TextView.VISIBLE);
                    } else {
                        tvNoAttendance.setVisibility(TextView.GONE);
                        for (QueryDocumentSnapshot doc : qs) {
                            try {
                                String sport = doc.getString("sport");
                                
                                Timestamp entryTimestamp = doc.getTimestamp("requestedAt");
                                String entry = entryTimestamp != null ? 
                                    dateFormat.format(entryTimestamp.toDate()) : "-";
                                
                                Timestamp exitTimestamp = doc.getTimestamp("exitTime");
                                String exit = exitTimestamp != null ? 
                                    dateFormat.format(exitTimestamp.toDate()) : "Not exited";

                                TextView tv = new TextView(this);
                                tv.setText("🏃 " + sport + "\n   Entry: " + entry + "\n   Exit: " + exit);
                                tv.setTextSize(14);
                                tv.setPadding(16, 16, 16, 16);
                                tv.setBackgroundResource(R.drawable.card_background);
                                
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                );
                                params.setMargins(0, 0, 0, 16);
                                tv.setLayoutParams(params);
                                
                                llAttendance.addView(tv);
                            } catch (Exception e) {
                                android.util.Log.w("StudentProfile", "Skipping document: " + doc.getId());
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load attendance: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadAchievements() {
        db.collection("achievements")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(qs -> {
                    llAchievements.removeAllViews();
                    if (qs.isEmpty()) {
                        tvNoAchievements.setVisibility(TextView.VISIBLE);
                    } else {
                        tvNoAchievements.setVisibility(TextView.GONE);
                        for (QueryDocumentSnapshot doc : qs) {
                            String title = doc.getString("title");
                            String date = doc.getTimestamp("date") != null ? 
                                dateFormat.format(doc.getTimestamp("date").toDate()) : "-";

                            TextView tv = new TextView(this);
                            tv.setText("🏆 " + title + "\n   " + date);
                            tv.setTextSize(14);
                            tv.setPadding(16, 16, 16, 16);
                            tv.setBackgroundResource(R.drawable.card_background);
                            
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            params.setMargins(0, 0, 0, 16);
                            tv.setLayoutParams(params);
                            
                            llAchievements.addView(tv);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load achievements: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}