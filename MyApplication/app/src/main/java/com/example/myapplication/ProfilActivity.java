package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfilActivity extends AppCompatActivity {

    private TextView tvNom, tvPrenom, tvEmail, tvTelephone, tvAge, tvPoids, tvTaille, tvSexe, tvGroupeSanguin;
    private ImageButton btnEditNom, btnEditPrenom, btnEditEmail, btnEditTelephone,
            btnEditAge, btnEditPoids, btnEditTaille, btnEditSexe, btnEditGroupeSanguin;
    private Button btnSave;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profil2);

        // Lier les composants XML
        tvNom = findViewById(R.id.tvNom);
        tvPrenom = findViewById(R.id.tvPrenom);
        tvEmail = findViewById(R.id.tvEmail);
        tvTelephone = findViewById(R.id.tvTelephone);
        tvAge = findViewById(R.id.tvAge);
        tvPoids = findViewById(R.id.tvPoids);
        tvTaille = findViewById(R.id.tvTaille);
        tvSexe = findViewById(R.id.tvSexe);
        tvGroupeSanguin = findViewById(R.id.tvGroupeSanguin);

        btnEditNom = findViewById(R.id.btnEditNom);
        btnEditPrenom = findViewById(R.id.btnEditPrenom);
        btnEditEmail = findViewById(R.id.btnEditEmail);
        btnEditTelephone = findViewById(R.id.btnEditTelephone);
        btnEditAge = findViewById(R.id.btnEditAge);
        btnEditPoids = findViewById(R.id.btnEditPoids);
        btnEditTaille = findViewById(R.id.btnEditTaille);
        btnEditSexe = findViewById(R.id.btnEditSexe);
        btnEditGroupeSanguin = findViewById(R.id.btnEditGroupeSanguin);

        btnSave = findViewById(R.id.btnSave);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = user.getUid();
        db = FirebaseFirestore.getInstance();

        chargerProfil();

        // Configuration des listeners pour les boutons de modification
        btnEditNom.setOnClickListener(v -> activerEdition(tvNom, "nom"));
        btnEditPrenom.setOnClickListener(v -> activerEdition(tvPrenom, "prenom"));
        btnEditEmail.setOnClickListener(v -> activerEdition(tvEmail, "email"));
        btnEditTelephone.setOnClickListener(v -> activerEdition(tvTelephone, "telephone"));
        btnEditAge.setOnClickListener(v -> activerEdition(tvAge, "age"));
        btnEditPoids.setOnClickListener(v -> activerEdition(tvPoids, "poids"));
        btnEditTaille.setOnClickListener(v -> activerEdition(tvTaille, "taille"));
        btnEditSexe.setOnClickListener(v -> activerEdition(tvSexe, "sexe"));
        btnEditGroupeSanguin.setOnClickListener(v -> activerEdition(tvGroupeSanguin, "groupeSanguin"));

        btnSave.setOnClickListener(v -> enregistrerProfil());
    }

    private void chargerProfil() {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tvNom.setText(documentSnapshot.getString("nom"));
                        tvPrenom.setText(documentSnapshot.getString("prenom"));
                        tvEmail.setText(documentSnapshot.getString("email"));
                        tvTelephone.setText(documentSnapshot.getString("telephone"));
                        tvAge.setText(documentSnapshot.getString("age"));
                        tvPoids.setText(documentSnapshot.getString("poids"));
                        tvTaille.setText(documentSnapshot.getString("taille"));
                        tvSexe.setText(documentSnapshot.getString("sexe"));
                        tvGroupeSanguin.setText(documentSnapshot.getString("groupeSanguin"));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erreur de chargement", Toast.LENGTH_SHORT).show()
                );
    }

    private void activerEdition(TextView textView, String champ) {
        // Ici vous pouvez implémenter la logique pour transformer le TextView en EditText
        // et permettre la modification du champ spécifique
        // Par exemple, ouvrir un dialogue de modification ou changer la vue

        Toast.makeText(this, "Modification du champ: " + champ, Toast.LENGTH_SHORT).show();

        // Exemple basique de modification directe (à adapter selon vos besoins)

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifier " + champ);

        final EditText input = new EditText(this);
        input.setText(textView.getText());
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            textView.setText(input.getText());
            mettreAJourChamp(champ, input.getText().toString());
        });
        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.cancel());

        builder.show();

    }

    private void mettreAJourChamp(String champ, String nouvelleValeur) {
        Map<String, Object> data = new HashMap<>();
        data.put(champ, nouvelleValeur);

        db.collection("users")
                .document(userId)
                .update(data)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, champ + " mis à jour", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erreur mise à jour " + champ, Toast.LENGTH_SHORT).show()
                );
    }

    private void enregistrerProfil() {
        // Cette méthode peut être conservée si vous voulez garder un enregistrement global
        // ou supprimée si vous gérez uniquement des mises à jour par champ

        Map<String, Object> data = new HashMap<>();
        data.put("nom", tvNom.getText().toString().trim());
        data.put("prenom", tvPrenom.getText().toString().trim());
        data.put("email", tvEmail.getText().toString().trim());
        data.put("telephone", tvTelephone.getText().toString().trim());
        data.put("age", tvAge.getText().toString().trim());
        data.put("poids", tvPoids.getText().toString().trim());
        data.put("taille", tvTaille.getText().toString().trim());
        data.put("sexe", tvSexe.getText().toString().trim());
        data.put("groupeSanguin", tvGroupeSanguin.getText().toString().trim());

        db.collection("users")
                .document(userId)
                .update(data)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Profil mis à jour avec succès", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show()
                );
    }
}