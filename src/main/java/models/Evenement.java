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
    /** Salle demandée (quand lieu = esprit) */
    private Integer salleId;
    /** Statut de la demande de salle : null si pas de demande, sinon EN_ATTENTE/REFUSE/CONFIRME */
    private StatutDemandeSalle statutDemandeSalle;

    public Evenement() {
    }

    public Evenement(int id, String titre, String description,
                     Date dateDebut, Date dateFin,
                     String lieu, int priorite,
                     boolean rappelActif, TypeEvenement type) {
        this(id, titre, description, dateDebut, dateFin, lieu, priorite, rappelActif, type, null, null);
    }

    public Evenement(int id, String titre, String description,
                     Date dateDebut, Date dateFin,
                     String lieu, int priorite,
                     boolean rappelActif, TypeEvenement type,
                     Integer salleId, StatutDemandeSalle statutDemandeSalle) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.priorite = priorite;
        this.rappelActif = rappelActif;
        this.type = type;
        this.salleId = salleId;
        this.statutDemandeSalle = statutDemandeSalle;
    }

    public int getId() { return id; }
    public String getTitre() { return titre; }
    public String getDescription() { return description; }
    public Date getDateDebut() { return dateDebut; }
    public Date getDateFin() { return dateFin; }
    public String getLieu() { return lieu; }
    public int getPriorite() { return priorite; }
    public boolean isRappelActif() { return rappelActif; }
    public TypeEvenement getType() { return type; }
    public Integer getSalleId() { return salleId; }
    public StatutDemandeSalle getStatutDemandeSalle() { return statutDemandeSalle; }

    public void setId(int id) { this.id = id; }
    public void setTitre(String titre) { this.titre = titre; }
    public void setDescription(String description) { this.description = description; }
    public void setDateDebut(Date dateDebut) { this.dateDebut = dateDebut; }
    public void setDateFin(Date dateFin) { this.dateFin = dateFin; }
    public void setLieu(String lieu) { this.lieu = lieu; }
    public void setPriorite(int priorite) { this.priorite = priorite; }
    public void setRappelActif(boolean rappelActif) { this.rappelActif = rappelActif; }
    public void setType(TypeEvenement type) { this.type = type; }
    public void setSalleId(Integer salleId) { this.salleId = salleId; }
    public void setStatutDemandeSalle(StatutDemandeSalle statutDemandeSalle) { this.statutDemandeSalle = statutDemandeSalle; }

    /** Indique si cet événement demande une salle (lieu esprit + salle sélectionnée) */
    public boolean isDemandeSalle() {
        return salleId != null && "esprit".equalsIgnoreCase(lieu != null ? lieu.trim() : "");
    }

    @Override
    public String toString() {
        return "Evenement{" + "id=" + id + ", titre='" + titre + '\'' + ", lieu='" + lieu + '\'' + ", priorite=" + priorite + ", type=" + type + '}';
    }
}
