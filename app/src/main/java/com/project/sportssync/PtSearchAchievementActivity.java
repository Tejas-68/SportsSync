package com.project.sportssync;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class PtSearchAchievementActivity extends AppCompatActivity {

    private EditText etSearch;
    private Button btnSearch;
    private LinearLayout llResults;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pt_search_achievement);

        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        llResults = findViewById(R.id.llResults);
        db = FirebaseFirestore.getInstance();

        btnSearch.setOnClickListener(v -> searchAchievements());
    }

    private void searchAchievements() {
        String queryText = etSearch.getText().toString().trim();

        if (TextUtils.isEmpty(queryText)) {
            Toast.makeText(this, "Enter UUCMS ID or Name", Toast.LENGTH_SHORT).show();
            return;
        }

        llResults.removeAllViews();

        // Search by UUCMS OR studentName the fuckinn search button needs to be added
        db.collection("achievements")
                .whereEqualTo("uucms", queryText)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        for (QueryDocumentSnapshot doc : snap) {
                            addAchievementView(doc);
                        }
                    } else {
                        db.collection("achievements")
                                .whereEqualTo("studentName", queryText)
                                .get()
                                .addOnSuccessListener(nameSnap -> {
                                    if (!nameSnap.isEmpty()) {
                                        for (QueryDocumentSnapshot doc : nameSnap) {
                                            addAchievementView(doc);
                                        }
                                    } else {
                                        Toast.makeText(this, "No achievements found", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void addAchievementView(QueryDocumentSnapshot doc) {
        LayoutInflater inflater = LayoutInflater.from(this);
        TextView tv = new TextView(this);
        String info = "🏆 " + doc.getString("title") +
                "\n👤 " + doc.getString("studentName") +
                " (" + doc.getString("uucms") + ")" +
                "\n📅 " + doc.getTimestamp("createdAt");
        tv.setText(info);
        tv.setPadding(10, 20, 10, 20);
        llResults.addView(tv);
    }
}