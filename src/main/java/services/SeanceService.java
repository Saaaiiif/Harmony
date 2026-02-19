package services;

import interfaces.Services;
import models.Seance;
import utiles.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SeanceService implements Services<Seance> {

    private final Connection cnx = MyDataBase.getInstance().getConnection();

    @Override
    public void add(Seance s) {
        if (cnx == null) {
            throw new RuntimeException("Pas de connexion à la base de données.");
        }
        String sql = "INSERT INTO seance (titre, description, date_debut, date_fin, salle_id, confirmee, type_seance, nombre_participants, date_creation) VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, s.getTitre());
            ps.setString(2, s.getDescription());
            ps.setTimestamp(3, s.getDateDebut() != null ? new Timestamp(s.getDateDebut().getTime()) : null);
            ps.setTimestamp(4, s.getDateFin() != null ? new Timestamp(s.getDateFin().getTime()) : null);
            ps.setInt(5, s.getSalleId());
            ps.setBoolean(6, s.isConfirmee());
            ps.setString(7, s.getTypeSeance());
            ps.setInt(8, s.getNombreParticipants());
            ps.setTimestamp(9, s.getDateCreation() != null ? new Timestamp(s.getDateCreation().getTime()) : new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Impossible d'ajouter la séance : " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Seance> getAll() {
        List<Seance> list = new ArrayList<>();
        String sql = "SELECT * FROM seance ORDER BY date_debut";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void update(Seance s) {
        String sql = "UPDATE seance SET titre=?, description=?, date_debut=?, date_fin=?, salle_id=?, confirmee=?, type_seance=?, nombre_participants=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, s.getTitre());
            ps.setString(2, s.getDescription());
            ps.setTimestamp(3, s.getDateDebut() != null ? new Timestamp(s.getDateDebut().getTime()) : null);
            ps.setTimestamp(4, s.getDateFin() != null ? new Timestamp(s.getDateFin().getTime()) : null);
            ps.setInt(5, s.getSalleId());
            ps.setBoolean(6, s.isConfirmee());
            ps.setString(7, s.getTypeSeance());
            ps.setInt(8, s.getNombreParticipants());
            ps.setInt(9, s.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Impossible de modifier la séance : " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(int id) {
        if (cnx == null) {
            throw new RuntimeException("Pas de connexion à la base de données.");
        }
        String sql = "DELETE FROM seance WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Impossible de supprimer la séance : " + ex.getMessage(), ex);
        }
    }

    public Seance getById(int id) {
        String sql = "SELECT * FROM seance WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Seance> getBySalleId(int salleId) {
        List<Seance> list = new ArrayList<>();
        String sql = "SELECT * FROM seance WHERE salle_id=? ORDER BY date_debut";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, salleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Seance> getNonConfirmees() {
        List<Seance> list = new ArrayList<>();
        String sql = "SELECT * FROM seance WHERE confirmee=false ORDER BY date_debut";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static Seance mapRow(ResultSet rs) throws SQLException {
        return new Seance(
                rs.getInt("id"),
                rs.getString("titre"),
                rs.getString("description"),
                rs.getTimestamp("date_debut"),
                rs.getTimestamp("date_fin"),
                rs.getInt("salle_id"),
                rs.getBoolean("confirmee"),
                rs.getString("type_seance"),
                rs.getInt("nombre_participants"),
                rs.getTimestamp("date_creation")
        );
    }
}
