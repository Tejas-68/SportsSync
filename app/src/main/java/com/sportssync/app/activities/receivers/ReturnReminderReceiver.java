package com.sportssync.app.activities.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.sportssync.app.activities.utils.NotificationHelper;

public class ReturnReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String equipmentName = intent.getStringExtra("equipmentName");
        String reminderType = intent.getStringExtra("reminderType");

        String returnTime = reminderType.equals("4pm") ? "5:00 PM" : "immediately";

        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.showReturnReminder(equipmentName, returnTime);
    }
}