package models.ForumModels;

import java.time.LocalDateTime;

public class Categorie {
    private int idCategorie;
    private String nomCategorie;
    private String description;
    private LocalDateTime dateCreation;

    public Categorie() {}

    public Categorie(String nomCategorie, String description) {
        this.nomCategorie = nomCategorie;
        this.description = description;
        this.dateCreation = LocalDateTime.now();
    }


    public int getIdCategorie() { return idCategorie; }
    public void setIdCategorie(int idCategorie) { this.idCategorie = idCategorie; }

    public String getNomCategorie() { return nomCategorie; }
    public void setNomCategorie(String nomCategorie) { this.nomCategorie = nomCategorie; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    @Override
    public String toString() {
        return "Categorie{" +
                "id=" + idCategorie +
                ", nom='" + nomCategorie + '\'' +
                ", description='" + description + '\'' +
                ", dateCreation=" + dateCreation +
                '}';
    }
}
