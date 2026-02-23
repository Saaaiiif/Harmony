package models;

/**
 * Classe pour stocker temporairement les données d'inscription
 * entre les 3 étapes du formulaire d'inscription.
 */
public class UserRegistrationData {

    // Données de l'étape 1
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String dateNaissance;

    // Données de l'étape 2
    private Sexe sexe;
    private Double poids;
    private Integer taille;
    private NiveauActivitePhysique niveauActivite;
    private NiveauScolaire niveauScolaire;
    private String etablissement;
    private String imagePath; // Photo de profil (optionnel)

    // ✅ NOUVEAU : Données de l'étape 3 — visage pour la reconnaissance faciale
    private String facePath;  // Chemin vers l'image du visage capturé

    // Constructeur complet
    public UserRegistrationData(String nom, String prenom, String email, String password, String dateNaissance,
                                Sexe sexe, Double poids, Integer taille, NiveauActivitePhysique niveauActivite,
                                NiveauScolaire niveauScolaire, String etablissement) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.password = password;
        this.dateNaissance = dateNaissance;
        this.sexe = sexe;
        this.poids = poids;
        this.taille = taille;
        this.niveauActivite = niveauActivite;
        this.niveauScolaire = niveauScolaire;
        this.etablissement = etablissement;
        this.imagePath = null;
        this.facePath = null;
    }

    public UserRegistrationData(String nom, String prenom, String email, String password, String dateNaissance) {
        this(nom, prenom, email, password, dateNaissance, null, null, null, null, null, null);
    }

    // ========== GETTERS ==========
    public String getNom()            { return nom; }
    public String getPrenom()         { return prenom; }
    public String getEmail()          { return email; }
    public String getPassword()       { return password; }
    public String getDateNaissance()  { return dateNaissance; }
    public Sexe getSexe()             { return sexe; }
    public Double getPoids()          { return poids; }
    public Integer getTaille()        { return taille; }
    public NiveauActivitePhysique getNiveauActivite() { return niveauActivite; }
    public NiveauScolaire getNiveauScolaire()         { return niveauScolaire; }
    public String getEtablissement()  { return etablissement; }
    public String getImagePath()      { return imagePath; }
    public String getFacePath()       { return facePath; }

    // ========== SETTERS ==========
    public void setNom(String nom)                    { this.nom = nom; }
    public void setPrenom(String prenom)              { this.prenom = prenom; }
    public void setEmail(String email)                { this.email = email; }
    public void setPassword(String password)          { this.password = password; }
    public void setDateNaissance(String d)            { this.dateNaissance = d; }
    public void setSexe(Sexe sexe)                    { this.sexe = sexe; }
    public void setPoids(Double poids)                { this.poids = poids; }
    public void setTaille(Integer taille)             { this.taille = taille; }
    public void setNiveauActivite(NiveauActivitePhysique n) { this.niveauActivite = n; }
    public void setNiveauScolaire(NiveauScolaire n)   { this.niveauScolaire = n; }
    public void setEtablissement(String e)            { this.etablissement = e; }
    public void setImagePath(String imagePath)        { this.imagePath = imagePath; }
    public void setFacePath(String facePath)          { this.facePath = facePath; }

    @Override
    public String toString() {
        return "UserRegistrationData{" +
                "nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", dateNaissance='" + dateNaissance + '\'' +
                '}';
    }
}
