package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ajout_ordonance extends AppCompatActivity {

    private OrdonnanceAdapter adapter;
    private List<Ordonnance> ordonnanceList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajout_ordonance);

        // Initialize UI components
        ListView listView = findViewById(R.id.lvMedications);
        Button addButton = findViewById(R.id.btnAjouter);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize the list and adapter
        ordonnanceList = new ArrayList<>();
        adapter = new OrdonnanceAdapter(this, ordonnanceList);
        listView.setAdapter(adapter);

        // Button click to open add medication activity
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(ajout_ordonance.this, ordonance_ajouter.class);
            startActivity(intent);
        });

        // Handle item click
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Ordonnance selectedOrdonnance = ordonnanceList.get(position);
            showOptionsDialog(selectedOrdonnance);
        });

        // Load data from Firestore
        loadOrdonnances();
    }

    private void showOptionsDialog(Ordonnance ordonnance) {
        CharSequence[] options = new CharSequence[]{"Modifier", "Supprimer", "Annuler"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Options");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Modifier
                Intent intent = new Intent(ajout_ordonance.this, ordonance_ajouter.class);
                intent.putExtra("ordonnance_id", ordonnance.getId());
                intent.putExtra("ordonnance_name", ordonnance.getNom());
                intent.putExtra("ordonnance_image", ordonnance.getImageBase64());
                startActivity(intent);
            } else if (which == 1) {
                // Supprimer
                deleteOrdonnance(ordonnance.getId());
            }
        });
        builder.show();
    }

    private void deleteOrdonnance(String ordonnanceId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .collection("ordonnances")
                .document(ordonnanceId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Ordonnance supprimée", Toast.LENGTH_SHORT).show();
                    loadOrdonnances();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Erreur de suppression", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadOrdonnances() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .document(user.getUid())
                .collection("ordonnances")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ordonnanceList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Ordonnance ordonnance = document.toObject(Ordonnance.class);
                            ordonnance.setId(document.getId());
                            ordonnanceList.add(ordonnance);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Erreur de chargement", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}