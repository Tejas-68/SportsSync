package com.project.sportssync;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class StudentDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private String userId, uucms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.dashboard_fragment_container, new HomeFragment())
                    .commit();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (itemId == R.id.nav_qr) {
                Intent qrIntent = new Intent(StudentDashboardActivity.this, QrScanActivity.class);
                startActivity(qrIntent);
                return true;
            } else if (itemId == R.id.nav_history) {
                loadFragment(new StudentHistoryFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
                Intent i = new Intent(StudentDashboardActivity.this, StudentProfileFragment.class);
                startActivity(i);
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.dashboard_fragment_container, fragment)
                .commit();
    }
}
