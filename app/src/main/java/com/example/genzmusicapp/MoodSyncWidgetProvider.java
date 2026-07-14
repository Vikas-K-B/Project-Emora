package com.example.genzmusicapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class MoodSyncWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_UPDATE_WIDGET = "com.example.genzmusicapp.ACTION_UPDATE_WIDGET";
    public static final String EXTRA_BPM = "extra_bpm";
    public static final String EXTRA_MOOD = "extra_mood";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, "--", "Status: Syncing...");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_UPDATE_WIDGET.equals(intent.getAction())) {
            int bpm = intent.getIntExtra(EXTRA_BPM, 0);
            String mood = intent.getStringExtra(EXTRA_MOOD);
            if (mood == null) mood = "Status: Syncing...";
            else mood = "Mood: " + mood;

            String bpmText = (bpm > 0) ? bpm + " bpm" : "-- bpm";

            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName thisWidget = new ComponentName(context, MoodSyncWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
            
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, bpmText, mood);
            }
        }
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId, String bpmText, String moodText) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_mood_sync);
        views.setTextViewText(R.id.widgetBpmText, bpmText);
        views.setTextViewText(R.id.widgetMoodText, moodText);

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("OPEN_PLAYER", true);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        views.setOnClickPendingIntent(R.id.widgetTitle, pendingIntent);
        views.setOnClickPendingIntent(R.id.widgetBpmText, pendingIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
