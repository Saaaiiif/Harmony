package models;

import java.util.Date;

public class Evenement {

    private int id;
    private String titre;
    private String description;
    private Date dateDebut;
    private Date dateFin;
    private String lieu;
    private int priorite;
    private boolean rappelActif;
    private TypeEvenement type;

    public Evenement() {
    }

    public Evenement(int id, String titre, String description,
                     Date dateDebut, Date dateFin,
                     String lieu, int priorite,
                     boolean rappelActif, TypeEvenement type) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.priorite = priorite;
        this.rappelActif = rappelActif;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getDescription() {
        return description;
    }

    public Date getDateDebut() {
        return dateDebut;
    }

    public Date getDateFin() {
        return dateFin;
    }

    public String getLieu() {
        return lieu;
    }

    public int getPriorite() {
        return priorite;
    }

    public boolean isRappelActif() {
        return rappelActif;
    }

    public TypeEvenement getType() {
        return type;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDateDebut(Date dateDebut) {
        this.dateDebut = dateDebut;
    }

    public void setDateFin(Date dateFin) {
        this.dateFin = dateFin;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public void setPriorite(int priorite) {
        this.priorite = priorite;
    }

    public void setRappelActif(boolean rappelActif) {
        this.rappelActif = rappelActif;
    }

    public void setType(TypeEvenement type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Evenement{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", lieu='" + lieu + '\'' +
                ", priorite=" + priorite +
                ", type=" + type +
                '}';
    }
}
