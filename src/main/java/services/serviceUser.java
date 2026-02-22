package services;

import models.*;
import interfaces.services;
import utils.MyDataBase;
import utils.PasswordUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class serviceUser implements services<user> {
    private Connection cnx;

    // Dossier de stockage des images (à la racine du projet)
    private static final String IMAGE_DIR = "user_images/";

    public serviceUser() {
        this.cnx = MyDataBase.getInstance().getCnx();
        // Créer le dossier images s'il n'existe pas
        new File(IMAGE_DIR).mkdirs();
    }

    // ==================== GESTION IMAGE ====================

    public String saveUserImage(String sourceImagePath) {
        if (sourceImagePath == null || sourceImagePath.isEmpty()) return null;
        try {
            File sourceFile = new File(sourceImagePath);
            if (!sourceFile.exists()) return null;
            String extension = "";
            int dotIndex = sourceFile.getName().lastIndexOf('.');
            if (dotIndex >= 0) extension = sourceFile.getName().substring(dotIndex);
            String uniqueName = UUID.randomUUID().toString() + extension;
            Path destPath = Paths.get(IMAGE_DIR + uniqueName);
            Files.copy(sourceFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
            return IMAGE_DIR + uniqueName;
        } catch (IOException e) {
            System.err.println("Erreur lors de la copie de l'image: " + e.getMessage());
            return null;
        }
    }

    public void deleteUserImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return;
        try { new File(imagePath).delete(); } catch (Exception e) {
            System.err.println("Erreur suppression image: " + e.getMessage());
        }
    }

    // ==================== ADD ====================

    @Override
    public void add(user user) { add(user, null); }

    public void add(user user, String sourceImagePath) {
        String hashedPassword = PasswordUtils.hashPassword(user.getUser_password());
        String savedImagePath = saveUserImage(sourceImagePath);

        String req = "INSERT INTO `user`(" +
                "`user_nom`, `user_prenom`, `user_email`, `user_password`, " +
                "`user_date_de_naissance`, `date_inscription`, `type_utilisateur`, " +
                "`user_sexe`, `user_poids`, `user_taille`, `user_niveau_activite_physique`, " +
                "`user_niveau_scolaire`, `user_etablissement_scolaire`, `user_image_path`, `is_active`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)";

        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, user.getUser_nom());
            pstm.setString(2, user.getUser_prenom());
            pstm.setString(3, user.getUser_email());
            pstm.setString(4, hashedPassword);
            pstm.setString(5, user.getUser_date_de_naissance());
            pstm.setString(6, user.getDate_inscription());
            pstm.setString(7, user.getType_utilisateur().name());
            pstm.setString(8, user.getUser_sexe() != null ? user.getUser_sexe().name() : null);
            if (user.getUser_poids() != null) pstm.setDouble(9, user.getUser_poids());
            else pstm.setNull(9, Types.DECIMAL);
            if (user.getUser_taille() != null) pstm.setInt(10, user.getUser_taille());
            else pstm.setNull(10, Types.INTEGER);
            pstm.setString(11, user.getUser_niveau_activite_physique() != null ? user.getUser_niveau_activite_physique().name() : null);
            pstm.setString(12, user.getUser_niveau_scolaire() != null ? user.getUser_niveau_scolaire().name() : null);
            pstm.setString(13, user.getUser_etablissement_scolaire());
            pstm.setString(14, savedImagePath);

            pstm.executeUpdate();
            System.out.println("Utilisateur ajouté avec succès !");
        } catch (SQLException e) {
            if (savedImagePath != null) deleteUserImage(savedImagePath);
            System.err.println("Erreur lors de l'ajout : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== GET ALL (actifs uniquement) ====================

    @Override
    public List<user> getAll() {
        return getAllByStatus(true);
    }

    /**
     * Récupère les utilisateurs archivés (is_active = 0).
     */
    public List<user> getArchived() {
        return getAllByStatus(false);
    }

    private List<user> getAllByStatus(boolean activeStatus) {
        List<user> users = new ArrayList<>();
        String req = "SELECT * FROM `user` WHERE `is_active` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, activeStatus ? 1 : 0);
            ResultSet rs = pstm.executeQuery();
            while (rs.next()) users.add(mapResultSetToUser(rs));
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération : " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    // ==================== ARCHIVAGE (remplace deleteById) ====================

    /**
     * Archive un utilisateur (is_active = 0) sans le supprimer de la BDD.
     * Supprime également sa session active pour le déconnecter immédiatement.
     */
    public void archiveById(int id) {
        // 1. Invalider les sessions actives de cet utilisateur
        String deleteSessionsReq = "DELETE FROM sessions WHERE user_id = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(deleteSessionsReq)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur suppression sessions : " + e.getMessage());
        }

        // 2. Archiver l'utilisateur
        String req = "UPDATE `user` SET `is_active` = 0 WHERE `user_id` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
            System.out.println("Utilisateur archivé avec succès (ID=" + id + ")");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'archivage : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Réactive un utilisateur archivé (is_active = 1).
     */
    public void restoreById(int id) {
        String req = "UPDATE `user` SET `is_active` = 1 WHERE `user_id` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
            System.out.println("Utilisateur restauré avec succès (ID=" + id + ")");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la restauration : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Suppression définitive (réservé à l'admin pour cas extrêmes).
     */
    @Override
    public void deleteById(int id) {
        user u = getOneById(id);
        if (u != null && u.getUser_image_path() != null) deleteUserImage(u.getUser_image_path());

        String req = "DELETE FROM `user` WHERE `user_id` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
            System.out.println("Utilisateur supprimé définitivement (ID=" + id + ")");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== UPDATE ====================

    @Override
    public void updateById(int id, String nom, String prenom, String email, String password,
                           String dateNaissance, String dateInscription, Role role,
                           Sexe sexe, Double poids, Integer taille,
                           NiveauActivitePhysique niveauActivite, NiveauScolaire niveauScolaire,
                           String etablissement) {
        updateById(id, nom, prenom, email, password, dateNaissance, dateInscription, role,
                sexe, poids, taille, niveauActivite, niveauScolaire, etablissement, null, false);
    }

    public void updateById(int id, String nom, String prenom, String email, String password,
                           String dateNaissance, String dateInscription, Role role,
                           Sexe sexe, Double poids, Integer taille,
                           NiveauActivitePhysique niveauActivite, NiveauScolaire niveauScolaire,
                           String etablissement, String newImageSourcePath, boolean removeImage) {

        // Gérer l'image
        user existing = getOneById(id);
        String oldImagePath = (existing != null) ? existing.getUser_image_path() : null;
        String finalImagePath;

        if (removeImage) {
            if (oldImagePath != null) deleteUserImage(oldImagePath);
            finalImagePath = null;
        } else if (newImageSourcePath != null) {
            String newSavedPath = saveUserImage(newImageSourcePath);
            if (oldImagePath != null) deleteUserImage(oldImagePath);
            finalImagePath = newSavedPath;
        } else {
            finalImagePath = oldImagePath;
        }

        String req = "UPDATE `user` SET " +
                "`user_nom` = ?, `user_prenom` = ?, `user_email` = ?, " +
                "`user_date_de_naissance` = ?, `date_inscription` = ?, `type_utilisateur` = ?, " +
                "`user_sexe` = ?, `user_poids` = ?, `user_taille` = ?, " +
                "`user_niveau_activite_physique` = ?, `user_niveau_scolaire` = ?, " +
                "`user_etablissement_scolaire` = ?, `user_image_path` = ? " +
                "WHERE `user_id` = ?";

        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, nom);
            pstm.setString(2, prenom);
            pstm.setString(3, email);
            pstm.setString(4, dateNaissance);
            pstm.setString(5, dateInscription);
            pstm.setString(6, role.name());
            pstm.setString(7, sexe != null ? sexe.name() : null);
            if (poids != null) pstm.setDouble(8, poids); else pstm.setNull(8, Types.DECIMAL);
            if (taille != null) pstm.setInt(9, taille); else pstm.setNull(9, Types.INTEGER);
            pstm.setString(10, niveauActivite != null ? niveauActivite.name() : null);
            pstm.setString(11, niveauScolaire != null ? niveauScolaire.name() : null);
            pstm.setString(12, etablissement);
            pstm.setString(13, finalImagePath);
            pstm.setInt(14, id);

            pstm.executeUpdate();
            System.out.println("Utilisateur mis à jour avec succès !");
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== GET ONE ====================

    @Override
    public user getOneById(int id) {
        // Recherche dans tous les users (actifs ET archivés) pour les opérations internes
        String req = "SELECT * FROM `user` WHERE `user_id` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) return mapResultSetToUser(rs);
        } catch (SQLException e) {
            System.err.println("Erreur getOneById : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // ==================== LOGIN : vérifie actif + mot de passe ====================

    public user getByEmailAndPassword(String email, String plainPassword) {
        // On sélectionne uniquement les utilisateurs ACTIFS
        String req = "SELECT * FROM `user` WHERE `user_email` = ? AND `is_active` = 1";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, email);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                String storedHashedPassword = rs.getString("user_password");
                if (PasswordUtils.checkPassword(plainPassword, storedHashedPassword)) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Vérifie si un email existe mais est archivé (pour afficher un message adapté au login).
     */
    public boolean isEmailArchived(String email) {
        String req = "SELECT COUNT(*) FROM `user` WHERE `user_email` = ? AND `is_active` = 0";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, email);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ==================== MAPPING ResultSet -> user ====================

    private user mapResultSetToUser(ResultSet rs) throws SQLException {
        user u = new user();
        u.setUser_id(rs.getInt("user_id"));
        u.setUser_nom(rs.getString("user_nom"));
        u.setUser_prenom(rs.getString("user_prenom"));
        u.setUser_email(rs.getString("user_email"));
        u.setUser_password(rs.getString("user_password"));
        u.setUser_date_de_naissance(rs.getString("user_date_de_naissance"));
        u.setDate_inscription(rs.getString("date_inscription"));
        u.setType_utilisateur(Role.valueOf(rs.getString("type_utilisateur")));

        String sexeStr = rs.getString("user_sexe");
        if (sexeStr != null && !sexeStr.isEmpty()) {
            try { u.setUser_sexe(Sexe.valueOf(sexeStr)); } catch (IllegalArgumentException ignored) {}
        }
        u.setUser_poids(rs.getObject("user_poids") != null ? rs.getDouble("user_poids") : null);
        u.setUser_taille(rs.getObject("user_taille") != null ? rs.getInt("user_taille") : null);

        String activiteStr = rs.getString("user_niveau_activite_physique");
        if (activiteStr != null && !activiteStr.isEmpty()) {
            try { u.setUser_niveau_activite_physique(NiveauActivitePhysique.valueOf(activiteStr)); } catch (IllegalArgumentException ignored) {}
        }
        String scolaireStr = rs.getString("user_niveau_scolaire");
        if (scolaireStr != null && !scolaireStr.isEmpty()) {
            try { u.setUser_niveau_scolaire(NiveauScolaire.valueOf(scolaireStr)); } catch (IllegalArgumentException ignored) {}
        }
        u.setUser_etablissement_scolaire(rs.getString("user_etablissement_scolaire"));
        u.setUser_image_path(rs.getString("user_image_path"));

        // is_active : récupération robuste (colonne peut ne pas exister sur ancienne BDD)
        try {
            u.setIs_active(rs.getInt("is_active") == 1);
        } catch (SQLException e) {
            u.setIs_active(true); // fallback sécurisé
        }

        return u;
    }
}
