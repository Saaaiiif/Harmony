package models;
import java.time.LocalDateTime;


public class Post {
    private int idPost;
    private String titre;
    private String contenu;
    private LocalDateTime dateCreation;
    private int idEtudiant;
    private String nomEtudiant;  // pour affichage
    private int idCategorie;
    private String nomCategorie;  // pour affichage
    private String imagePath;


    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }


    public String getNomCategorie() {
        return nomCategorie;
    }
    public void setNomCategorie(String nomCategorie) {
        this.nomCategorie = nomCategorie;
    }


    public void setNomEtudiant(String nomEtudiant) {
        this.nomEtudiant = nomEtudiant;
    }
    public String getNomEtudiant() {
        return nomEtudiant;
    }




    public Post() {}
    public Post(String titre, String contenu, int idEtudiant, int idCategorie ,String imagePath) {
        this.titre = titre;
        this.contenu = contenu;
        this.dateCreation = LocalDateTime.now();
        this.idEtudiant = idEtudiant;
        this.idCategorie = idCategorie;
        this.imagePath = imagePath;
    }

    public Post(int idPost, String titre, String contenu,
                LocalDateTime dateCreation, int idEtudiant, int idCategorie) {
        this.idPost = idPost;
        this.titre = titre;
        this.contenu = contenu;
        this.dateCreation = LocalDateTime.now();
        this.idEtudiant = idEtudiant;
        this.idCategorie = idCategorie;
    }

    public int getIdPost() { return idPost; }
    public void setIdPost(int idPost) { this.idPost = idPost; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public int getIdEtudiant() { return idEtudiant; }
    public void setIdEtudiant(int idEtudiant) { this.idEtudiant = idEtudiant; }

    public int getIdCategorie() { return idCategorie; }
    public void setIdCategorie(int idCategorie) { this.idCategorie = idCategorie; }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + idPost +
                ", titre='" + titre + '\'' +
                ", contenu='" + contenu + '\'' +
                ", date=" + dateCreation +
                ", idEtudiant=" + idEtudiant +
                ", idCategorie=" + idCategorie +
                '}';
    }
}
