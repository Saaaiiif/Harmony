package models;

import java.time.LocalDate;

public class NuitSommeil {
    private LocalDate dateCoucher;
    private String heureCoucher;
    private LocalDate dateReveil;
    private String heureReveil;
    private String qualite;
    private boolean stress;
    private boolean cafeine;
    private boolean bruit;

    public NuitSommeil(LocalDate dateCoucher, String heureCoucher, LocalDate dateReveil, String heureReveil,
                       String qualite, boolean stress, boolean cafeine, boolean bruit) {
        this.dateCoucher = dateCoucher;
        this.heureCoucher = heureCoucher;
        this.dateReveil = dateReveil;
        this.heureReveil = heureReveil;
        this.qualite = qualite;
        this.stress = stress;
        this.cafeine = cafeine;
        this.bruit = bruit;
    }

    // --- GETTERS & SETTERS POUR LA MODIFICATION ---
    public LocalDate getDateCoucher() { return dateCoucher; }
    public void setDateCoucher(LocalDate dateCoucher) { this.dateCoucher = dateCoucher; }

    public String getHeureCoucher() { return heureCoucher; }
    public void setHeureCoucher(String heureCoucher) { this.heureCoucher = heureCoucher; }

    public LocalDate getDateReveil() { return dateReveil; }
    public void setDateReveil(LocalDate dateReveil) { this.dateReveil = dateReveil; }

    public String getHeureReveil() { return heureReveil; }
    public void setHeureReveil(String heureReveil) { this.heureReveil = heureReveil; }

    public String getQualite() { return qualite; }
    public void setQualite(String qualite) { this.qualite = qualite; }

    public boolean isStress() { return stress; }
    public void setStress(boolean stress) { this.stress = stress; }

    public boolean isCafeine() { return cafeine; }
    public void setCafeine(boolean cafeine) { this.cafeine = cafeine; }

    public boolean isBruit() { return bruit; }
    public void setBruit(boolean bruit) { this.bruit = bruit; }

    // --- METHODES POUR L'AFFICHAGE DANS LE TABLEAU ---
    public String getCoucherAffichage() {
        return dateCoucher.toString() + " à " + heureCoucher;
    }

    public String getReveilAffichage() {
        return dateReveil.toString() + " à " + heureReveil;
    }

    public String getFacteursAffichage() {
        String f = "";
        if(stress) f += "Stress, ";
        if(cafeine) f += "Caféine, ";
        if(bruit) f += "Bruit, ";

        if (f.isEmpty()) return "Aucun";
        return f.substring(0, f.length() - 2); // Enlève la dernière virgule
    }
}