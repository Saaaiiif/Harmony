package models;

public enum Humeur {
    TRES_BIEN("Très bien", 5),
    BIEN("Bien", 4),
    NEUTRE("Neutre", 3),
    MAL("Mal", 2),
    TRES_MAL("Très mal", 1);

    private final String label;
    private final int score;

    Humeur(String label, int score) {
        this.label = label;
        this.score = score;
    }

    public String getLabel() {
        return label;
    }

    public int getScore() {
        return score;
    }

    @Override
    public String toString() {
        return label;
    }

    public static Humeur fromScore(int score) {
        for (Humeur h : values()) {
            if (h.score == score) {
                return h;
            }
        }
        return NEUTRE;
    }

    public static Humeur fromString(String text) {
        for (Humeur h : values()) {
            if (h.name().equalsIgnoreCase(text) || h.label.equalsIgnoreCase(text)) {
                return h;
            }
        }
        return NEUTRE;
    }
}
