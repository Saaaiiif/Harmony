package models.ActiviteModels;

import java.sql.Timestamp;

public class Activite {
    private int id_activite;
    private int user_id; // CORRIGÉ : Plus de valeur par défaut
    private Timestamp date_activite;
    private int id_exercice;
    private int duree_minutes;
    private int calories_brulees;
    private int nb_series;
    private int nb_repetitions;
    private float poids;
    private String notes;

    public Activite() {}

    // Constructeur avec ID (pour la récupération depuis la BDD)
    public Activite(int id_activite, int user_id, Timestamp date_activite, int id_exercice, int duree_minutes, int calories_brulees, int nb_series, int nb_repetitions, float poids, String notes) {
        this.id_activite = id_activite;
        this.user_id = user_id;
        this.date_activite = date_activite;
        this.id_exercice = id_exercice;
        this.duree_minutes = duree_minutes;
        this.calories_brulees = calories_brulees;
        this.nb_series = nb_series;
        this.nb_repetitions = nb_repetitions;
        this.poids = poids;
        this.notes = notes;
    }

    // Constructeur sans ID (pour l'ajout d'une nouvelle activité)
    public Activite(int user_id, Timestamp date_activite, int id_exercice, int duree_minutes, int calories_brulees, int nb_series, int nb_repetitions, float poids, String notes) {
        this.user_id = user_id;
        this.date_activite = date_activite;
        this.id_exercice = id_exercice;
        this.duree_minutes = duree_minutes;
        this.calories_brulees = calories_brulees;
        this.nb_series = nb_series;
        this.nb_repetitions = nb_repetitions;
        this.poids = poids;
        this.notes = notes;
    }

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public int getId_activite() { return id_activite; }
    public void setId_activite(int id_activite) { this.id_activite = id_activite; }
    public Timestamp getDate_activite() { return date_activite; }
    public void setDate_activite(Timestamp date_activite) { this.date_activite = date_activite; }
    public int getId_exercice() { return id_exercice; }
    public void setId_exercice(int id_exercice) { this.id_exercice = id_exercice; }
    public int getDuree_minutes() { return duree_minutes; }
    public void setDuree_minutes(int duree_minutes) { this.duree_minutes = duree_minutes; }
    public int getCalories_brulees() { return calories_brulees; }
    public void setCalories_brulees(int calories_brulees) { this.calories_brulees = calories_brulees; }
    public int getNb_series() { return nb_series; }
    public void setNb_series(int nb_series) { this.nb_series = nb_series; }
    public int getNb_repetitions() { return nb_repetitions; }
    public void setNb_repetitions(int nb_repetitions) { this.nb_repetitions = nb_repetitions; }
    public float getPoids() { return poids; }
    public void setPoids(float poids) { this.poids = poids; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}