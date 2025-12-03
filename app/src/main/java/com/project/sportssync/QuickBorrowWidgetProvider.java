package com.project.sportssync;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

public class QuickBorrowWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE);
        String sportName = prefs.getString("widget_" + appWidgetId + "_sport", "Quick Borrow");
        String equipmentName = prefs.getString("widget_" + appWidgetId + "_equipment", "");

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_borrow);
        
        if (!equipmentName.isEmpty()) {
            views.setTextViewText(R.id.widget_borrow_text, "Borrow\n" + equipmentName);
        } else {
            views.setTextViewText(R.id.widget_borrow_text, "Quick Borrow");
        }

        // Create Intent to launch QuickBorrowActionActivity
        Intent intent = new Intent(context, QuickBorrowActionActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                appWidgetId, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        views.setOnClickPendingIntent(R.id.widget_borrow_icon, pendingIntent);
        views.setOnClickPendingIntent(R.id.widget_borrow_text, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
