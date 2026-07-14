package com.example.genzmusicapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.genzmusicapp.db.AppDatabase;
import com.example.genzmusicapp.db.WellnessHistory;

import java.util.List;
import java.util.concurrent.Executors;

public class WellnessNotificationReceiver extends BroadcastReceiver {

    public static final String ACTION_DAILY_SUMMARY = "com.example.genzmusicapp.ACTION_DAILY_SUMMARY";
    public static final String ACTION_WEEKLY_SUMMARY = "com.example.genzmusicapp.ACTION_WEEKLY_SUMMARY";
    
    private static final String CHANNEL_ID = "WellnessSummaryChannel";
    private static final int NOTIFICATION_ID = 2001;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        
        SharedPreferences prefs = context.getSharedPreferences("MusicZPrefs", Context.MODE_PRIVATE);
        
        String action = intent.getAction();
        if (ACTION_DAILY_SUMMARY.equals(action)) {
            if (!prefs.getBoolean("prefDailySummary", true)) return;
            generateDailySummary(context);
        } else if (ACTION_WEEKLY_SUMMARY.equals(action)) {
            if (!prefs.getBoolean("prefWeeklySummary", true)) return;
            generateWeeklySummary(context);
        }
    }
    
    private void generateDailySummary(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<WellnessHistory> history = AppDatabase.getDatabase(context).wellnessDao().getRecentHistory(500);
            String content = "Check out your daily emotional stability breakdown!";
            if (history != null && !history.isEmpty()) {
                long avg = 0;
                for (WellnessHistory h : history) avg += h.bpm;
                avg /= history.size();
                
                String[] lowHrMessages = {
                    "Your average heart rate today was " + avg + " BPM. You maintained great calm today.",
                    "A very relaxed day! Your heart rate averaged " + avg + " BPM.",
                    "Your baseline was " + avg + " BPM today. Great job keeping stress low!"
                };
                
                String[] highHrMessages = {
                    "Your average heart rate today was " + avg + " BPM. It's been a high-energy day!",
                    "Quite an active day! You averaged " + avg + " BPM.",
                    "Your heart rate was elevated today at " + avg + " BPM. Take some time to rest."
                };
                
                int rnd = new java.util.Random().nextInt(3);
                content = (avg < 75) ? lowHrMessages[rnd] : highHrMessages[rnd];
            }
            sendNotification(context, "Daily Wellness Summary", content);
        });
    }

    private void generateWeeklySummary(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            String[] weeklyMessages = {
                "Your weekly stress timeline is ready! Check how your mood evolved this week.",
                "Your Weekly Wellness Report is in. Tap to see your emotional stability score.",
                "A new week, a new baseline! Review your weekly biometric summary now."
            };
            int rnd = new java.util.Random().nextInt(3);
            sendNotification(context, "Weekly Wellness Summary", weeklyMessages[rnd]);
        });
    }
    
    private void sendNotification(Context context, String title, String text) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Wellness Summaries", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        // Open the Wellness screen instead of Player since it's a summary
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 2, openIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        manager.notify(NOTIFICATION_ID + (int)(System.currentTimeMillis() % 1000), builder.build());
    }
}
