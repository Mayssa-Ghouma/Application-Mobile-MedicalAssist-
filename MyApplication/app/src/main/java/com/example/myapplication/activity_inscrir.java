package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class activity_inscrir extends AppCompatActivity {

    private EditText inputNom, inputPrenom, inputAge, inputTelephone, inputPassword, inputTaille, inputPoids, inputMail;
    private Spinner inputSexe, inputSang;
    private Button btnInscrire;
    private String selectedSexe, selectedSang;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inscrir);

        // Initialisation Firebase
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialisation des vues
        initViews();

        // Configuration des Spinners
        setupSpinners();

        // Gestion du clic sur le bouton "S'inscrire"
        btnInscrire.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void initViews() {
        inputNom = findViewById(R.id.input_nom);
        inputPrenom = findViewById(R.id.input_prenom);
        inputAge = findViewById(R.id.input_age);
        inputTelephone = findViewById(R.id.input_telephone);
        inputPassword = findViewById(R.id.input_password);
        inputTaille = findViewById(R.id.input_taille);
        inputPoids = findViewById(R.id.input_poids);
        inputMail = findViewById(R.id.input_mail);
        inputSexe = findViewById(R.id.input_sexe);
        inputSang = findViewById(R.id.input_sang);
        btnInscrire = findViewById(R.id.btn_inscrire);
    }

    private void setupSpinners() {
        // Spinner pour le sexe
        String[] sexeOptions = {"Homme", "Femme"};
        ArrayAdapter<String> adapterSexe = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sexeOptions);
        inputSexe.setAdapter(adapterSexe);
        inputSexe.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSexe = sexeOptions[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSexe = "Homme";
            }
        });

        // Spinner pour le groupe sanguin
        String[] sangOptions = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        ArrayAdapter<String> adapterSang = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sangOptions);
        inputSang.setAdapter(adapterSang);
        inputSang.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSang = sangOptions[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSang = "O+";
            }
        });
    }

    private void registerUser() {
        // Récupération des données
        String email = inputMail.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String nom = inputNom.getText().toString().trim();
        String prenom = inputPrenom.getText().toString().trim();
        String age = inputAge.getText().toString().trim();
        String taille = inputTaille.getText().toString().trim();
        String poids = inputPoids.getText().toString().trim();
        String telephone = inputTelephone.getText().toString().trim();

        // Validation des champs
        if (email.isEmpty() || password.isEmpty() || nom.isEmpty() || prenom.isEmpty() ||
                age.isEmpty() || taille.isEmpty() || poids.isEmpty() || telephone.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        // Création du compte Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            assert user != null;
                            saveUserToFirestore(user.getUid(), nom, prenom, age, selectedSexe, selectedSang, poids, taille, telephone, email);
                        } else {
                            Log.w("FirebaseAuth", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(activity_inscrir.this, "Erreur: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserToFirestore(String userId, String nom, String prenom, String age, String sexe,
                                     String groupeSanguin, String poids, String taille, String telephone, String email) {
        Utilisateur utilisateur = new Utilisateur(nom, prenom, age, sexe, groupeSanguin, poids, taille, telephone, email);

        firestore.collection("users").document(userId).set(utilisateur)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(activity_inscrir.this, "Inscription réussie !", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(activity_inscrir.this, interfac_login.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(activity_inscrir.this, "Erreur Firestore : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
