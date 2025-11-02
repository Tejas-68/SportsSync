package com.sportssync.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.sportssync.app.R;

import com.sportssync.app.activities.utils.PreferenceManager;

public class SplashActivity extends AppCompatActivity {

    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        preferenceManager = new PreferenceManager(this);

        new Handler().postDelayed(() -> {
            checkUserSession();
        }, 2000);
    }

    private void checkUserSession() {
        String userId = preferenceManager.getUserId();
        String userType = preferenceManager.getUserType();

        if (userId != null && userType != null) {
            if (userType.equals("student")) {
                startActivity(new Intent(this, StudentDashboardActivity.class));
            } else {
                startActivity(new Intent(this, AdminDashboardActivity.class));
            }
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }
}