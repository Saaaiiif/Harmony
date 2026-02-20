package services;

import interfaces.Services;
import models.Evenement;
import models.StatutDemandeSalle;
import models.TypeEvenement;
import utiles.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService implements Services<Evenement> {

    private Connection cnx = MyDataBase.getInstance().getConnection();

    @Override
    public void add(Evenement e) {
        String sql = "INSERT INTO evenement (titre, description, date_debut, date_fin, lieu, priorite, rappel_actif, type_evenement, salle_id, statut_demande_salle) VALUES (?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setDate(3, new java.sql.Date(e.getDateDebut().getTime()));
            ps.setDate(4, new java.sql.Date(e.getDateFin().getTime()));
            ps.setString(5, e.getLieu());
            ps.setInt(6, e.getPriorite());
            ps.setBoolean(7, e.isRappelActif());
            ps.setString(8, e.getType().name());
            ps.setObject(9, e.getSalleId());
            ps.setString(10, e.getStatutDemandeSalle() != null ? e.getStatutDemandeSalle().name() : null);
            ps.executeUpdate();
        } catch (SQLException ex) {
            if (ex.getMessage() != null && (ex.getMessage().contains("salle_id") || ex.getMessage().contains("statut_demande_salle"))) {
                addSansDemandeSalle(e);
            } else {
                ex.printStackTrace();
            }
        }
    }

    private void addSansDemandeSalle(Evenement e) {
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
        } catch (SQLException ex2) {
            ex2.printStackTrace();
        }
    }

    @Override
    public List<Evenement> getAll() {
        List<Evenement> list = new ArrayList<>();
        String sql = "SELECT * FROM evenement ORDER BY date_debut DESC";
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

    private Evenement mapRow(ResultSet rs) throws SQLException {
        Integer salleId = null;
        StatutDemandeSalle statut = null;
        try {
            int sid = rs.getInt("salle_id");
            if (!rs.wasNull()) salleId = sid;
        } catch (SQLException ignored) { }
        try {
            String s = rs.getString("statut_demande_salle");
            if (s != null && !s.isEmpty()) statut = StatutDemandeSalle.valueOf(s);
        } catch (SQLException | IllegalArgumentException ignored) { }

        return new Evenement(
                rs.getInt("id"),
                rs.getString("titre"),
                rs.getString("description"),
                rs.getDate("date_debut"),
                rs.getDate("date_fin"),
                rs.getString("lieu"),
                rs.getInt("priorite"),
                rs.getBoolean("rappel_actif"),
                TypeEvenement.valueOf(rs.getString("type_evenement")),
                salleId,
                statut
        );
    }

    @Override
    public void update(Evenement e) {
        String sql = "UPDATE evenement SET titre=?, description=?, date_debut=?, date_fin=?, lieu=?, priorite=?, rappel_actif=?, type_evenement=?, salle_id=?, statut_demande_salle=? WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, e.getTitre());
            ps.setString(2, e.getDescription());
            ps.setDate(3, new java.sql.Date(e.getDateDebut().getTime()));
            ps.setDate(4, new java.sql.Date(e.getDateFin().getTime()));
            ps.setString(5, e.getLieu());
            ps.setInt(6, e.getPriorite());
            ps.setBoolean(7, e.isRappelActif());
            ps.setString(8, e.getType().name());
            ps.setObject(9, e.getSalleId());
            ps.setString(10, e.getStatutDemandeSalle() != null ? e.getStatutDemandeSalle().name() : null);
            ps.setInt(11, e.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            if (ex.getMessage() != null && (ex.getMessage().contains("salle_id") || ex.getMessage().contains("statut_demande_salle"))) {
                updateSansDemandeSalle(e);
            } else {
                ex.printStackTrace();
            }
        }
    }

    private void updateSansDemandeSalle(Evenement e) {
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
        } catch (SQLException ex2) {
            ex2.printStackTrace();
        }
    }

    /** Demandes de salle en attente (pour admin) */
    public List<Evenement> getDemandesEnAttente() {
        List<Evenement> list = new ArrayList<>();
        if (cnx == null) return list;
        String sql = "SELECT * FROM evenement WHERE salle_id IS NOT NULL AND statut_demande_salle = 'EN_ATTENTE' ORDER BY date_debut";
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

    /** Vérifie si la salle est disponible (pas de chevauchement avec un autre événement confirmé) */
    public boolean isSalleDisponiblePourEvenement(Evenement ev) {
        if (ev.getSalleId() == null) return true;
        String sql = "SELECT COUNT(*) FROM evenement WHERE salle_id = ? AND statut_demande_salle = 'CONFIRME' AND id != ? " +
                "AND date_debut < ? AND date_fin > ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, ev.getSalleId());
            ps.setInt(2, ev.getId());
            ps.setDate(3, new java.sql.Date(ev.getDateFin().getTime()));
            ps.setDate(4, new java.sql.Date(ev.getDateDebut().getTime()));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) == 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

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
