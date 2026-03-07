package com.tsaklidis.client;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;


public class Logger extends AppWidgetProvider {
    private static Data data;
    private static final long REFRESH_THRESHOLD = 30000; // 30 seconds debounce
    public static final String ACTION_MANUAL_REFRESH = "com.tsaklidis.client.ACTION_MANUAL_REFRESH";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean isManual) {
        if (isManual) {
            SharedPreferences prefs = context.getSharedPreferences("LoggerPrefs", Context.MODE_PRIVATE);
            long lastUpdate = prefs.getLong("last_widget_refresh", 0);
            long now = System.currentTimeMillis();

            if (now - lastUpdate < REFRESH_THRESHOLD) {
                Log.d("Logger", "Refresh skipped - too frequent");
                return;
            }
            prefs.edit().putLong("last_widget_refresh", now).apply();
            Toast.makeText(context, "Ανανέωση...", Toast.LENGTH_SHORT).show();
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.logger);

        data = new Data(context, appWidgetManager, views, appWidgetId);

        // Create an Intent for manual refresh
        Intent intentUpdate = new Intent(context, Logger.class);
        intentUpdate.setAction(ACTION_MANUAL_REFRESH);

        // Wrap the intent as a PendingIntent
        PendingIntent pendingUpdate = PendingIntent.getBroadcast(
                context, 0, intentUpdate,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Attach the PendingIntent to the entire widget layout
        views.setOnClickPendingIntent(R.id.widget_root, pendingUpdate);
        views.setOnClickPendingIntent(R.id.created_on, pendingUpdate);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, false);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d("Logger", "onReceive() called with action: " + intent.getAction());
        
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, Logger.class));

        if (ACTION_MANUAL_REFRESH.equals(intent.getAction())) {
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, true);
            }
        } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }
}
