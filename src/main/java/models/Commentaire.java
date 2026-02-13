package models;
import java.time.LocalDateTime;

public class Commentaire {
    private int idCommentaire;
    private String contenu;
    private LocalDateTime dateCommentaire;
    private int idPost;
    private int idEtudiant;

    public Commentaire() {}

    public Commentaire(int idCommentaire, String contenu,
                       LocalDateTime dateCommentaire,
                       int idPost, int idEtudiant) {
        this.idCommentaire = idCommentaire;
        this.contenu = contenu;
        this.dateCommentaire = LocalDateTime.now();
        this.idPost = idPost;
        this.idEtudiant = idEtudiant;
    }

    public int getIdCommentaire() { return idCommentaire; }
    public void setIdCommentaire(int idCommentaire) { this.idCommentaire = idCommentaire; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDateCommentaire() { return LocalDateTime.now(); }
    public void setDateCommentaire(LocalDateTime dateCommentaire) { this.dateCommentaire = dateCommentaire; }

    public int getIdPost() { return idPost; }
    public void setIdPost(int idPost) { this.idPost = idPost; }

    public int getIdEtudiant() { return idEtudiant; }
    public void setIdEtudiant(int idEtudiant) { this.idEtudiant = idEtudiant; }

    @Override
    public String toString() {
        return "Commentaire{" +
                "id=" + idCommentaire +
                ", contenu='" + contenu + '\'' +
                ", date=" + dateCommentaire +
                ", idPost=" + idPost +
                ", idEtudiant=" + idEtudiant +
                '}';
    }
}
