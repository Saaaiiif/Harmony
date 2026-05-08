package models.MeditationModels;

public class Conseil {
    private int id;
    private int sessionId;
    private String contenu;

    public Conseil() {}

    public Conseil(int id, int sessionId, String contenu) {
        this.id = id;
        this.sessionId = sessionId;
        this.contenu = contenu;
    }

    public Conseil(int sessionId, String contenu) {
        this.sessionId = sessionId;
        this.contenu = contenu;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSessionId() { return sessionId; }
    public void setSessionId(int sessionId) { this.sessionId = sessionId; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    @Override
    public String toString() {
        return "Conseil{id=" + id + ", sessionId=" + sessionId + ", contenu='" + contenu + "'}";
    }
}
