package models;

public class user {

    private int user_id;
    private String user_nom, user_prenom, user_email, user_password;
    private String user_date_de_naissance, date_inscription;
    private Role type_utilisateur;

    // SANTÉ
    private Sexe user_sexe;
    private Double user_poids;
    private Integer user_taille;
    private NiveauActivitePhysique user_niveau_activite_physique;

    // SCOLAIRE
    private NiveauScolaire user_niveau_scolaire;
    private String user_etablissement_scolaire;

    // IMAGE DE PROFIL
    private String user_image_path;

    // ✅ NOUVEAU : IMAGE DU VISAGE (reconnaissance faciale)
    private String face_image_path;

    // ARCHIVAGE : true = actif, false = archivé
    private boolean is_active = true;

    // ========== CONSTRUCTEURS ==========

    public user() {}

    public user(String nom, String prenom, String email, String password,
                String dateNaissance, String dateInscription, Role role) {
        this.user_nom = nom;
        this.user_prenom = prenom;
        this.user_email = email;
        this.user_password = password;
        this.user_date_de_naissance = dateNaissance;
        this.date_inscription = dateInscription;
        this.type_utilisateur = role;
        this.user_id = 0;
        this.is_active = true;
    }

    public user(String nom, String prenom, String email, String password,
                String dateNaissance, String dateInscription, Role role,
                Sexe sexe, Double poids, Integer taille, NiveauActivitePhysique niveauActivite,
                NiveauScolaire niveauScolaire, String etablissement) {
        this(nom, prenom, email, password, dateNaissance, dateInscription, role);
        this.user_sexe = sexe;
        this.user_poids = poids;
        this.user_taille = taille;
        this.user_niveau_activite_physique = niveauActivite;
        this.user_niveau_scolaire = niveauScolaire;
        this.user_etablissement_scolaire = etablissement;
    }

    // ========== GETTERS ET SETTERS ==========

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public String getUser_nom() { return user_nom; }
    public void setUser_nom(String user_nom) { this.user_nom = user_nom; }

    public String getUser_prenom() { return user_prenom; }
    public void setUser_prenom(String user_prenom) { this.user_prenom = user_prenom; }

    public String getUser_email() { return user_email; }
    public void setUser_email(String user_email) { this.user_email = user_email; }

    public String getUser_password() { return user_password; }
    public void setUser_password(String user_password) { this.user_password = user_password; }

    public String getDate_inscription() { return date_inscription; }
    public void setDate_inscription(String date_inscription) { this.date_inscription = date_inscription; }

    public String getUser_date_de_naissance() { return user_date_de_naissance; }
    public void setUser_date_de_naissance(String ddn) { this.user_date_de_naissance = ddn; }

    public Role getType_utilisateur() { return type_utilisateur; }
    public void setType_utilisateur(Role type_utilisateur) { this.type_utilisateur = type_utilisateur; }

    // SANTÉ
    public Sexe getUser_sexe() { return user_sexe; }
    public void setUser_sexe(Sexe user_sexe) { this.user_sexe = user_sexe; }

    public Double getUser_poids() { return user_poids; }
    public void setUser_poids(Double user_poids) { this.user_poids = user_poids; }

    public Integer getUser_taille() { return user_taille; }
    public void setUser_taille(Integer user_taille) { this.user_taille = user_taille; }

    public NiveauActivitePhysique getUser_niveau_activite_physique() { return user_niveau_activite_physique; }
    public void setUser_niveau_activite_physique(NiveauActivitePhysique nap) { this.user_niveau_activite_physique = nap; }

    // SCOLAIRE
    public NiveauScolaire getUser_niveau_scolaire() { return user_niveau_scolaire; }
    public void setUser_niveau_scolaire(NiveauScolaire niveauScolaire) { this.user_niveau_scolaire = niveauScolaire; }

    public String getUser_etablissement_scolaire() { return user_etablissement_scolaire; }
    public void setUser_etablissement_scolaire(String e) { this.user_etablissement_scolaire = e; }

    // IMAGE DE PROFIL
    public String getUser_image_path() { return user_image_path; }
    public void setUser_image_path(String user_image_path) { this.user_image_path = user_image_path; }

    // ✅ IMAGE DU VISAGE
    public String getFace_image_path() { return face_image_path; }
    public void setFace_image_path(String face_image_path) { this.face_image_path = face_image_path; }

    // ARCHIVAGE
    public boolean isIs_active() { return is_active; }
    public void setIs_active(boolean is_active) { this.is_active = is_active; }

    @Override
    public String toString() {
        return "user{" +
                "user_id=" + user_id +
                ", user_nom='" + user_nom + '\'' +
                ", user_prenom='" + user_prenom + '\'' +
                ", user_email='" + user_email + '\'' +
                ", type_utilisateur=" + type_utilisateur +
                ", is_active=" + is_active +
                '}';
    }
}
