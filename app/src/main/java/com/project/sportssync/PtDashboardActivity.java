package com.project.sportssync;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class PtDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pt_dashboard);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.dashboard_fragment_container, new AdminActiveStudentsFragment())
                    .commit();
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_active_students) {
                loadFragment(new AdminActiveStudentsFragment());
                return true;
            } else if (itemId == R.id.nav_achievements) {
                loadFragment(new AdminAchievementsFragment());
                return true;
            } else if (itemId == R.id.nav_attendance_history) {
                loadFragment(new AdminAttendanceHistoryFragment());
                return true;
            } else if (itemId == R.id.nav_profile) {
                loadFragment(new AdminProfileFragment());
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