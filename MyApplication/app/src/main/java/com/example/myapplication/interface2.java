package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class interface2 extends AppCompatActivity {
    private ImageView play;
    private ImageView play1;
    private ImageView play2;
    private ImageView play3;
    private ImageView play4;

    private ImageView play5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interface2);

        play = findViewById(R.id.medicaments);

        play.setOnClickListener(view -> {
            Intent intent = new Intent(interface2.this, ajoutmed.class);
            startActivity(intent);

        });

        play1= findViewById(R.id.urgence);

        play1.setOnClickListener(view -> {
            Intent intent = new Intent(interface2.this, ajout_contact.class);
            startActivity(intent);

        });

        play2= findViewById(R.id.rendezVous);

        play2.setOnClickListener(view -> {
            Intent intent = new Intent(interface2.this, ajout_rendezVous.class);
            startActivity(intent);
        });

        play3= findViewById(R.id.ordonance);

        play3.setOnClickListener(view -> {
            Intent intent = new Intent(interface2.this, ajout_ordonance.class);
            startActivity(intent);
        });

        play4= findViewById(R.id.nav_log_out);

        play4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Déconnexion et retour à la page login
                logoutUser();
            }
        });

        play5= findViewById(R.id.profil);

        play5.setOnClickListener(view -> {
            Intent intent = new Intent(interface2.this, ProfilActivity.class);
            startActivity(intent);
        });


    }

    private void logoutUser() {
        // Ici vous pouvez ajouter toute logique de nettoyage nécessaire
        // Par exemple, effacer les données de session, etc.

        // Redirection vers l'activité de login
        Intent intent = new Intent(interface2.this, interfac_login.class);

        // Effacer la pile d'activités pour empêcher de revenir en arrière avec le bouton back
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        startActivity(intent);
        finish(); // Terminer l'activité actuelle
    }
}