package com.project.sportssync;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PtAddAchievementActivity extends AppCompatActivity {

    private EditText etUucms, etStudentName, etTitle;
    private Button btnAddAchievement;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pt_add_achievement);

        etUucms = findViewById(R.id.etUucms);
        etStudentName = findViewById(R.id.etStudentName);
        etTitle = findViewById(R.id.etTitle);
        btnAddAchievement = findViewById(R.id.btnAddAchievement);

        db = FirebaseFirestore.getInstance();

        btnAddAchievement.setOnClickListener(v -> saveAchievement());
    }

    private void saveAchievement() {
        String uucms = etUucms.getText().toString().trim();
        String studentName = etStudentName.getText().toString().trim();
        String title = etTitle.getText().toString().trim();

        if (TextUtils.isEmpty(uucms) || TextUtils.isEmpty(studentName) || TextUtils.isEmpty(title)) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String achievementId = UUID.randomUUID().toString();

        Map<String, Object> achievement = new HashMap<>();
        achievement.put("achievementId", achievementId);
        achievement.put("uucms", uucms);
        achievement.put("studentName", studentName);
        achievement.put("title", title);
        achievement.put("createdAt", Timestamp.now());
        achievement.put("addedBy", "admin");

        db.collection("achievements").document(achievementId)
                .set(achievement)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Achievement saved!", Toast.LENGTH_SHORT).show();
                    etUucms.setText("");
                    etStudentName.setText("");
                    etTitle.setText("");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}