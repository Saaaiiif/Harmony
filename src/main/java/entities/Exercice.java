package entities;

public class Exercice {
    private int id_exercice;
    private String nom_exercice;
    private String type_exercice;
    private String image_exercice;

    public Exercice() {}

    public Exercice(int id_exercice, String nom_exercice, String type_exercice, String image_exercice) {
        this.id_exercice = id_exercice;
        this.nom_exercice = nom_exercice;
        this.type_exercice = type_exercice;
        this.image_exercice = image_exercice;
    }

    public Exercice(String nom_exercice, String type_exercice, String image_exercice) {
        this.nom_exercice = nom_exercice;
        this.type_exercice = type_exercice;
        this.image_exercice = image_exercice;
    }

    public int getId_exercice() { return id_exercice; }
    public void setId_exercice(int id_exercice) { this.id_exercice = id_exercice; }
    public String getNom_exercice() { return nom_exercice; }
    public void setNom_exercice(String nom_exercice) { this.nom_exercice = nom_exercice; }
    public String getType_exercice() { return type_exercice; }
    public void setType_exercice(String type_exercice) { this.type_exercice = type_exercice; }
    public String getImage_exercice() { return image_exercice; }
    public void setImage_exercice(String image_exercice) { this.image_exercice = image_exercice; }

    @Override
    public String toString() {
        return "Exercice{id=" + id_exercice + ", nom='" + nom_exercice + "', type='" + type_exercice + "'}";
    }
}