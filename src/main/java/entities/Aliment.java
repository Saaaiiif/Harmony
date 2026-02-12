package entities;

public class Aliment {
    private int id_aliment;
    private String nom_aliment;
    private int calories_pour_100g;

    // Constructeur vide (nécessaire pour Java)
    public Aliment() {}

    // Constructeur complet (pour créer un objet avec toutes les infos)
    public Aliment(int id_aliment, String nom_aliment, int calories_pour_100g) {
        this.id_aliment = id_aliment;
        this.nom_aliment = nom_aliment;
        this.calories_pour_100g = calories_pour_100g;
    }

    // Constructeur sans ID (utile pour l'insertion, car l'ID est AUTO_INCREMENT)
    public Aliment(String nom_aliment, int calories_pour_100g) {
        this.nom_aliment = nom_aliment;
        this.calories_pour_100g = calories_pour_100g;
    }

    // Getters et Setters (pour que Java puisse lire et modifier les variables)
    public int getId_aliment() { return id_aliment; }
    public void setId_aliment(int id_aliment) { this.id_aliment = id_aliment; }

    public String getNom_aliment() { return nom_aliment; }
    public void setNom_aliment(String nom_aliment) { this.nom_aliment = nom_aliment; }

    public int getCalories_pour_100g() { return calories_pour_100g; }
    public void setCalories_pour_100g(int calories_pour_100g) { this.calories_pour_100g = calories_pour_100g; }

    // Pour afficher l'objet proprement dans la console
    @Override
    public String toString() {
        return "Aliment{" +
                "id=" + id_aliment +
                ", nom='" + nom_aliment + '\'' +
                ", calories=" + calories_pour_100g +
                '}';
    }
}