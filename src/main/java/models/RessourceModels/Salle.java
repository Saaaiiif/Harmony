package models.RessourceModels;

import java.util.Date;

public class Salle {
    private int id;
    private String nom;
    private int capacite;
    private String equipements;
    private boolean disponible;
    private String description;
    private Date dateCreation;

    public Salle() {
    }

    public Salle(int id, String nom, int capacite, String equipements, boolean disponible, String description, Date dateCreation) {
        this.id = id;
        this.nom = nom;
        this.capacite = capacite;
        this.equipements = equipements;
        this.disponible = disponible;
        this.description = description;
        this.dateCreation = dateCreation;
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

    public int getCapacite() {
        return capacite;
    }

    public void setCapacite(int capacite) {
        this.capacite = capacite;
    }

    public String getEquipements() {
        return equipements;
    }

    public void setEquipements(String equipements) {
        this.equipements = equipements;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Date dateCreation) {
        this.dateCreation = dateCreation;
    }

    @Override
    public String toString() {
        return "Salle{id=" + id + ", nom='" + nom + '\'' + ", capacite=" + capacite + ", disponible=" + disponible + '}';
    }
}
