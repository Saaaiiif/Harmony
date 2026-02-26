package models.UserModels;

public enum NiveauActivitePhysique {
    SEDENTAIRE("Sédentaire", "Peu ou pas d'exercice"),
    LEGER("Léger", "Exercice léger 1-3 jours/semaine"),
    MODERE("Modéré", "Exercice modéré 3-5 jours/semaine"),
    INTENSE("Intense", "Exercice intense 6-7 jours/semaine"),
    TRES_INTENSE("Très intense", "Exercice très intense & travail physique");

    private final String displayName;
    private final String description;

    NiveauActivitePhysique(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
