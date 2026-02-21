package models;

public class Aliment {
    private int id_aliment;
    private String nom_aliment;
    private int calories_pour_100g;
    private double proteines;
    private double glucides;
    private double lipides;

    // Constructeur vide (nécessaire pour Java)
    public Aliment() {}


    // Constructeur complet (avec ID)
    public Aliment(int id_aliment, String nom_aliment, int calories_pour_100g, double proteines, double glucides, double lipides) {
        this.id_aliment = id_aliment;
        this.nom_aliment = nom_aliment;
        this.calories_pour_100g = calories_pour_100g;
        this.proteines = proteines;
        this.glucides = glucides;
        this.lipides = lipides;
    }

    // Constructeur complet (sans ID pour l'insertion en BDD)
    public Aliment(String nom_aliment, int calories_pour_100g, double proteines, double glucides, double lipides) {
        this.nom_aliment = nom_aliment;
        this.calories_pour_100g = calories_pour_100g;
        this.proteines = proteines;
        this.glucides = glucides;
        this.lipides = lipides;
    }


    // Ancien constructeur sans ID (Met les macros à 0.0 par défaut pour éviter les crashs)
    public Aliment(String nom_aliment, int calories_pour_100g) {
        this.nom_aliment = nom_aliment;
        this.calories_pour_100g = calories_pour_100g;
        this.proteines = 0.0;
        this.glucides = 0.0;
        this.lipides = 0.0;
    }

    // Ancien constructeur avec ID (Met les macros à 0.0 par défaut)
    public Aliment(int id_aliment, String nom_aliment, int calories_pour_100g) {
        this.id_aliment = id_aliment;
        this.nom_aliment = nom_aliment;
        this.calories_pour_100g = calories_pour_100g;
        this.proteines = 0.0;
        this.glucides = 0.0;
        this.lipides = 0.0;
    }

    

    public int getId_aliment() { return id_aliment; }
    public void setId_aliment(int id_aliment) { this.id_aliment = id_aliment; }

    public String getNom_aliment() { return nom_aliment; }
    public void setNom_aliment(String nom_aliment) { this.nom_aliment = nom_aliment; }

    public int getCalories_pour_100g() { return calories_pour_100g; }
    public void setCalories_pour_100g(int calories_pour_100g) { this.calories_pour_100g = calories_pour_100g; }

    public double getProteines() { return proteines; }
    public void setProteines(double proteines) { this.proteines = proteines; }

    public double getGlucides() { return glucides; }
    public void setGlucides(double glucides) { this.glucides = glucides; }

    public double getLipides() { return lipides; }
    public void setLipides(double lipides) { this.lipides = lipides; }
}