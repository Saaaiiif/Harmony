package services.ForumServices;

import utils.MyDataBase;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service statistiques du forum
 * Colonnes réelles :
 *   post        : id_post, titre, contenu, date_creation, user_id, id_categorie
 *   commentaire : id_commentaire, contenu, date_commentaire, id_post, user_id
 *   categorie   : id_categorie, nom_categorie, description, date_creation
 *   user        : user_id, user_nom, user_prenom
 */
public class ForumStatsService {

    private Connection cnx = MyDataBase.getInstance().getCnx();

    // ── KPIs ─────────────────────────────────────────────────────────────────

    public int getTotalPosts() {
        return queryCount("SELECT COUNT(*) FROM post");
    }

    public int getTotalCommentaires() {
        return queryCount("SELECT COUNT(*) FROM commentaire");
    }

    public int getTotalCategories() {
        return queryCount("SELECT COUNT(*) FROM categorie");
    }

    public int getAuteursActifs() {
        return queryCount("SELECT COUNT(DISTINCT user_id) FROM post");
    }

    // ── Posts par catégorie — PieChart ────────────────────────────────────────
    public Map<String, Integer> getPostsParCategorie() {
        Map<String, Integer> data = new LinkedHashMap<>();
        String sql =
                "SELECT c.nom_categorie, COUNT(p.id_post) AS nb " +
                        "FROM categorie c " +
                        "LEFT JOIN post p ON c.id_categorie = p.id_categorie " +
                        "GROUP BY c.id_categorie, c.nom_categorie " +
                        "ORDER BY nb DESC";
        try {
            ResultSet rs = cnx.createStatement().executeQuery(sql);
            while (rs.next()) {
                data.put(rs.getString("nom_categorie"), rs.getInt("nb"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return data;
    }

    // ── Posts par jour (7 derniers jours) — LineChart ─────────────────────────
    public Map<String, Integer> getPostsParJour() {
        Map<String, Integer> data = new LinkedHashMap<>();
        String sql =
                "SELECT DATE(date_creation) AS jour, COUNT(*) AS nb " +
                        "FROM post " +
                        "WHERE date_creation >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
                        "GROUP BY DATE(date_creation) " +
                        "ORDER BY jour ASC";
        try {
            ResultSet rs = cnx.createStatement().executeQuery(sql);
            while (rs.next()) {
                // Format court : MM-DD
                String jour = rs.getString("jour");
                if (jour != null && jour.length() == 10)
                    jour = jour.substring(5); // "2026-02-20" → "02-20"
                data.put(jour, rs.getInt("nb"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return data;
    }

    // ── Top 5 auteurs — BarChart ──────────────────────────────────────────────
    public Map<String, Integer> getTopAuteurs() {
        Map<String, Integer> data = new LinkedHashMap<>();
        String sql =
                "SELECT CONCAT(u.user_prenom, ' ', u.user_nom) AS auteur, " +
                        "       COUNT(p.id_post) AS nb " +
                        "FROM post p " +
                        "JOIN user u ON p.user_id = u.user_id " +
                        "GROUP BY p.user_id, u.user_nom, u.user_prenom " +
                        "ORDER BY nb DESC " +
                        "LIMIT 5";
        try {
            ResultSet rs = cnx.createStatement().executeQuery(sql);
            while (rs.next()) {
                data.put(rs.getString("auteur"), rs.getInt("nb"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return data;
    }

    // ── Top 5 posts les plus commentés — BarChart ─────────────────────────────
    public Map<String, Integer> getCommentairesParPost() {
        Map<String, Integer> data = new LinkedHashMap<>();
        String sql =
                "SELECT p.titre, COUNT(c.id_commentaire) AS nb " +
                        "FROM post p " +
                        "LEFT JOIN commentaire c ON p.id_post = c.id_post " +
                        "GROUP BY p.id_post, p.titre " +
                        "ORDER BY nb DESC " +
                        "LIMIT 5";
        try {
            ResultSet rs = cnx.createStatement().executeQuery(sql);
            while (rs.next()) {
                String titre = rs.getString("titre");
                if (titre != null && titre.length() > 20)
                    titre = titre.substring(0, 20) + "…";
                data.put(titre, rs.getInt("nb"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return data;
    }

    // ── Helper ────────────────────────────────────────────────────────────────
    private int queryCount(String sql) {
        try {
            ResultSet rs = cnx.createStatement().executeQuery(sql);
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}
