package models.ActiviteModels;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class Sommeil {
    private int id_sommeil;
    private int user_id; // CORRIGÉ
    private Timestamp date_coucher;
    private Timestamp date_reveil;
    private String qualite_sommeil;
    private boolean stress;
    private boolean cafeine;
    private boolean bruit;

    public Sommeil() {}

    public Sommeil(int id_sommeil, int user_id, Timestamp date_coucher, Timestamp date_reveil, String qualite_sommeil, boolean stress, boolean cafeine, boolean bruit) {
        this.id_sommeil = id_sommeil;
        this.user_id = user_id;
        this.date_coucher = date_coucher;
        this.date_reveil = date_reveil;
        this.qualite_sommeil = qualite_sommeil;
        this.stress = stress;
        this.cafeine = cafeine;
        this.bruit = bruit;
    }

    public Sommeil(int user_id, Timestamp date_coucher, Timestamp date_reveil, String qualite_sommeil, boolean stress, boolean cafeine, boolean bruit) {
        this.user_id = user_id;
        this.date_coucher = date_coucher;
        this.date_reveil = date_reveil;
        this.qualite_sommeil = qualite_sommeil;
        this.stress = stress;
        this.cafeine = cafeine;
        this.bruit = bruit;
    }

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public int getId_sommeil() { return id_sommeil; }
    public void setId_sommeil(int id_sommeil) { this.id_sommeil = id_sommeil; }
    public Timestamp getDate_coucher() { return date_coucher; }
    public void setDate_coucher(Timestamp date_coucher) { this.date_coucher = date_coucher; }
    public Timestamp getDate_reveil() { return date_reveil; }
    public void setDate_reveil(Timestamp date_reveil) { this.date_reveil = date_reveil; }
    public String getQualite_sommeil() { return qualite_sommeil; }
    public void setQualite_sommeil(String qualite_sommeil) { this.qualite_sommeil = qualite_sommeil; }
    public boolean isStress() { return stress; }
    public void setStress(boolean stress) { this.stress = stress; }
    public boolean isCafeine() { return cafeine; }
    public void setCafeine(boolean cafeine) { this.cafeine = cafeine; }
    public boolean isBruit() { return bruit; }
    public void setBruit(boolean bruit) { this.bruit = bruit; }

    public String getCoucherAffichage() {
        if (date_coucher == null) return "";
        return new SimpleDateFormat("dd/MM/yyyy 'à' HH:mm").format(date_coucher);
    }
    public String getReveilAffichage() {
        if (date_reveil == null) return "";
        return new SimpleDateFormat("dd/MM/yyyy 'à' HH:mm").format(date_reveil);
    }
}