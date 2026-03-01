package models.RessourceModels;

import java.util.Date;

public class Tache {

    private int id;
    private String nom;
    private Date deadline;
    private String notes;
    private StatutTache statut;

    public Tache() {
    }

    public Tache(int id, String nom, Date deadline, String notes, StatutTache statut) {
        this.id = id;
        this.nom = nom;
        this.deadline = deadline;
        this.notes = notes;
        this.statut = statut;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public StatutTache getStatut() {
        return statut;
    }

    public void setStatut(StatutTache statut) {
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "Tache{id=" + id + ", nom='" + nom + '\'' + ", statut=" + statut + '}';
    }
}
