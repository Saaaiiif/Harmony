package models.ActiviteModels;

public class Exercice {
    private int id_exercice;
    private String nom_exercice;
    private String type_exercice;
    private String video_exercice; // NOUVEAU

    public Exercice() { this.video_exercice = ""; }

    public Exercice(int id_exercice, String nom_exercice, String type_exercice, String video_exercice) {
        this.id_exercice = id_exercice;
        this.nom_exercice = nom_exercice;
        this.type_exercice = type_exercice;
        this.video_exercice = (video_exercice == null) ? "" : video_exercice;
    }

    public Exercice(String nom_exercice, String type_exercice, String video_exercice) {
        this.nom_exercice = nom_exercice;
        this.type_exercice = type_exercice;
        this.video_exercice = (video_exercice == null) ? "" : video_exercice;
    }

    public int getId_exercice() { return id_exercice; }
    public void setId_exercice(int id_exercice) { this.id_exercice = id_exercice; }
    public String getNom_exercice() { return nom_exercice; }
    public void setNom_exercice(String nom_exercice) { this.nom_exercice = nom_exercice; }
    public String getType_exercice() { return type_exercice; }
    public void setType_exercice(String type_exercice) { this.type_exercice = type_exercice; }
    public String getVideo_exercice() { return (video_exercice == null) ? "" : video_exercice; }
    public void setVideo_exercice(String video_exercice) { this.video_exercice = (video_exercice == null) ? "" : video_exercice; }
}