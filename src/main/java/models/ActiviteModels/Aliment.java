package models.ActiviteModels;

public class Aliment {
    private int id_aliment;
    private String nom_aliment;
    private int calories_pour_100g;
    private double proteines;
    private double glucides;
    private double lipides;

    // Constructeur par défaut
    public Aliment() {
        this.proteines = 0.0;
        this.glucides = 0.0;
        this.lipides = 0.0;
    }

    // Constructeur complet (avec ID, utile pour la récupération en BDD)
    public Aliment(int id_aliment, String nom_aliment, int calories_pour_100g, double proteines, double glucides, double lipides) {
        this.id_aliment = id_aliment;
        this.nom_aliment = nom_aliment;
        this.calories_pour_100g = calories_pour_100g;
        this.proteines = proteines;
        this.glucides = glucides;
        this.lipides = lipides;
    }

    // Constructeur sans ID (utile pour l'insertion en BDD)
    public Aliment(String nom_aliment, int calories_pour_100g, double proteines, double glucides, double lipides) {
        this.nom_aliment = nom_aliment;
        this.calories_pour_100g = calories_pour_100g;
        this.proteines = proteines;
        this.glucides = glucides;
        this.lipides = lipides;
    }

    // Ancien constructeur sans ID (macros par défaut à 0)
    public Aliment(String nom_aliment, int calories_pour_100g) {
        this(nom_aliment, calories_pour_100g, 0.0, 0.0, 0.0);
    }

    // Ancien constructeur avec ID (macros par défaut à 0)
    public Aliment(int id_aliment, String nom_aliment, int calories_pour_100g) {
        this(id_aliment, nom_aliment, calories_pour_100g, 0.0, 0.0, 0.0);
    }

    // --- GETTERS & SETTERS ---

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

    @Override
    public String toString() {
        return nom_aliment + " (" + calories_pour_100g + " kcal/100g)";
    }
}