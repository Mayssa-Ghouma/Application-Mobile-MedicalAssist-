package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class ajout_rendezVous extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private ListView listViewRendezVous;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    //private ArrayAdapter<String> adapter;
    //private List<String> rendezVousList;
    private RendezVousAdapter adapter;
    private List<RendezVous> rendezVousList;
    private TextView tvMessageVide;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajout_rendez_vous);

        // Initialisation UNIQUE des vues
        calendarView = findViewById(R.id.calendarView);
        listViewRendezVous = findViewById(R.id.list_rendez_vous);
        tvMessageVide = findViewById(R.id.tvMessageVide);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Initialisation UNIQUE de la liste et de l'adaptateur
        rendezVousList = new ArrayList<>();
        adapter = new RendezVousAdapter(this, rendezVousList);
        listViewRendezVous.setAdapter(adapter);

        // Initialisez le click listener pour la liste
        listViewRendezVous.setOnItemClickListener((parent, view, position, id) -> {
            RendezVous selectedRdv = rendezVousList.get(position);
            showActionDialog(selectedRdv);
        });


        // Configurer le calendrier
        setupCalendar();

        // Charger les rendez-vous existants
        chargerEtMarquerRendezVous();
    }

    private void setupCalendar() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // Bloquer les dates passées
        calendarView.state().edit()
                .setMinimumDate(CalendarDay.from(today))
                .commit();

        // Gestion sélection date
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (date.isBefore(CalendarDay.from(today))) {
                widget.clearSelection();
                Toast.makeText(this, "Sélection impossible: date passée", Toast.LENGTH_SHORT).show();
            } else {
                String dateStr = String.format(Locale.getDefault(),
                        "%d/%d/%d", date.getDay(), date.getMonth() + 1, date.getYear());
                ouvrirAjoutRendezVous(dateStr);
            }
        });
    }

    private void ouvrirAjoutRendezVous(String date) {
        Intent intent = new Intent(this, rendezVous_ajouter.class);
        intent.putExtra("selectedDate", date);
        startActivity(intent);
    }

    private void chargerEtMarquerRendezVous() {
        String userId = auth.getCurrentUser().getUid();
        if (userId == null) return;

        // Obtenir la date d'aujourd'hui à minuit
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long todayTimestamp = today.getTimeInMillis();

        db.collection("users").document(userId).collection("rendezVous")
                .whereGreaterThanOrEqualTo("timestamp", todayTimestamp)
                .orderBy("timestamp")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        rendezVousList.clear();
                        HashSet<CalendarDay> datesRendezVous = new HashSet<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                RendezVous rdv = document.toObject(RendezVous.class);
                                rdv.setId(document.getId());

                                // Vérification supplémentaire côté client
                                if (rdv.getTimestamp() >= todayTimestamp) {
                                    rendezVousList.add(rdv);

                                    // Conversion de la date pour le marquage calendrier
                                    String[] dateParts = rdv.getDate().split("/");
                                    int day = Integer.parseInt(dateParts[0]);
                                    int month = Integer.parseInt(dateParts[1]) - 1; // -1 car janvier=0
                                    int year = Integer.parseInt(dateParts[2]);
                                    datesRendezVous.add(CalendarDay.from(year, month, day));
                                }
                            } catch (Exception e) {
                                Log.e("Firestore", "Erreur parsing rendez-vous", e);
                            }
                        }

                        // Mise à jour de l'UI
                        adapter.notifyDataSetChanged();
                        calendarView.removeDecorators(); // Nettoyer les anciens marqueurs
                        calendarView.addDecorator(new EventDecorator(datesRendezVous));
                        updateEmptyState();

                    } else {
                        Toast.makeText(this, "Erreur: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showActionDialog(RendezVous rdv) {
        new AlertDialog.Builder(this)
                .setTitle("Rendez-vous du " + rdv.getDate())
                .setMessage("Docteur: " + rdv.getNombreteur() + "\n" +
                        "Spécialité: " + rdv.getSpecialite() + "\n" +
                        "Heure: " + rdv.getTime())
                .setPositiveButton("Modifier", (dialog, which) -> {
                    Intent intent = new Intent(this, rendezVous_ajouter.class);
                    intent.putExtra("rendezVousId", rdv.getId()); // Envoyer l'ID
                    startActivity(intent);
                })
                .setNegativeButton("Supprimer", (dialog, which) -> {
                    showDeleteConfirmation(rdv);
                })
                .setNeutralButton("Annuler", null)
                .show();
    }

    private void showDeleteConfirmation(RendezVous rdv) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmer la suppression")
                .setMessage("Voulez-vous vraiment supprimer ce rendez-vous?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    deleteRendezVous(rdv);
                })
                .setNegativeButton("Non", null)
                .show();
    }

    private void deleteRendezVous(RendezVous rdv) {
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("rendezVous")
                .document(rdv.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Rendez-vous supprimé", Toast.LENGTH_SHORT).show();
                    chargerEtMarquerRendezVous();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur de suppression", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEmptyState() {
        if (tvMessageVide != null) {
            tvMessageVide.setVisibility(rendezVousList.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        chargerEtMarquerRendezVous();
    }

    // Décorateur pour les dates avec rendez-vous
    private class EventDecorator implements DayViewDecorator {
        private final HashSet<CalendarDay> dates;

        public EventDecorator(HashSet<CalendarDay> dates) {
            this.dates = dates;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new DotSpan(5, getResources().getColor(R.color.purple_700)));
        }
    }
}