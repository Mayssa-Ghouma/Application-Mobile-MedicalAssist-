package com.example.myapplication;

public class RendezVous {
    private String id;
    private String date;
    private String nombreteur;
    private String time;
    private String specialite;
    private String adresse;
    private String notes;
    private long timestamp;

    // Constructeur vide requis pour Firestore
    public RendezVous() {}

    // Getters et setters
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getNombreteur() { return nombreteur; }
    public void setNombreteur(String nombreteur) { this.nombreteur = nombreteur; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getSpecialite() { return specialite; }
    public void setSpecialite(String specialite) { this.specialite = specialite; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}