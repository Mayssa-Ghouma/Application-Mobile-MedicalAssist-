package com.example.myapplication;

import java.util.List;

public class Medicament {
    private String id;
    private String name;
    private String type;
    private String doseUnit;
    private String nombreFois;
    private String nombreDose;
    private List<String> horaires;
    private String imageUrl;
    // URL de l'image dans Firebase Storage
    private String imageBase64;
    // Constructeur vide requis pour Firestore
    public Medicament() {}

    // Getters et setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDoseUnit() { return doseUnit; }
    public void setDoseUnit(String doseUnit) { this.doseUnit = doseUnit; }
    public String getNombreFois() { return nombreFois; }
    public void setNombreFois(String nombreFois) { this.nombreFois = nombreFois; }
    public String getNombreDose() { return nombreDose; }
    public void setNombreDose(String nombreDose) { this.nombreDose = nombreDose; }
    public List<String> getHoraires() { return horaires; }
    public void setHoraires(List<String> horaires) { this.horaires = horaires; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }



    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }


}