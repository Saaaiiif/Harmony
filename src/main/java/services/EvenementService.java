package services;

import interfaces.Services;
import models.Evenement;
import models.TypeEvenement;
import utiles.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService implements Services<Evenement> {

    private Connection cnx = MyDataBase.getInstance().getConnection();

    // CREATE
    @Override
    public void add(Evenement e) {
        String sql = "INSERT INTO evenement (titre, description, date_debut, date_fin, lieu, priorite, rappel_actif, type_evenement) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setDate(3, new java.sql.Date(e.getDateDebut().getTime()));
            ps.setDate(4, new java.sql.Date(e.getDateFin().getTime()));
            ps.setString(5, e.getLieu());
            ps.setInt(6, e.getPriorite());
            ps.setBoolean(7, e.isRappelActif());
            ps.setString(8, e.getType().name());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // READ
    @Override
    public List<Evenement> getAll() {
        List<Evenement> list = new ArrayList<>();
        String sql = "SELECT * FROM evenement";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Evenement(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getDate("date_debut"),
                        rs.getDate("date_fin"),
                        rs.getString("lieu"),
                        rs.getInt("priorite"),
                        rs.getBoolean("rappel_actif"),
                        TypeEvenement.valueOf(rs.getString("type_evenement"))
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // UPDATE
    @Override
    public void update(Evenement e) {
        String sql = "UPDATE evenement SET titre=?, description=?, date_debut=?, date_fin=?, lieu=?, priorite=?, rappel_actif=?, type_evenement=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setDate(3, new java.sql.Date(e.getDateDebut().getTime()));
            ps.setDate(4, new java.sql.Date(e.getDateFin().getTime()));
            ps.setString(5, e.getLieu());
            ps.setInt(6, e.getPriorite());
            ps.setBoolean(7, e.isRappelActif());
            ps.setString(8, e.getType().name());
            ps.setInt(9, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // DELETE
    @Override
    public void delete(int id) {
        String sql = "DELETE FROM evenement WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
