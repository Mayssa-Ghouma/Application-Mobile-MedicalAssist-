package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

public class MedicationReminderReceiver extends BroadcastReceiver {
    private static final int NOTIFICATION_ID = 1001;
    private static final int REMIND_ME_LATER_ID = 1002;
    private static final long REMIND_LATER_DELAY = 10 * 60 * 1000; // 10 minutes en millisecondes

    @Override
    public void onReceive(Context context, Intent intent) {
        String medName = intent.getStringExtra("medicationName");
        String nombreDose = intent.getStringExtra("nombreDose");
        int horaireIndex = intent.getIntExtra("horaireIndex", 0) + 1;
        int alarmId = intent.getIntExtra("alarmId", NOTIFICATION_ID);

        // Intent pour arrêter l'alarme
        Intent stopIntent = new Intent(context, NotificationActionReceiver.class);
        stopIntent.setAction("STOP_ALARM");
        stopIntent.putExtra("alarmId", alarmId);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Intent pour rappeler plus tard
        Intent laterIntent = new Intent(context, NotificationActionReceiver.class);
        laterIntent.setAction("REMIND_LATER");
        laterIntent.putExtras(intent.getExtras());
        laterIntent.putExtra("alarmId", alarmId);
        PendingIntent laterPendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId + REMIND_ME_LATER_ID,
                laterIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Créer la notification persistante
        Notification notification = new NotificationCompat.Builder(context, medicament_ajouter.CHANNEL_ID)
                .setContentTitle("⏰ Rappel Médicament (" + horaireIndex + "ème prise)")
                .setContentText("Prenez " + nombreDose + " de " + medName)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true) // Notification persistante
                .setSound(android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                .addAction(R.drawable.ic_stop, "Arrêter", stopPendingIntent)
                .addAction(R.drawable.ic_later, "Plus tard", laterPendingIntent)
                .build();

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(alarmId, notification);
    }
}