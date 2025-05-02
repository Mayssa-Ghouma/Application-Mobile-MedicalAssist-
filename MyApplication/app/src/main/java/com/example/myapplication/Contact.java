package com.example.myapplication;

public class Contact {
    public String id;
    public String nom;
    public String telephone;
    public String imageUrl;

    public Contact() {
        // Requis pour Firebase
    }

    public Contact(String id, String nom, String telephone, String imageUrl) {
        this.id = id;
        this.nom = nom;
        this.telephone = telephone;
        this.imageUrl = imageUrl;
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

    public String getPhoneNumber() {
        return telephone;
    }

    public void setPhoneNumber(String telephone) {
        this.telephone = telephone;
    }

    // Getter et Setter pour imageBase64
    public String getImageBase64() {
        return imageUrl;
    }

    public void setImageBase64(String imageBase64) {
        this.imageUrl = imageBase64;
    }

    @Override
    public String toString() {
        return "Contact{" +
                "nom='" + nom + '\'' + "telephone='" + telephone + '\'' +
                ", imageBase64='" + imageUrl + '\'' +
                '}';
    }
}

