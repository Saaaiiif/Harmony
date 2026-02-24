package services;

import models.Consommation;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceConsommation {
    private Connection cnx;

    public ServiceConsommation() { this.cnx = MyDataBase.getInstance().getCnx(); }

    public void ajouter(Consommation c) {
        String qry = "INSERT INTO `consommation` (`user_id`, `date_consommation`, `type_repas`, `id_aliment`, `quantite_eau_ml`, `poids_grammes`) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, c.getUser_id());
            pstm.setTimestamp(2, c.getDate_consommation());
            pstm.setString(3, c.getType_repas());
            pstm.setInt(4, c.getId_aliment());
            pstm.setInt(5, c.getQuantite_eau_ml());
            pstm.setInt(6, c.getPoids_grammes());
            pstm.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public List<Consommation> afficherTout() {
        List<Consommation> liste = new ArrayList<>();
        try (Statement stm = cnx.createStatement(); ResultSet rs = stm.executeQuery("SELECT * FROM `consommation` WHERE user_id = 1")) {
            while (rs.next()) {
                Consommation c = new Consommation(rs.getInt("id_consommation"), rs.getTimestamp("date_consommation"), rs.getString("type_repas"), rs.getInt("id_aliment"), rs.getInt("quantite_eau_ml"), rs.getInt("poids_grammes"));
                c.setUser_id(rs.getInt("user_id"));
                liste.add(c);
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return liste;
    }

    public void modifier(Consommation c) {
        String qry = "UPDATE `consommation` SET `date_consommation` = ?, `type_repas` = ?, `id_aliment` = ?, `quantite_eau_ml` = ?, `poids_grammes` = ? WHERE `id_consommation` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setTimestamp(1, c.getDate_consommation());
            pstm.setString(2, c.getType_repas());
            pstm.setInt(3, c.getId_aliment());
            pstm.setInt(4, c.getQuantite_eau_ml());
            pstm.setInt(5, c.getPoids_grammes());
            pstm.setInt(6, c.getId_consommation());
            pstm.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    public void supprimer(int id) {
        try (PreparedStatement pstm = cnx.prepareStatement("DELETE FROM `consommation` WHERE `id_consommation` = ?")) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }
}