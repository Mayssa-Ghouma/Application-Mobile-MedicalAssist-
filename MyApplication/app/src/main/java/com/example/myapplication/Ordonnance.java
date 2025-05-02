package com.example.myapplication;
public class Ordonnance {

    private String nom;
    private String id;
    private String imageBase64;

    // Constructeur vide nécessaire pour Firestore
    public Ordonnance() {
    }

    // Constructeur avec les paramètres
    public Ordonnance(String nom, String imageBase64) {
        this.nom = nom;
        this.imageBase64 = imageBase64;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    // Getter et Setter pour nom
    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    // Getter et Setter pour imageBase64
    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    @Override
    public String toString() {
        return "Ordonnance{" +
                "nom='" + nom + '\'' +
                ", imageBase64='" + imageBase64 + '\'' +
                '}';
    }
}