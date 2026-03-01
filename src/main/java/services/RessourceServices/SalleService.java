package services.RessourceServices;

import interfaces.RessourceServices;
import models.RessourceModels.Salle;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalleService implements RessourceServices<Salle> {

    private final Connection cnx = MyDataBase.getInstance().getCnx();

    @Override
    public void add(Salle s) {
        if (cnx == null) {
            throw new RuntimeException("Pas de connexion à la base de données.");
        }
        String sql = "INSERT INTO salle (nom, capacite, equipements, disponible, description, date_creation) VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, s.getNom());
            ps.setInt(2, s.getCapacite());
            ps.setString(3, s.getEquipements());
            ps.setBoolean(4, s.isDisponible());
            ps.setString(5, s.getDescription());
            ps.setTimestamp(6, s.getDateCreation() != null ? new Timestamp(s.getDateCreation().getTime()) : new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Impossible d'ajouter la salle : " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Salle> getAll() {
        List<Salle> list = new ArrayList<>();
        if (cnx == null) return list;
        String sql = "SELECT * FROM salle ORDER BY nom";
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
    public void update(Salle s) {
        String sql = "UPDATE salle SET nom=?, capacite=?, equipements=?, disponible=?, description=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, s.getNom());
            ps.setInt(2, s.getCapacite());
            ps.setString(3, s.getEquipements());
            ps.setBoolean(4, s.isDisponible());
            ps.setString(5, s.getDescription());
            ps.setInt(6, s.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Impossible de modifier la salle : " + ex.getMessage(), ex);
        }
    }

    @Override
    public void delete(int id) {
        if (cnx == null) {
            throw new RuntimeException("Pas de connexion à la base de données.");
        }
        String sql = "DELETE FROM salle WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Impossible de supprimer la salle : " + ex.getMessage(), ex);
        }
    }

    public Salle getById(int id) {
        String sql = "SELECT * FROM salle WHERE id=?";
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

    private static Salle mapRow(ResultSet rs) throws SQLException {
        return new Salle(
                rs.getInt("id"),
                rs.getString("nom"),
                rs.getInt("capacite"),
                rs.getString("equipements"),
                rs.getBoolean("disponible"),
                rs.getString("description"),
                rs.getTimestamp("date_creation")
        );
    }
}
