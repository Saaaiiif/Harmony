package models;
import java.sql.Timestamp;

public class Consommation {
    private int id_consommation;
    private int user_id = 1; // NOUVEAU
    private Timestamp date_consommation;
    private String type_repas;
    private int id_aliment;
    private int quantite_eau_ml;
    private int poids_grammes;

    public Consommation() {}

    public Consommation(int id_consommation, Timestamp date_consommation, String type_repas, int id_aliment, int quantite_eau_ml, int poids_grammes) {
        this.id_consommation = id_consommation;
        this.date_consommation = date_consommation;
        this.type_repas = type_repas;
        this.id_aliment = id_aliment;
        this.quantite_eau_ml = quantite_eau_ml;
        this.poids_grammes = poids_grammes;
    }

    public Consommation(Timestamp date_consommation, String type_repas, int id_aliment, int quantite_eau_ml, int poids_grammes) {
        this.date_consommation = date_consommation;
        this.type_repas = type_repas;
        this.id_aliment = id_aliment;
        this.quantite_eau_ml = quantite_eau_ml;
        this.poids_grammes = poids_grammes;
    }

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public int getId_consommation() { return id_consommation; }
    public void setId_consommation(int id_consommation) { this.id_consommation = id_consommation; }
    public Timestamp getDate_consommation() { return date_consommation; }
    public void setDate_consommation(Timestamp date_consommation) { this.date_consommation = date_consommation; }
    public String getType_repas() { return type_repas; }
    public void setType_repas(String type_repas) { this.type_repas = type_repas; }
    public int getId_aliment() { return id_aliment; }
    public void setId_aliment(int id_aliment) { this.id_aliment = id_aliment; }
    public int getQuantite_eau_ml() { return quantite_eau_ml; }
    public void setQuantite_eau_ml(int quantite_eau_ml) { this.quantite_eau_ml = quantite_eau_ml; }
    public int getPoids_grammes() { return poids_grammes; }
    public void setPoids_grammes(int poids_grammes) { this.poids_grammes = poids_grammes; }
}