package models;

public enum NiveauScolaire {
    PRIMAIRE("Primaire"),
    COLLEGE("Collège"),
    LYCEE("Lycée"),
    LICENCE("Licence (Bac+3)"),
    MASTER("Master (Bac+5)"),
    DOCTORAT("Doctorat (Bac+8)"),
    AUTRE("Autre");

    private final String displayName;

    NiveauScolaire(String displayName) {
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
