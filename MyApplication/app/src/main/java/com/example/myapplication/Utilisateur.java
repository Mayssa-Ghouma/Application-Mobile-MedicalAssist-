package com.example.myapplication;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Utilisateur {
    public String nom, prenom, age, sexe, groupeSanguin, poids, taille, telephone, email;

    // Constructeur vide OBLIGATOIRE
    public Utilisateur() {}

    public Utilisateur(String nom, String prenom, String age, String sexe,
                       String groupeSanguin, String poids, String taille,
                       String telephone, String email) {
        this.nom = nom;
        this.prenom = prenom;
        this.age = age;
        this.sexe = sexe;
        this.groupeSanguin = groupeSanguin;
        this.poids = poids;
        this.taille = taille;
        this.telephone = telephone;
        this.email = email;
    }
}