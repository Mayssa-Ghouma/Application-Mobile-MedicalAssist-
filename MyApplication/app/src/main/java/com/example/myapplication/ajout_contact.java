package com.example.myapplication;

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

public class ajout_contact extends AppCompatActivity {

    private ContactAdapter adapter;
    private List<Contact> contactList;
    private ListView listView;  // On le rend global pour un éventuel refresh

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajout_contact);

        // Initialisation des vues
        listView = findViewById(R.id.lvMedications);
        Button addButton = findViewById(R.id.btnAjouter);

        // Initialisation de la liste et de l'adapter
        contactList = new ArrayList<>();
        adapter = new ContactAdapter(this, contactList);
        listView.setAdapter(adapter);

        // Ouvrir l'activité pour ajouter un contact
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(ajout_contact.this, contact_ajouter.class);
            startActivity(intent);
        });

        // Clic sur un contact pour voir les détails
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Contact selectedContact = contactList.get(position);

            Intent intent = new Intent(ajout_contact.this, ContactDetailActivity.class);
            intent.putExtra("contactId", selectedContact.getId());
            intent.putExtra("nom", selectedContact.getNom());
            intent.putExtra("telephone", selectedContact.getPhoneNumber());
            intent.putExtra("imageBase64", selectedContact.getImageBase64());
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recharger les contacts à chaque retour sur l'activité
        loadContacts();
    }

    private void loadContacts() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Utilisateur non connecté", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .document(user.getUid())
                .collection("contacts")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        contactList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Contact contact = document.toObject(Contact.class);
                            contact.setId(document.getId()); // Important pour modifier/supprimer
                            contactList.add(contact);
                        }
                        adapter.notifyDataSetChanged(); // Rafraîchir l'affichage
                    } else {
                        Toast.makeText(this, "Erreur de chargement des contacts", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
