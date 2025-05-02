package com.example.myapplication;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class NotificationActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int alarmId = intent.getIntExtra("alarmId", 0);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(alarmId);

        if ("REMIND_LATER".equals(action)) {
            // Reprogrammer l'alarme après 10 minutes
            long triggerTime = System.currentTimeMillis() + 10 * 60 * 1000;

            Intent alarmIntent = new Intent(context, MedicationReminderReceiver.class);
            alarmIntent.putExtras(intent.getExtras());
            alarmIntent.putExtra("alarmId", alarmId + 1); // Nouvel ID pour éviter le conflit

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    alarmId + 1,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent);
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent);
            }

            Log.d("Notification", "Alarme reprogrammée dans 10 minutes");
        }
    }
}