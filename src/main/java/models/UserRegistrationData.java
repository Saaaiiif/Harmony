package models;

/**
 * Classe pour stocker temporairement les données d'inscription
 * entre l'étape 1 et l'étape 2 du formulaire d'inscription
 */
public class UserRegistrationData {
    
    // Données de l'étape 1
    private String nom;
    private String prenom;
    private String email;
    private String password;
    private String dateNaissance;

    //données de l'etape 2
    private Sexe sexe;
    private Double poids;
    private Integer taille;
    private NiveauActivitePhysique niveauActivite;
    private NiveauScolaire niveauScolaire;
    private String etablissement;
    
    // Constructeur
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
    }
    public UserRegistrationData(String nom, String prenom, String email, String password, String dateNaissance) {
        this(nom, prenom, email, password, dateNaissance, null, null, null, null, null, null);
    }

    // Getters
    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getEmail() {
        return email;
    }

    public Sexe getSexe() {
        return sexe;
    }

    public void setSexe(Sexe sexe) {
        this.sexe = sexe;
    }

    public Integer getTaille() {
        return taille;
    }

    public void setTaille(Integer taille) {
        this.taille = taille;
    }

    public Double getPoids() {
        return poids;
    }

    public void setPoids(Double poids) {
        this.poids = poids;
    }

    public NiveauActivitePhysique getNiveauActivite() {
        return niveauActivite;
    }

    public void setNiveauActivite(NiveauActivitePhysique niveauActivite) {
        this.niveauActivite = niveauActivite;
    }

    public NiveauScolaire getNiveauScolaire() {
        return niveauScolaire;
    }

    public void setNiveauScolaire(NiveauScolaire niveauScolaire) {
        this.niveauScolaire = niveauScolaire;
    }

    public String getEtablissement() {
        return etablissement;
    }

    public void setEtablissement(String etablissement) {
        this.etablissement = etablissement;
    }

    public String getPassword() {
        return password;
    }

    public String getDateNaissance() {
        return dateNaissance;
    }

    // Setters (au cas où)
    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDateNaissance(String dateNaissance) {
        this.dateNaissance = dateNaissance;
    }

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
