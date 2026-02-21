package models;

import java.time.LocalDate;

public class JournalHumeur {
    private int id;
    private int userId;
    private LocalDate date;
    private Humeur humeur;
    private int score;
    private String contenu;

    public JournalHumeur() {
    }

    public JournalHumeur(int id, int userId, LocalDate date, Humeur humeur, int score, String contenu) {
        this.id = id;
        this.userId = userId;
        this.date = date;
        this.humeur = humeur;
        this.score = score;
        this.contenu = contenu;
    }

    public JournalHumeur(int userId, LocalDate date, Humeur humeur, int score, String contenu) {
        this.userId = userId;
        this.date = date;
        this.humeur = humeur;
        this.score = score;
        this.contenu = contenu;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Humeur getHumeur() {
        return humeur;
    }

    public void setHumeur(Humeur humeur) {
        this.humeur = humeur;
        this.score = humeur.getScore();
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    @Override
    public String toString() {
        return "JournalHumeur{" +
                "id=" + id +
                ", userId=" + userId +
                ", date=" + date +
                ", humeur=" + humeur +
                ", score=" + score +
                '}';
    }
}
