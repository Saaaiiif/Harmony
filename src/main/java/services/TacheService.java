package services;

import interfaces.Services;
import models.StatutTache;
import models.Tache;
import utiles.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TacheService implements Services<Tache> {

    private final Connection cnx = MyDataBase.getInstance().getConnection();

    @Override
    public void add(Tache t) {
        if (cnx == null) {
            throw new RuntimeException("Pas de connexion à la base de données.");
        }
        String sql = "INSERT INTO tache (nom, deadline, notes, statut_tache, calendrier_id) VALUES (?,?,?,?,1)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, t.getNom());
            ps.setDate(2, t.getDeadline() != null ? new java.sql.Date(t.getDeadline().getTime()) : null);
            ps.setString(3, t.getNotes());
            ps.setString(4, t.getStatut() != null ? t.getStatut().name() : StatutTache.A_FAIRE.name());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Impossible d'ajouter la tâche : " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Tache> getAll() {
        List<Tache> list = new ArrayList<>();
        String sql = "SELECT * FROM tache";
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
    public void update(Tache t) {
        String sql = "UPDATE tache SET nom=?, deadline=?, notes=?, statut_tache=?, calendrier_id=1 WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, t.getNom());
            ps.setDate(2, t.getDeadline() != null ? new java.sql.Date(t.getDeadline().getTime()) : null);
            ps.setString(3, t.getNotes());
            ps.setString(4, t.getStatut() != null ? t.getStatut().name() : StatutTache.A_FAIRE.name());
            ps.setInt(5, t.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Impossible de modifier la tâche : " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(int id) {
        if (cnx == null) {
            throw new RuntimeException("Pas de connexion à la base de données.");
        }
        String sql = "DELETE FROM tache WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Impossible de supprimer la tâche : " + ex.getMessage(), ex);
        }
    }

    public Tache getById(int id) {
        String sql = "SELECT * FROM tache WHERE id=?";
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

    private static Tache mapRow(ResultSet rs) throws SQLException {
        String statutStr = rs.getString("statut_tache");
        StatutTache statut = statutStr != null ? StatutTache.valueOf(statutStr) : StatutTache.A_FAIRE;
        return new Tache(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getDate("deadline"),
                rs.getString("notes"),
                statut
        );
    }
}
