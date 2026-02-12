package services;

import entities.Sommeil;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceSommeil {
    private Connection cnx;

    public ServiceSommeil() { this.cnx = MyDataBase.getInstance().getCnx(); }

    public void ajouter(Sommeil s) {
        String qry = "INSERT INTO `sommeil` (`date_coucher`, `date_reveil`, `qualite_sommeil`) VALUES (?, ?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setTimestamp(1, s.getDate_coucher());
            pstm.setTimestamp(2, s.getDate_reveil());
            pstm.setString(3, s.getQualite_sommeil());
            pstm.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public List<Sommeil> afficherTout() {
        List<Sommeil> liste = new ArrayList<>();
        try (Statement stm = cnx.createStatement(); ResultSet rs = stm.executeQuery("SELECT * FROM `sommeil`")) {
            while (rs.next()) {
                liste.add(new Sommeil(rs.getInt("id_sommeil"), rs.getTimestamp("date_coucher"), rs.getTimestamp("date_reveil"), rs.getString("qualite_sommeil")));
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return liste;
    }

    public void modifier(Sommeil s) {
        String qry = "UPDATE `sommeil` SET `date_coucher` = ?, `date_reveil` = ?, `qualite_sommeil` = ? WHERE `id_sommeil` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setTimestamp(1, s.getDate_coucher());
            pstm.setTimestamp(2, s.getDate_reveil());
            pstm.setString(3, s.getQualite_sommeil());
            pstm.setInt(4, s.getId_sommeil());
            pstm.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public void supprimer(int id) {
        try (PreparedStatement pstm = cnx.prepareStatement("DELETE FROM `sommeil` WHERE `id_sommeil` = ?")) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }
}