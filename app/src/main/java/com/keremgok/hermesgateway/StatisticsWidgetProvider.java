package com.keremgok.hermesgateway;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * App Widget Provider for Delivery Statistics Widget
 */
public class StatisticsWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "StatisticsWidget";
    private static final String ACTION_UPDATE = "com.keremgok.hermesgateway.ACTION_UPDATE_WIDGET";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called for " + appWidgetIds.length + " widgets");

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_UPDATE.equals(intent.getAction())) {
            Log.d(TAG, "Manual widget update requested");
            updateAllWidgets(context);
        }
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.d(TAG, "First widget added, widget enabled");
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "Last widget removed, widget disabled");
    }

    /**
     * Update a single app widget
     */
    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        try {
            Log.d(TAG, "Updating widget " + appWidgetId);

            // Get statistics data
            DeliveryStatistics statistics = new DeliveryStatistics(context);

            // Create RemoteViews
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_statistics);

            // Update statistics values
            views.setTextViewText(R.id.widget_webhook_count, String.valueOf(statistics.getWebhookSuccessCount()));
            views.setTextViewText(R.id.widget_sms_count, String.valueOf(statistics.getSmsSuccessCount()));
            views.setTextViewText(R.id.widget_email_count, String.valueOf(statistics.getEmailSuccessCount()));
            views.setTextViewText(R.id.widget_success_rate,
                    DeliveryStatistics.formatSuccessRate(statistics.getOverallSuccessRate()));

            // Update last update time
            String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
            views.setTextViewText(R.id.widget_last_update, currentTime);

            // Set up click intent to open main activity
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_statistics_container, pendingIntent);

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);

            Log.d(TAG, "Widget " + appWidgetId + " updated successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error updating widget " + appWidgetId, e);
        }
    }

    /**
     * Update all widgets of this provider
     */
    public static void updateAllWidgets(Context context) {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, StatisticsWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            Log.d(TAG, "Updating " + appWidgetIds.length + " widgets");

            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error updating all widgets", e);
        }
    }

    /**
     * Request widget update from other parts of the app
     */
    public static void requestWidgetUpdate(Context context) {
        try {
            Intent intent = new Intent(context, StatisticsWidgetProvider.class);
            intent.setAction(ACTION_UPDATE);
            context.sendBroadcast(intent);

            Log.d(TAG, "Widget update requested");

        } catch (Exception e) {
            Log.e(TAG, "Error requesting widget update", e);
        }
    }

    /**
     * Check if any widgets are currently active
     */
    public static boolean hasActiveWidgets(Context context) {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, StatisticsWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            return appWidgetIds.length > 0;

        } catch (Exception e) {
            Log.e(TAG, "Error checking active widgets", e);
            return false;
        }
    }

    /**
     * Get widget statistics for debugging
     */
    public static String getWidgetDebugInfo(Context context) {
        try {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, StatisticsWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

            StringBuilder info = new StringBuilder();
            info.append("Active widgets: ").append(appWidgetIds.length).append("\n");

            if (appWidgetIds.length > 0) {
                info.append("Widget IDs: ");
                for (int i = 0; i < appWidgetIds.length; i++) {
                    info.append(appWidgetIds[i]);
                    if (i < appWidgetIds.length - 1) {
                        info.append(", ");
                    }
                }
                info.append("\n");
            }

            DeliveryStatistics statistics = new DeliveryStatistics(context);
            info.append("Current stats - Webhook: ").append(statistics.getWebhookSuccessCount())
                    .append(", SMS: ").append(statistics.getSmsSuccessCount())
                    .append(", Email: ").append(statistics.getEmailSuccessCount())
                    .append(", Success Rate: ")
                    .append(DeliveryStatistics.formatSuccessRate(statistics.getOverallSuccessRate()));

            return info.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error getting widget debug info", e);
            return "Error getting widget info: " + e.getMessage();
        }
    }
}
