package com.sportssync.app.activities.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.sportssync.app.activities.receivers.ReturnReminderReceiver;
import java.util.Calendar;

public class NotificationScheduler {

    private Context context;

    public NotificationScheduler(Context context) {
        this.context = context;
    }

    public void scheduleReturnReminder(String recordId, String equipmentName, long returnByTime) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Calendar returnTime = Calendar.getInstance();
        returnTime.setTimeInMillis(returnByTime);
        returnTime.set(Calendar.HOUR_OF_DAY, 16);
        returnTime.set(Calendar.MINUTE, 0);
        returnTime.set(Calendar.SECOND, 0);

        if (System.currentTimeMillis() < returnTime.getTimeInMillis()) {
            Intent intent = new Intent(context, ReturnReminderReceiver.class);
            intent.putExtra("recordId", recordId);
            intent.putExtra("equipmentName", equipmentName);
            intent.putExtra("reminderType", "4pm");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    recordId.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        returnTime.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        returnTime.getTimeInMillis(),
                        pendingIntent
                );
            }
        }

        Calendar returnTime5pm = Calendar.getInstance();
        returnTime5pm.setTimeInMillis(returnByTime);
        returnTime5pm.set(Calendar.HOUR_OF_DAY, 17);
        returnTime5pm.set(Calendar.MINUTE, 0);
        returnTime5pm.set(Calendar.SECOND, 0);

        if (System.currentTimeMillis() < returnTime5pm.getTimeInMillis()) {
            Intent intent5pm = new Intent(context, ReturnReminderReceiver.class);
            intent5pm.putExtra("recordId", recordId);
            intent5pm.putExtra("equipmentName", equipmentName);
            intent5pm.putExtra("reminderType", "5pm");

            PendingIntent pendingIntent5pm = PendingIntent.getBroadcast(
                    context,
                    (recordId + "_5pm").hashCode(),
                    intent5pm,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        returnTime5pm.getTimeInMillis(),
                        pendingIntent5pm
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        returnTime5pm.getTimeInMillis(),
                        pendingIntent5pm
                );
            }
        }
    }

    public void cancelReturnReminder(String recordId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, ReturnReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                recordId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);

        PendingIntent pendingIntent5pm = PendingIntent.getBroadcast(
                context,
                (recordId + "_5pm").hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent5pm);
    }
}