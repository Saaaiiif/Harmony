package models.MeditationModels;

public class SessionMeditation {
    private int id;
    private int userId;
    private String auteur;
    private int duree;
    private String theme;
    private String audioUrl;

    public SessionMeditation() {}

    public SessionMeditation(int id, int userId, String auteur, int duree, String theme, String audioUrl) {
        this.id = id;
        this.userId = userId;
        this.auteur = auteur;
        this.duree = duree;
        this.theme = theme;
        this.audioUrl = audioUrl;
    }

    public SessionMeditation(int userId, String auteur, int duree, String theme, String audioUrl) {
        this.userId = userId;
        this.auteur = auteur;
        this.duree = duree;
        this.theme = theme;
        this.audioUrl = audioUrl;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAuteur() { return auteur; }
    public void setAuteur(String auteur) { this.auteur = auteur; }

    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public String getDureeFormatted() { return duree + " min"; }

    @Override
    public String toString() {
        return "SessionMeditation{id=" + id + ", auteur='" + auteur + "', theme='" + theme + "', duree=" + duree + '}';
    }
}
