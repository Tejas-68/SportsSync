package com.project.sportssync;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

public class PtManageAchievementsActivity extends AppCompatActivity {

    private LinearLayout llAchievements;
    private FirebaseFirestore db;
    private EditText etSearch;
    private Button btnSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pt_manage_achievements);

        llAchievements = findViewById(R.id.llAchievementsList);
        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);

        db = FirebaseFirestore.getInstance();

        // Load achievements with the null value
        loadAchievements(null);

        btnSearch.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            if (TextUtils.isEmpty(query)) {
                loadAchievements(null);
            } else {
                loadAchievements(query);
            }
        });
    }

    private void loadAchievements(String query) {
        if (query == null) {
            db.collection("achievements").get()
                    .addOnSuccessListener(qs -> showAchievements(qs))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            //run two queries Firestore doesn't support
            db.collection("achievements")
                    .whereEqualTo("uucms", query)
                    .get()
                    .addOnSuccessListener(qs1 -> {
                        db.collection("achievements")
                                .whereEqualTo("studentName", query)
                                .get()
                                .addOnSuccessListener(qs2 -> {
                                    // Merge both results
                                    qs1.getDocuments().addAll(qs2.getDocuments());
                                    showAchievements(qs1);
                                });
                    });
        }
    }

    private void showAchievements(QuerySnapshot qs) {
        llAchievements.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (DocumentSnapshot doc : qs.getDocuments()) {
            View card = inflater.inflate(R.layout.achievement_card, llAchievements, false);

            TextView tvTitle = card.findViewById(R.id.tvAchievementTitle);
            Button btnDelete = card.findViewById(R.id.btnDeleteAchievement);

            String id = doc.getId();
            String title = doc.getString("title");
            String uucms = doc.getString("uucms");
            String studentName = doc.getString("studentName");

            tvTitle.setText(uucms + " - " + studentName + " : " + title);

            btnDelete.setOnClickListener(v -> deleteAchievement(id, card));

            llAchievements.addView(card);
        }
    }

    private void deleteAchievement(String id, View card) {
        db.collection("achievements").document(id)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    llAchievements.removeView(card);
                    Toast.makeText(this, "Achievement deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}