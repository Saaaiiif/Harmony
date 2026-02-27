package services.ActiviteServices;

import models.ActiviteModels.Consommation;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SERVICE CONSOMMATION - GESTION DES CONSOMMATIONS ALIMENTAIRES
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ✅ VERSION COMPLÈTE
 *
 * Responsabilités :
 *  - Ajouter une consommation (repas)
 *  - Afficher toutes les consommations
 *  - Modifier une consommation
 *  - Supprimer une consommation
 *  - Filtrer par utilisateur
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class ServiceConsommation {

    private Connection cnx;

    /**
     * Constructeur - Initialise la connexion à la BD
     */
    public ServiceConsommation() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    /**
     * Ajoute une consommation alimentaire dans la base de données
     *
     * @param c Objet Consommation à ajouter
     */
    public void ajouter(Consommation c) {
        String qry = "INSERT INTO `consommation` (`user_id`, `date_consommation`, `type_repas`, " +
                "`id_aliment`, `quantite_eau_ml`, `poids_grammes`) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, c.getUser_id());
            pstm.setTimestamp(2, c.getDate_consommation());
            pstm.setString(3, c.getType_repas());
            pstm.setInt(4, c.getId_aliment());
            pstm.setInt(5, c.getQuantite_eau_ml());
            pstm.setInt(6, c.getPoids_grammes());

            pstm.executeUpdate();
            System.out.println("✅ Consommation ajoutée avec succès");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'ajout : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Récupère toutes les consommations de la base de données
     * Triées par date (plus récentes en premier)
     *
     * @return Liste de toutes les Consommation
     */
    public List<Consommation> afficherTout() {
        List<Consommation> liste = new ArrayList<>();
        String qry = "SELECT * FROM `consommation` ORDER BY `date_consommation` DESC";

        try (Statement stm = cnx.createStatement();
             ResultSet rs = stm.executeQuery(qry)) {

            while (rs.next()) {
                Consommation c = new Consommation(
                        rs.getInt("id_consommation"),
                        rs.getTimestamp("date_consommation"),
                        rs.getString("type_repas"),
                        rs.getInt("id_aliment"),
                        rs.getInt("quantite_eau_ml"),
                        rs.getInt("poids_grammes")
                );
                c.setUser_id(rs.getInt("user_id"));
                liste.add(c);
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'affichage : " + e.getMessage());
            e.printStackTrace();
        }
        return liste;
    }

    /**
     * Récupère les consommations d'un utilisateur spécifique
     *
     * @param user_id L'ID de l'utilisateur
     * @return Liste des Consommation pour cet utilisateur
     */
    public List<Consommation> afficherParUtilisateur(int user_id) {
        List<Consommation> liste = new ArrayList<>();
        String qry = "SELECT * FROM `consommation` WHERE `user_id` = ? ORDER BY `date_consommation` DESC";

        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, user_id);
            try (ResultSet rs = pstm.executeQuery()) {

                while (rs.next()) {
                    Consommation c = new Consommation(
                            rs.getInt("id_consommation"),
                            rs.getTimestamp("date_consommation"),
                            rs.getString("type_repas"),
                            rs.getInt("id_aliment"),
                            rs.getInt("quantite_eau_ml"),
                            rs.getInt("poids_grammes")
                    );
                    c.setUser_id(rs.getInt("user_id"));
                    liste.add(c);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'affichage par utilisateur : " + e.getMessage());
            e.printStackTrace();
        }
        return liste;
    }

    /**
     * Modifie une consommation existante
     *
     * @param c Objet Consommation avec les nouvelles valeurs
     */
    public void modifier(Consommation c) {
        String qry = "UPDATE `consommation` " +
                "SET `date_consommation` = ?, `type_repas` = ?, `id_aliment` = ?, " +
                "`quantite_eau_ml` = ?, `poids_grammes` = ? " +
                "WHERE `id_consommation` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setTimestamp(1, c.getDate_consommation());
            pstm.setString(2, c.getType_repas());
            pstm.setInt(3, c.getId_aliment());
            pstm.setInt(4, c.getQuantite_eau_ml());
            pstm.setInt(5, c.getPoids_grammes());
            pstm.setInt(6, c.getId_consommation());

            pstm.executeUpdate();
            System.out.println("✅ Consommation modifiée avec succès");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la modification : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Supprime une consommation par ID
     *
     * @param id L'ID de la consommation à supprimer
     */
    public void supprimer(int id) {
        String qry = "DELETE FROM `consommation` WHERE `id_consommation` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
            System.out.println("✅ Consommation supprimée avec succès");

        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
        }
    }
}