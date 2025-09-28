package com.project.sportssync;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import java.util.Calendar;

public class AutoLogoutService extends Service {

    private static final String TAG = "AutoLogoutService";
    private static final int LOGOUT_ALARM_ID = 1001;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AutoLogoutService created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        scheduleLogout();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void scheduleLogout() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent logoutIntent = new Intent(this, LogoutReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 
                LOGOUT_ALARM_ID, 
                logoutIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 19); // 7 PM
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        Log.d(TAG, "Logout scheduled for: " + calendar.getTime());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AutoLogoutService destroyed");
    }
}
