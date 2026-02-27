package services.ActiviteServices;

import models.ActiviteModels.Activite;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SERVICE ACTIVITÉ - GESTION DES ACTIVITÉS/EXERCICES
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ✅ VERSION COMPLÈTE
 *
 * Responsabilités :
 *  - Ajouter une activité (exercice)
 *  - Afficher toutes les activités
 *  - Modifier une activité
 *  - Supprimer une activité
 *  - Filtrer par utilisateur
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class ServiceActivite {

    private Connection cnx;

    /**
     * Constructeur - Initialise la connexion à la BD
     */
    public ServiceActivite() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    /**
     * Ajoute une activité (exercice) dans la base de données
     *
     * @param a Objet Activite à ajouter
     */
    public void ajouter(Activite a) {
        String qry = "INSERT INTO `activite` (`user_id`, `date_activite`, `id_exercice`, `duree_minutes`, " +
                "`calories_brulees`, `nb_series`, `nb_repetitions`, `poids`, `notes`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, a.getUser_id());
            pstm.setTimestamp(2, a.getDate_activite());
            pstm.setInt(3, a.getId_exercice());
            pstm.setInt(4, a.getDuree_minutes());
            pstm.setInt(5, a.getCalories_brulees());
            pstm.setInt(6, a.getNb_series());
            pstm.setInt(7, a.getNb_repetitions());
            pstm.setFloat(8, a.getPoids());
            pstm.setString(9, a.getNotes());

            pstm.executeUpdate();
            System.out.println("✅ Activité ajoutée avec succès");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'ajout : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Récupère toutes les activités de la base de données
     * Triées par date (plus récentes en premier)
     *
     * @return Liste de toutes les Activite
     */
    public List<Activite> afficherTout() {
        List<Activite> liste = new ArrayList<>();
        String qry = "SELECT * FROM `activite` ORDER BY `date_activite` DESC";

        try (Statement stm = cnx.createStatement();
             ResultSet rs = stm.executeQuery(qry)) {

            while (rs.next()) {
                Activite a = new Activite(
                        rs.getInt("id_activite"),
                        rs.getTimestamp("date_activite"),
                        rs.getInt("id_exercice"),
                        rs.getInt("duree_minutes"),
                        rs.getInt("calories_brulees"),
                        rs.getInt("nb_series"),
                        rs.getInt("nb_repetitions"),
                        rs.getFloat("poids"),
                        rs.getString("notes")
                );
                a.setUser_id(rs.getInt("user_id"));
                liste.add(a);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'affichage : " + e.getMessage());
            e.printStackTrace();
        }
        return liste;
    }

    /**
     * Récupère les activités d'un utilisateur spécifique
     *
     * @param user_id L'ID de l'utilisateur
     * @return Liste des Activite pour cet utilisateur
     */
    public List<Activite> afficherParUtilisateur(int user_id) {
        List<Activite> liste = new ArrayList<>();
        String qry = "SELECT * FROM `activite` WHERE `user_id` = ? ORDER BY `date_activite` DESC";

        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, user_id);
            try (ResultSet rs = pstm.executeQuery()) {

                while (rs.next()) {
                    Activite a = new Activite(
                            rs.getInt("id_activite"),
                            rs.getTimestamp("date_activite"),
                            rs.getInt("id_exercice"),
                            rs.getInt("duree_minutes"),
                            rs.getInt("calories_brulees"),
                            rs.getInt("nb_series"),
                            rs.getInt("nb_repetitions"),
                            rs.getFloat("poids"),
                            rs.getString("notes")
                    );
                    a.setUser_id(rs.getInt("user_id"));
                    liste.add(a);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'affichage par utilisateur : " + e.getMessage());
            e.printStackTrace();
        }
        return liste;
    }

    /**
     * Modifie une activité existante
     *
     * @param a Objet Activite avec les nouvelles valeurs
     */
    public void modifier(Activite a) {
        String qry = "UPDATE `activite` " +
                "SET `date_activite` = ?, `id_exercice` = ?, `duree_minutes` = ?, `calories_brulees` = ?, " +
                "`nb_series` = ?, `nb_repetitions` = ?, `poids` = ?, `notes` = ? " +
                "WHERE `id_activite` = ?";
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
            System.out.println("✅ Activité modifiée avec succès");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la modification : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Supprime une activité par ID
     *
     * @param id L'ID de l'activité à supprimer
     */
    public void supprimer(int id) {
        String qry = "DELETE FROM `activite` WHERE `id_activite` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
            System.out.println("✅ Activité supprimée avec succès");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
        }
    }
}