package entities;
import java.sql.Timestamp;

public class Sommeil {
    private int id_sommeil;
    private Timestamp date_coucher;
    private Timestamp date_reveil;
    private String qualite_sommeil;

    public Sommeil() {}

    public Sommeil(int id_sommeil, Timestamp date_coucher, Timestamp date_reveil, String qualite_sommeil) {
        this.id_sommeil = id_sommeil;
        this.date_coucher = date_coucher;
        this.date_reveil = date_reveil;
        this.qualite_sommeil = qualite_sommeil;
    }

    public Sommeil(Timestamp date_coucher, Timestamp date_reveil, String qualite_sommeil) {
        this.date_coucher = date_coucher;
        this.date_reveil = date_reveil;
        this.qualite_sommeil = qualite_sommeil;
    }

    public int getId_sommeil() { return id_sommeil; }
    public void setId_sommeil(int id_sommeil) { this.id_sommeil = id_sommeil; }
    public Timestamp getDate_coucher() { return date_coucher; }
    public void setDate_coucher(Timestamp date_coucher) { this.date_coucher = date_coucher; }
    public Timestamp getDate_reveil() { return date_reveil; }
    public void setDate_reveil(Timestamp date_reveil) { this.date_reveil = date_reveil; }
    public String getQualite_sommeil() { return qualite_sommeil; }
    public void setQualite_sommeil(String qualite_sommeil) { this.qualite_sommeil = qualite_sommeil; }

    @Override
    public String toString() {
        return "Sommeil{id=" + id_sommeil + ", qualite='" + qualite_sommeil + "'}";
    }
}