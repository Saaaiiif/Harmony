package services;

import models.user;
import models.Role;
import models.Sexe;
import models.NiveauActivitePhysique;
import models.NiveauScolaire;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class SessionDAO {
    private Connection cnx;

    public SessionDAO() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    public void createSession(int userId, String token, LocalDateTime expiresAt) {
        String req = "INSERT INTO sessions (user_id, token, expires_at) VALUES (?, ?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, userId);
            pstm.setString(2, token);
            pstm.setTimestamp(3, Timestamp.valueOf(expiresAt));
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isTokenValid(String token) {
        String req = "SELECT expires_at FROM sessions WHERE token = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, token);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                LocalDateTime expiresAt = rs.getTimestamp("expires_at").toLocalDateTime();
                return LocalDateTime.now().isBefore(expiresAt);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Récupère l'utilisateur associé à un token de session valide.
     *
     * ✅ CORRECTION : ajout de user_image_path et face_image_path
     * qui étaient manquants et causaient la disparition de l'image
     * lors de la reconnexion automatique (remember-me).
     */
    public Optional<user> getUserByToken(String token) {
        String req = "SELECT u.* FROM user u " +
                     "JOIN sessions s ON u.user_id = s.user_id " +
                     "WHERE s.token = ? AND s.expires_at > NOW()";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, token);
            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
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

                // Poids / Taille avec null-check (évite NPE si colonne vide)
                Object poidsObj = rs.getObject("user_poids");
                if (poidsObj != null) u.setUser_poids(rs.getDouble("user_poids"));

                Object tailleObj = rs.getObject("user_taille");
                if (tailleObj != null) u.setUser_taille(rs.getInt("user_taille"));

                String activiteStr = rs.getString("user_niveau_activite_physique");
                if (activiteStr != null && !activiteStr.isEmpty()) {
                    try { u.setUser_niveau_activite_physique(NiveauActivitePhysique.valueOf(activiteStr)); }
                    catch (IllegalArgumentException ignored) {}
                }

                String scolaireStr = rs.getString("user_niveau_scolaire");
                if (scolaireStr != null && !scolaireStr.isEmpty()) {
                    try { u.setUser_niveau_scolaire(NiveauScolaire.valueOf(scolaireStr)); }
                    catch (IllegalArgumentException ignored) {}
                }

                u.setUser_etablissement_scolaire(rs.getString("user_etablissement_scolaire"));

                // ✅ CORRECTION PRINCIPALE : ces deux champs étaient absents
                u.setUser_image_path(rs.getString("user_image_path"));

                try {
                    u.setFace_image_path(rs.getString("face_image_path"));
                } catch (SQLException e) {
                    u.setFace_image_path(null); // fallback si colonne absente
                }

                try {
                    u.setIs_active(rs.getInt("is_active") == 1);
                } catch (SQLException e) {
                    u.setIs_active(true);
                }

                return Optional.of(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public void deleteSession(String token) {
        String req = "DELETE FROM sessions WHERE token = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, token);
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteSessionsByUser(int userId) {
        String req = "DELETE FROM sessions WHERE user_id = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, userId);
            pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
