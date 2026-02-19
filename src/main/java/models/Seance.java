package models;

import java.util.Date;

public class Seance {
    private int id;
    private String titre;
    private String description;
    private Date dateDebut;
    private Date dateFin;
    private int salleId;
    private boolean confirmee;
    private String typeSeance; // COURS, REUNION, etc.
    private int nombreParticipants;
    private Date dateCreation;

    public Seance() {
    }

    public Seance(int id, String titre, String description, Date dateDebut, Date dateFin,
                   int salleId, boolean confirmee, String typeSeance, int nombreParticipants, Date dateCreation) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.salleId = salleId;
        this.confirmee = confirmee;
        this.typeSeance = typeSeance;
        this.nombreParticipants = nombreParticipants;
        this.dateCreation = dateCreation;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(Date dateDebut) {
        this.dateDebut = dateDebut;
    }

    public Date getDateFin() {
        return dateFin;
    }

    public void setDateFin(Date dateFin) {
        this.dateFin = dateFin;
    }

    public int getSalleId() {
        return salleId;
    }

    public void setSalleId(int salleId) {
        this.salleId = salleId;
    }

    public boolean isConfirmee() {
        return confirmee;
    }

    public void setConfirmee(boolean confirmee) {
        this.confirmee = confirmee;
    }

    public String getTypeSeance() {
        return typeSeance;
    }

    public void setTypeSeance(String typeSeance) {
        this.typeSeance = typeSeance;
    }

    public int getNombreParticipants() {
        return nombreParticipants;
    }

    public void setNombreParticipants(int nombreParticipants) {
        this.nombreParticipants = nombreParticipants;
    }

    public Date getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Date dateCreation) {
        this.dateCreation = dateCreation;
    }

    @Override
    public String toString() {
        return "Seance{id=" + id + ", titre='" + titre + '\'' + ", confirmee=" + confirmee + ", salleId=" + salleId + '}';
    }
}
