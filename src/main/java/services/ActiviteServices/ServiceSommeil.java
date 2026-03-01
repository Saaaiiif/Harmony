package services.ActiviteServices;

import models.ActiviteModels.Sommeil;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * SERVICE SOMMEIL - GESTION DES NUITS DE SOMMEIL
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * ✅ VERSION COMPLÈTE ET CORRIGÉE
 *
 * Responsabilités :
 *  - Ajouter une nuit de sommeil
 *  - Afficher tous les enregistrements de sommeil
 *  - Modifier une nuit de sommeil
 *  - Supprimer une nuit de sommeil
 *
 * Correction critique :
 *  ❌ AVANT : Les positions des paramètres PreparedStatement étaient mal alignées
 *  ✅ APRÈS : Positions correctes (1-7) avec user_id inclus
 * ═══════════════════════════════════════════════════════════════════════════
 */
public class ServiceSommeil {

    private Connection cnx;

    /**
     * Constructeur - Initialise la connexion à la BD
     */
    public ServiceSommeil() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    /**
     * Ajoute une nuit de sommeil dans la base de données
     *
     * ✅ CORRIGÉ : user_id maintenant géré correctement
     *    Position 1: user_id
     *    Position 2: date_coucher
     *    Position 3: date_reveil
     *    Position 4: qualite_sommeil
     *    Position 5: stress
     *    Position 6: cafeine
     *    Position 7: bruit
     *
     * @param s Objet Sommeil à ajouter
     * @return true si l'insertion a réussi, false sinon
     */
    public boolean ajouter(Sommeil s) {
        String qry = "INSERT INTO `sommeil` (`user_id`, `date_coucher`, `date_reveil`, `qualite_sommeil`, `stress`, `cafeine`, `bruit`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            // Position 1: user_id
            pstm.setInt(1, s.getUser_id());
            // Position 2: date_coucher
            pstm.setTimestamp(2, s.getDate_coucher());
            // Position 3: date_reveil
            pstm.setTimestamp(3, s.getDate_reveil());
            // Position 4: qualite_sommeil
            pstm.setString(4, s.getQualite_sommeil());
            // Position 5: stress
            pstm.setBoolean(5, s.isStress());
            // Position 6: cafeine
            pstm.setBoolean(6, s.isCafeine());
            // Position 7: bruit
            pstm.setBoolean(7, s.isBruit());

            int rows = pstm.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Sommeil ajouté avec succès (user_id=" + s.getUser_id() + ")");
            }
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("❌ ERREUR SQL AJOUT SOMMEIL : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Récupère tous les enregistrements de sommeil de la base de données
     * Triés par date_coucher (plus récents en premier)
     *
     * @return Liste de tous les enregistrements Sommeil
     */
    public List<Sommeil> afficherTout() {
        List<Sommeil> liste = new ArrayList<>();
        String qry = "SELECT * FROM `sommeil` ORDER BY `date_coucher` DESC";

        try (Statement stm = cnx.createStatement();
             ResultSet rs = stm.executeQuery(qry)) {

            while (rs.next()) {
                // ✅ CORRECTION : utiliser le constructeur 8-args (id_sommeil, user_id, ...)
                // L'ancien code utilisait le constructeur 7-args dont le 1er int → user_id,
                // laissant id_sommeil=0 (même bug que Consommation et Activite)
                Sommeil s = new Sommeil(
                        rs.getInt("id_sommeil"),
                        rs.getInt("user_id"),
                        rs.getTimestamp("date_coucher"),
                        rs.getTimestamp("date_reveil"),
                        rs.getString("qualite_sommeil"),
                        rs.getBoolean("stress"),
                        rs.getBoolean("cafeine"),
                        rs.getBoolean("bruit")
                );
                liste.add(s);
            }

        } catch (SQLException e) {
            System.err.println("❌ ERREUR AFFICHER SOMMEIL : " + e.getMessage());
            e.printStackTrace();
        }
        return liste;
    }

    /**
     * Modifie un enregistrement de sommeil existant
     *
     * @param s Objet Sommeil avec les nouvelles valeurs (id_sommeil doit être défini)
     * @return true si la mise à jour a réussi, false sinon
     */
    public boolean modifier(Sommeil s) {
        String qry = "UPDATE `sommeil` " +
                "SET `date_coucher` = ?, `date_reveil` = ?, `qualite_sommeil` = ?, `stress` = ?, `cafeine` = ?, `bruit` = ? " +
                "WHERE `id_sommeil` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setTimestamp(1, s.getDate_coucher());
            pstm.setTimestamp(2, s.getDate_reveil());
            pstm.setString(3, s.getQualite_sommeil());
            pstm.setBoolean(4, s.isStress());
            pstm.setBoolean(5, s.isCafeine());
            pstm.setBoolean(6, s.isBruit());
            pstm.setInt(7, s.getId_sommeil());

            int rows = pstm.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Sommeil modifié avec succès (id=" + s.getId_sommeil() + ")");
            }
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("❌ ERREUR SQL MODIF SOMMEIL : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Supprime un enregistrement de sommeil par ID
     *
     * @param id_sommeil L'ID du sommeil à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public boolean supprimer(int id_sommeil) {
        String qry = "DELETE FROM `sommeil` WHERE `id_sommeil` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id_sommeil);
            int rows = pstm.executeUpdate();

            if (rows > 0) {
                System.out.println("✅ Sommeil supprimé avec succès (id=" + id_sommeil + ")");
            }
            return rows > 0;

        } catch (SQLException e) {
            System.err.println("❌ ERREUR SQL SUPPR SOMMEIL : " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Récupère les enregistrements de sommeil d'un utilisateur spécifique
     *
     * @param user_id L'ID de l'utilisateur
     * @return Liste des enregistrements Sommeil pour cet utilisateur
     */
    public List<Sommeil> afficherParUtilisateur(int user_id) {
        List<Sommeil> liste = new ArrayList<>();
        String qry = "SELECT * FROM `sommeil` WHERE `user_id` = ? ORDER BY `date_coucher` DESC";

        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, user_id);
            try (ResultSet rs = pstm.executeQuery()) {

                while (rs.next()) {
                    // ✅ CORRECTION : même fix que afficherTout()
                    Sommeil s = new Sommeil(
                            rs.getInt("id_sommeil"),
                            rs.getInt("user_id"),
                            rs.getTimestamp("date_coucher"),
                            rs.getTimestamp("date_reveil"),
                            rs.getString("qualite_sommeil"),
                            rs.getBoolean("stress"),
                            rs.getBoolean("cafeine"),
                            rs.getBoolean("bruit")
                    );
                    liste.add(s);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ ERREUR AFFICHER SOMMEIL PAR UTILISATEUR : " + e.getMessage());
            e.printStackTrace();
        }
        return liste;
    }
}