package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ContactDetailActivity extends AppCompatActivity {

    private String contactId;
    private String telephone;
    private String nom;
    private String imageBase64;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_detail);

        // Récupérer les données passées
        contactId = getIntent().getStringExtra("contactId");
        nom = getIntent().getStringExtra("nom");
        telephone = getIntent().getStringExtra("telephone");
        imageBase64 = getIntent().getStringExtra("imageBase64");

        // Lier les vues
        ImageView imgContact = findViewById(R.id.imgContact);
        TextView tvNom = findViewById(R.id.tvNom);
        TextView tvTelephone = findViewById(R.id.tvTelephone);
        Button btnModifier = findViewById(R.id.btnModifier);
        Button btnSupprimer = findViewById(R.id.btnSupprimer);
        Button btnAppeler = findViewById(R.id.btnAppeler);

        // Définir les données
        tvNom.setText(nom);
        tvTelephone.setText(telephone);
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            imgContact.setImageBitmap(decodedBitmap);
        }

        // Action Appeler
        btnAppeler.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + telephone));
            startActivity(intent);
        });

        // Action Supprimer
        btnSupprimer.setOnClickListener(view -> {
            new AlertDialog.Builder(ContactDetailActivity.this)
                    .setTitle("Supprimer ce contact ?")
                    .setMessage("Êtes-vous sûr de vouloir supprimer ce contact ?")
                    .setPositiveButton("Oui", (dialog, which) -> supprimerContact())
                    .setNegativeButton("Annuler", null)
                    .show();
        });

        // Action Modifier
        btnModifier.setOnClickListener(view -> {
            Intent intent = new Intent(ContactDetailActivity.this, contact_ajouter.class);
            intent.putExtra("contactId", contactId); // à gérer dans contact_ajouter si tu veux la modification
            startActivity(intent);
        });
    }

    private void supprimerContact() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("contacts")
                .document(contactId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Contact supprimé", Toast.LENGTH_SHORT).show();
                    finish(); // revenir à la liste
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                });
    }
}
