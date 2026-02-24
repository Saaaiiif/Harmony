package services;

import models.Activite;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceActivite {
    private Connection cnx;

    public ServiceActivite() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    public void ajouter(Activite a) {
        String qry = "INSERT INTO `activite` (`user_id`, `date_activite`, `id_exercice`, `duree_minutes`, `calories_brulees`, `nb_series`, `nb_repetitions`, `poids`, `notes`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, a.getUser_id()); // Gestion du user_id !
            pstm.setTimestamp(2, a.getDate_activite());
            pstm.setInt(3, a.getId_exercice());
            pstm.setInt(4, a.getDuree_minutes());
            pstm.setInt(5, a.getCalories_brulees());
            pstm.setInt(6, a.getNb_series());
            pstm.setInt(7, a.getNb_repetitions());
            pstm.setFloat(8, a.getPoids());
            pstm.setString(9, a.getNotes());
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout : " + e.getMessage());
        }
    }

    public List<Activite> afficherTout() {
        List<Activite> liste = new ArrayList<>();
        // Filtrage par utilisateur connecté (1 par défaut)
        String req = "SELECT * FROM `activite` WHERE user_id = 1";
        try (Statement stm = cnx.createStatement(); ResultSet rs = stm.executeQuery(req)) {
            while (rs.next()) {
                Activite a = new Activite(
                        rs.getInt("id_activite"), rs.getTimestamp("date_activite"),
                        rs.getInt("id_exercice"), rs.getInt("duree_minutes"),
                        rs.getInt("calories_brulees"), rs.getInt("nb_series"),
                        rs.getInt("nb_repetitions"), rs.getFloat("poids"), rs.getString("notes")
                );
                a.setUser_id(rs.getInt("user_id"));
                liste.add(a);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'affichage : " + e.getMessage());
        }
        return liste;
    }

    public void modifier(Activite a) {
        String qry = "UPDATE `activite` SET `date_activite` = ?, `id_exercice` = ?, `duree_minutes` = ?, `calories_brulees` = ?, `nb_series` = ?, `nb_repetitions` = ?, `poids` = ?, `notes` = ? WHERE `id_activite` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setTimestamp(1, a.getDate_activite());
            pstm.setInt(2, a.getId_exercice());
            pstm.setInt(3, a.getDuree_minutes());
            pstm.setInt(4, a.getCalories_brulees());
            pstm.setInt(5, a.getNb_series());
            pstm.setInt(6, a.getNb_repetitions());
            pstm.setFloat(7, a.getPoids());
            pstm.setString(8, a.getNotes());
            pstm.setInt(9, a.getId_activite());
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la modification : " + e.getMessage());
        }
    }

    public void supprimer(int id) {
        String qry = "DELETE FROM `activite` WHERE `id_activite` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression : " + e.getMessage());
        }
    }
}