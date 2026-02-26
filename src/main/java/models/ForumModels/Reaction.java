package models.ForumModels;
import java.time.LocalDateTime;
import java.util.Objects;

public class Reaction {
    private int idReaction;
    private String typeReaction;
    private LocalDateTime dateReaction;
    private int idPost;
    private int idEtudiant;

    public Reaction(int idReaction, String typeReaction, LocalDateTime dateReaction, int idPost, int idEtudiant) {
        this.idReaction = idReaction;
        this.typeReaction = typeReaction;
        this.dateReaction = dateReaction;
        this.idPost = idPost;
        this.idEtudiant = idEtudiant;
    }

    public int getIdReaction() {
        return idReaction;
    }

    public String getTypeReaction() {
        return typeReaction;
    }

    public LocalDateTime getDateReaction() {
        return dateReaction;
    }

    public int getIdPost() {
        return idPost;
    }

    public int getIdEtudiant() {
        return idEtudiant;
    }

    public void setIdReaction(int idReaction) {
        this.idReaction = idReaction;
    }

    public void setTypeReaction(String typeReaction) {
        this.typeReaction = typeReaction;
    }

    public void setDateReaction(LocalDateTime dateReaction) {
        this.dateReaction = dateReaction;
    }

    public void setIdPost(int idPost) {
        this.idPost = idPost;
    }

    public void setIdEtudiant(int idEtudiant) {
        this.idEtudiant = idEtudiant;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Reaction reaction = (Reaction) o;
        return idReaction == reaction.idReaction && idPost == reaction.idPost && idEtudiant == reaction.idEtudiant && Objects.equals(typeReaction, reaction.typeReaction) && Objects.equals(dateReaction, reaction.dateReaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idReaction, typeReaction, dateReaction, idPost, idEtudiant);
    }

    @Override
    public String toString() {
        return "Reaction{" +
                "idReaction=" + idReaction +
                ", typeReaction='" + typeReaction + '\'' +
                ", dateReaction=" + dateReaction +
                ", idPost=" + idPost +
                ", idEtudiant=" + idEtudiant +
                '}';
    }
}
