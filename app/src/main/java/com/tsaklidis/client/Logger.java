package com.tsaklidis.client;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;


public class Logger extends AppWidgetProvider {
    public static final String ACTION_WIDGET_REFRESH = "com.tsaklidis.client.ACTION_WIDGET_REFRESH";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.logger);

        // Fetch data specifically for the widget
        // Data constructor will call appWidgetManager.updateAppWidget
        new Data(context, appWidgetManager, views, appWidgetId);

        // Explicitly set the click intent to refresh
        Intent intent = new Intent(context, Logger.class);
        intent.setAction(ACTION_WIDGET_REFRESH);
        // Important: use a unique request code (appWidgetId) to ensure it's not grouped
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);
        // Also set on all children just in case the launcher consumes root clicks
        views.setOnClickPendingIntent(R.id.temperature, pendingIntent);
        views.setOnClickPendingIntent(R.id.title, pendingIntent);
        views.setOnClickPendingIntent(R.id.created_on, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d("Logger", "onReceive: " + intent.getAction());
        if (ACTION_WIDGET_REFRESH.equals(intent.getAction())) {
            Toast.makeText(context, "Ανανέωση...", Toast.LENGTH_SHORT).show();
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, Logger.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            onUpdate(context, appWidgetManager, appWidgetIds);
        }
    }
}
