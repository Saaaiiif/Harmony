package services.ForumServices;

import interfaces.ForumServices;
import models.ForumModels.Post;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicePost implements ForumServices<Post> {

    Connection cnx = MyDataBase.getInstance().getCnx();

    // ─── Récupérer tous les posts avec JOIN user ───────────────────────────────
    @Override
    public List<Post> getAll() {
        List<Post> posts = new ArrayList<>();
        String req =
                "SELECT p.*, c.nom_categorie, " +
                        "       u.user_nom, u.user_prenom, u.user_image_path " +
                        "FROM post p " +
                        "JOIN categorie c ON p.id_categorie = c.id_categorie " +
                        "JOIN user u ON p.user_id = u.user_id";

        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                posts.add(mapPost(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }

    // ─── Posts par catégorie avec JOIN user ───────────────────────────────────
    public List<Post> getPostsByCategorie(int idCategorie) {
        List<Post> posts = new ArrayList<>();
        String req =
                "SELECT p.*, " +
                        "       u.user_nom, u.user_prenom, u.user_image_path " +
                        "FROM post p " +
                        "JOIN user u ON p.user_id = u.user_id " +
                        "WHERE p.id_categorie = ? " +
                        "ORDER BY p.date_creation DESC";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, idCategorie);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                posts.add(mapPost(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }

    // ─── Recherche avec JOIN user ─────────────────────────────────────────────
    public List<Post> searchPosts(String keyword) {
        List<Post> posts = new ArrayList<>();
        String sql =
                "SELECT p.*, c.nom_categorie, " +
                        "       u.user_nom, u.user_prenom, u.user_image_path " +
                        "FROM post p " +
                        "JOIN categorie c ON p.id_categorie = c.id_categorie " +
                        "JOIN user u ON p.user_id = u.user_id " +
                        "WHERE LOWER(p.titre) LIKE ? " +
                        "   OR LOWER(p.contenu) LIKE ? " +
                        "   OR LOWER(c.nom_categorie) LIKE ?";

        try {
            PreparedStatement pst = cnx.prepareStatement(sql);
            String kw = "%" + keyword.toLowerCase() + "%";
            pst.setString(1, kw);
            pst.setString(2, kw);
            pst.setString(3, kw);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                posts.add(mapPost(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }

    // ─── Ajout ────────────────────────────────────────────────────────────────
    @Override
    public void add(Post post) {
        String req =
                "INSERT INTO post (titre, contenu, date_creation, user_id, id_categorie, image_path) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, post.getTitre());
            ps.setString(2, post.getContenu());
            ps.setTimestamp(3, Timestamp.valueOf(post.getDateCreation()));
            ps.setInt(4, post.getIdEtudiant()); // contient user_id depuis la session
            ps.setInt(5, post.getIdCategorie());
            ps.setString(6, post.getImagePath());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ─── Update ───────────────────────────────────────────────────────────────
    @Override
    public void update(Post post) {
        String req = "UPDATE post SET titre=?, contenu=? WHERE id_post=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, post.getTitre());
            ps.setString(2, post.getContenu());
            ps.setInt(3, post.getIdPost());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────
    @Override
    public void delete(Post post) {
        deleteById(post.getIdPost());
    }

    public void deleteById(int id) {
        String req = "DELETE FROM post WHERE id_post = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ─── Mapper ResultSet → Post ──────────────────────────────────────────────
    private Post mapPost(ResultSet rs) throws SQLException {
        Post p = new Post();
        p.setIdPost(rs.getInt("id_post"));
        p.setTitre(rs.getString("titre"));
        p.setContenu(rs.getString("contenu"));
        p.setIdCategorie(rs.getInt("id_categorie"));
        p.setIdEtudiant(rs.getInt("user_id"));

        try { p.setImagePath(rs.getString("image_path")); } catch (Exception ignored) {}
        try { p.setNomCategorie(rs.getString("nom_categorie")); } catch (Exception ignored) {}

        // Nom complet depuis user
        String nomComplet = rs.getString("user_prenom") + " " + rs.getString("user_nom");
        p.setNomEtudiant(nomComplet);

        // Image de profil de l'auteur
        p.setAuteurImagePath(rs.getString("user_image_path"));

        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) p.setDateCreation(ts.toLocalDateTime());

        return p;
    }
}
