package models.UserModels;

public enum Sexe {
    HOMME("Homme"),
    FEMME("Femme"),
    AUTRE("Autre");

    private final String displayName;

    Sexe(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
