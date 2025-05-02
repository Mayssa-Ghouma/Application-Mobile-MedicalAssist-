package com.example.myapplication;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class rendezVous_ajouter extends AppCompatActivity {

    private EditText etHoraire, etAdresse, etNomDocteur, etSpecialite, etNotes;
    private Button btnAjouterRdv;
    private String selectedDate;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String rendezVousId;
    private long existingTimestamp;

    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public static final String CHANNEL_ID = "rdv_reminder_channel";
    private static final String ACTION_NOTIFY = "com.example.myapplication.NOTIFY";
    private static final int NOTIFICATION_ID_DAY_BEFORE = 1000;
    private static final int NOTIFICATION_ID_HOUR_BEFORE = 1001;
    private static final long TWO_HOURS_IN_MS = 2 * 60 * 60 * 1000; // 2 heures en millisecondes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rendez_vous_ajouter);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        Intent intent = getIntent();
        selectedDate = intent.getStringExtra("selectedDate");
        rendezVousId = intent.getStringExtra("rendezVousId");

        initializeViews();
        setupButtonListener();

        if (rendezVousId != null) {
            loadRendezVousData();
        }
    }

    private void loadRendezVousData() {
        db.collection("users")
                .document(currentUser.getUid())
                .collection("rendezVous")
                .document(rendezVousId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        etHoraire.setText(documentSnapshot.getString("time"));
                        etAdresse.setText(documentSnapshot.getString("adresse"));
                        etNomDocteur.setText(documentSnapshot.getString("nombreteur"));
                        etSpecialite.setText(documentSnapshot.getString("specialite"));
                        etNotes.setText(documentSnapshot.getString("notes"));
                        existingTimestamp = documentSnapshot.getLong("timestamp");

                        btnAjouterRdv.setText("Modifier le rendez-vous");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur de chargement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void initializeViews() {
        etHoraire = findViewById(R.id.et_horaire);
        etAdresse = findViewById(R.id.et_adresse);
        etNomDocteur = findViewById(R.id.et_nom_docteur);
        etSpecialite = findViewById(R.id.et_specialite);
        etNotes = findViewById(R.id.et_notes);
        btnAjouterRdv = findViewById(R.id.btn_ajouter_rdv);
    }

    private void setupButtonListener() {
        btnAjouterRdv.setOnClickListener(v -> {
            String horaire = etHoraire.getText().toString().trim();
            String adresse = etAdresse.getText().toString().trim();
            String nomDocteur = etNomDocteur.getText().toString().trim();
            String specialite = etSpecialite.getText().toString().trim();
            String notes = etNotes.getText().toString().trim();

            if (horaire.isEmpty() || adresse.isEmpty() || nomDocteur.isEmpty() || specialite.isEmpty()) {
                Toast.makeText(this, "Tous les champs obligatoires doivent être remplis", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!horaire.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                Toast.makeText(this, "Format d'heure invalide. Utilisez HH:mm (ex: 14:30)", Toast.LENGTH_LONG).show();
                return;
            }

            checkRendezVousConflict(horaire, adresse, nomDocteur, specialite, notes);
        });
    }

    private void checkRendezVousConflict(String time, String adresse, String nomDocteur,
                                         String specialite, String notes) {
        try {
            Date dateTime = dateTimeFormat.parse(selectedDate + " " + time);
            if (dateTime == null) {
                Toast.makeText(this, "Erreur de format de date/heure", Toast.LENGTH_SHORT).show();
                return;
            }

            long newTimestamp = dateTime.getTime();
            long newStartTime = newTimestamp - TWO_HOURS_IN_MS;
            long newEndTime = newTimestamp + TWO_HOURS_IN_MS;

            // Vérifier les conflits avec d'autres rendez-vous
            db.collection("users")
                    .document(currentUser.getUid())
                    .collection("rendezVous")
                    .whereGreaterThanOrEqualTo("timestamp", newStartTime)
                    .whereLessThanOrEqualTo("timestamp", newEndTime)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            QuerySnapshot querySnapshot = task.getResult();
                            if (querySnapshot != null && !querySnapshot.isEmpty()) {
                                // Vérifier s'il s'agit d'une modification du même rendez-vous
                                if (rendezVousId != null) {
                                    boolean conflictWithOther = false;
                                    for (com.google.firebase.firestore.QueryDocumentSnapshot document : querySnapshot) {
                                        if (!document.getId().equals(rendezVousId)) {
                                            conflictWithOther = true;
                                            break;
                                        }
                                    }
                                    if (!conflictWithOther) {
                                        // Pas de conflit avec d'autres rendez-vous, seulement avec lui-même (modification)
                                        saveOrUpdateRendezVous(time, adresse, nomDocteur, specialite, notes);
                                        return;
                                    }
                                }
                                // Conflit détecté
                                Toast.makeText(this, "Impossible d'ajouter/modifier le rendez-vous. Il doit y avoir au moins 2 heures entre deux rendez-vous.", Toast.LENGTH_LONG).show();
                            } else {
                                // Pas de conflit
                                saveOrUpdateRendezVous(time, adresse, nomDocteur, specialite, notes);
                            }
                        } else {
                            Toast.makeText(this, "Erreur lors de la vérification des conflits: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (ParseException e) {
            Toast.makeText(this, "Format de date/heure invalide", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveOrUpdateRendezVous(String time, String adresse, String nomDocteur,
                                        String specialite, String notes) {
        try {
            Date dateTime = dateTimeFormat.parse(selectedDate + " " + time);
            if (dateTime == null) {
                Toast.makeText(this, "Erreur de format de date/heure", Toast.LENGTH_SHORT).show();
                return;
            }

            long timestamp = dateTime.getTime();
            Map<String, Object> rdv = new HashMap<>();
            rdv.put("date", selectedDate);
            rdv.put("time", time);
            rdv.put("timestamp", timestamp);
            rdv.put("adresse", adresse);
            rdv.put("nombreteur", nomDocteur);
            rdv.put("specialite", specialite);
            rdv.put("notes", notes);
            rdv.put("userId", currentUser.getUid());

            if (rendezVousId != null) {
                // Mode modification - annuler les anciennes notifications
                cancelExistingNotifications();

                db.collection("users")
                        .document(currentUser.getUid())
                        .collection("rendezVous")
                        .document(rendezVousId)
                        .set(rdv)
                        .addOnSuccessListener(aVoid -> {
                            // Recréer les notifications avec les nouvelles données
                            scheduleRdvNotifications(selectedDate, time, timestamp, nomDocteur, specialite);
                            Toast.makeText(this, "Rendez-vous modifié avec succès", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Erreur de modification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                // Mode création
                db.collection("users")
                        .document(currentUser.getUid())
                        .collection("rendezVous")
                        .add(rdv)
                        .addOnSuccessListener(documentReference -> {
                            scheduleRdvNotifications(selectedDate, time, timestamp, nomDocteur, specialite);
                            Toast.makeText(this, "Rendez-vous ajouté avec succès", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Erreur d'ajout: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        } catch (ParseException e) {
            Toast.makeText(this, "Format de date/heure invalide", Toast.LENGTH_SHORT).show();
        }
    }

    // Les autres méthodes restent inchangées...
    private void cancelExistingNotifications() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(this, RdvReminderReceiver.class);
        PendingIntent dayBeforeIntent = PendingIntent.getBroadcast(
                this,
                NOTIFICATION_ID_DAY_BEFORE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        PendingIntent hourBeforeIntent = PendingIntent.getBroadcast(
                this,
                NOTIFICATION_ID_HOUR_BEFORE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(dayBeforeIntent);
            alarmManager.cancel(hourBeforeIntent);
        }
    }

    private void scheduleRdvNotifications(String date, String time, long timestamp,
                                          String nomDocteur, String specialite) {
        createNotificationChannel();

        // Vérifier que le rendez-vous est dans le futur
        if (timestamp <= System.currentTimeMillis()) {
            return; // Ne pas planifier de notifications pour les rendez-vous passés
        }

        // Notification 1 jour avant
        long dayBeforeTime = timestamp - (24 * 60 * 60 * 1000);
        if (dayBeforeTime > System.currentTimeMillis()) {
            scheduleNotification(
                    dayBeforeTime,
                    NOTIFICATION_ID_DAY_BEFORE,
                    "Rappel: Rendez-vous demain",
                    String.format("Vous avez un rendez-vous avec Dr. %s (%s) le %s à %s",
                            nomDocteur, specialite, date, time)
            );
        }

        // Notification 1 heure avant
        long hourBeforeTime = timestamp - (60 * 60 * 1000);
        if (hourBeforeTime > System.currentTimeMillis()) {
            scheduleNotification(
                    hourBeforeTime,
                    NOTIFICATION_ID_HOUR_BEFORE,
                    "Rappel: Rendez-vous bientôt",
                    String.format("Rendez-vous avec Dr. %s à %s", nomDocteur, time)
            );
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Rappels de rendez-vous",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications pour les rappels de rendez-vous médicaux");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void scheduleNotification(long triggerTime, int notificationId,
                                      String title, String message) {
        Intent intent = new Intent(this, RdvReminderReceiver.class);
        intent.setAction(ACTION_NOTIFY + notificationId); // Action unique pour chaque notification
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        intent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            }
        }
    }
}