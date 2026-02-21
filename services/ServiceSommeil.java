package services;

import models.Sommeil;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceSommeil {
    private Connection cnx;

    public ServiceSommeil() { this.cnx = MyDataBase.getInstance().getCnx(); }

    public boolean ajouter(Sommeil s) {
        String qry = "INSERT INTO `sommeil` (`date_coucher`, `date_reveil`, `qualite_sommeil`, `stress`, `cafeine`, `bruit`) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setTimestamp(1, s.getDate_coucher());
            pstm.setTimestamp(2, s.getDate_reveil());
            pstm.setString(3, s.getQualite_sommeil());
            pstm.setBoolean(4, s.isStress());
            pstm.setBoolean(5, s.isCafeine());
            pstm.setBoolean(6, s.isBruit());

            int rows = pstm.executeUpdate(); // Vérifie si la ligne a bien été ajoutée
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("ERREUR SQL AJOUT SOMMEIL : " + e.getMessage());
            return false;
        }
    }

    public List<Sommeil> afficherTout() {
        List<Sommeil> liste = new ArrayList<>();
        try (Statement stm = cnx.createStatement(); ResultSet rs = stm.executeQuery("SELECT * FROM `sommeil`")) {
            while (rs.next()) {
                Sommeil s = new Sommeil(
                        rs.getInt("id_sommeil"),
                        rs.getTimestamp("date_coucher"),
                        rs.getTimestamp("date_reveil"),
                        rs.getString("qualite_sommeil"),
                        rs.getBoolean("stress"),
                        rs.getBoolean("cafeine"),
                        rs.getBoolean("bruit")
                );
                liste.add(s);
            }
        } catch (SQLException e) { System.err.println("ERREUR AFFICHER SOMMEIL : " + e.getMessage()); }
        return liste;
    }

    public boolean modifier(Sommeil s) {
        String qry = "UPDATE `sommeil` SET `date_coucher` = ?, `date_reveil` = ?, `qualite_sommeil` = ?, `stress` = ?, `cafeine` = ?, `bruit` = ? WHERE `id_sommeil` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setTimestamp(1, s.getDate_coucher());
            pstm.setTimestamp(2, s.getDate_reveil());
            pstm.setString(3, s.getQualite_sommeil());
            pstm.setBoolean(4, s.isStress());
            pstm.setBoolean(5, s.isCafeine());
            pstm.setBoolean(6, s.isBruit());
            pstm.setInt(7, s.getId_sommeil());

            int rows = pstm.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("ERREUR SQL MODIF SOMMEIL : " + e.getMessage());
            return false;
        }
    }

    public boolean supprimer(int id_sommeil) {
        String qry = "DELETE FROM `sommeil` WHERE `id_sommeil` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id_sommeil);
            int rows = pstm.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("ERREUR SQL SUPPR SOMMEIL : " + e.getMessage());
            return false;
        }
    }
}