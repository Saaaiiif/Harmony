package services.UserServices;

import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Service métier pour la gestion des codes de réinitialisation de mot de passe.
 * Utilise la table `password_reset_codes` (voir migration_password_reset.sql).
 */
public class PasswordResetService {

    private final Connection cnx;

    public PasswordResetService() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    /**
     * Vérifie si un email appartient à un utilisateur actif dans la BDD.
     */
    public boolean emailExiste(String email) {
        String req = "SELECT COUNT(*) FROM `user` WHERE `user_email` = ? AND `is_active` = 1";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, email);
            ResultSet rs = pstm.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Erreur emailExiste : " + e.getMessage());
            return false;
        }
    }

    /**
     * Génère un code à 6 chiffres, invalide les anciens codes pour cet email,
     * et sauvegarde le nouveau code avec une expiration de 15 minutes.
     *
     * @param email Email de l'utilisateur
     * @return Le code généré (à envoyer par email)
     */
    public String genererEtSauvegarderCode(String email) {
        // Générer un code aléatoire à 6 chiffres
        String code = String.format("%06d", new Random().nextInt(1_000_000));

        // Invalider tous les anciens codes non utilisés pour cet email
        String deleteOld = "DELETE FROM `password_reset_codes` WHERE `email` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(deleteOld)) {
            pstm.setString(1, email);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur suppression anciens codes : " + e.getMessage());
        }

        // Insérer le nouveau code avec expiration = maintenant + 15 min
        LocalDateTime expiration = LocalDateTime.now().plusMinutes(15);
        String insert = "INSERT INTO `password_reset_codes` (`email`, `code`, `expires_at`, `used`) VALUES (?, ?, ?, 0)";
        try (PreparedStatement pstm = cnx.prepareStatement(insert)) {
            pstm.setString(1, email);
            pstm.setString(2, code);
            pstm.setTimestamp(3, Timestamp.valueOf(expiration));
            pstm.executeUpdate();
            System.out.println("Code de reset généré pour " + email + " : " + code);
        } catch (SQLException e) {
            System.err.println("Erreur insertion code reset : " + e.getMessage());
        }

        return code;
    }

    /**
     * Vérifie si le code fourni est valide pour cet email :
     *  - Code non utilisé
     *  - Pas encore expiré
     *
     * Si le code est correct, il est marqué comme utilisé immédiatement.
     *
     * @param email Email de l'utilisateur
     * @param code  Code saisi par l'utilisateur
     * @return true si le code est valide
     */
    public boolean verifierCode(String email, String code) {
        String req = "SELECT `id` FROM `password_reset_codes` " +
                "WHERE `email` = ? AND `code` = ? AND `used` = 0 AND `expires_at` > ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, email);
            pstm.setString(2, code);
            pstm.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id");
                // Marquer le code comme utilisé
                marquerCodeUtilise(id);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur vérification code : " + e.getMessage());
        }
        return false;
    }

    /**
     * Met à jour le mot de passe (déjà hashé) de l'utilisateur dans la BDD.
     *
     * @param email          Email de l'utilisateur
     * @param hashedPassword Nouveau mot de passe hashé (BCrypt)
     * @return true si la mise à jour a réussi
     */
    public boolean mettreAJourMotDePasse(String email, String hashedPassword) {
        String req = "UPDATE `user` SET `user_password` = ? WHERE `user_email` = ? AND `is_active` = 1";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, hashedPassword);
            pstm.setString(2, email);
            int rows = pstm.executeUpdate();
            if (rows > 0) {
                System.out.println("Mot de passe mis à jour pour : " + email);
                // Nettoyer les codes restants pour cet email
                supprimerCodes(email);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur mise à jour MDP : " + e.getMessage());
        }
        return false;
    }

    // ========== Méthodes privées ==========

    private void marquerCodeUtilise(int codeId) {
        String req = "UPDATE `password_reset_codes` SET `used` = 1 WHERE `id` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, codeId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur marquer code utilisé : " + e.getMessage());
        }
    }

    private void supprimerCodes(String email) {
        String req = "DELETE FROM `password_reset_codes` WHERE `email` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, email);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur suppression codes : " + e.getMessage());
        }
    }
}
