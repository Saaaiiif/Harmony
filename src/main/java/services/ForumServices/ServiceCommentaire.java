package services.ForumServices;

import interfaces.ForumServices;
import models.ForumModels.Commentaire;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceCommentaire implements ForumServices<Commentaire> {

    Connection cnx = MyDataBase.getInstance().getCnx();

    // ─── Ajout ────────────────────────────────────────────────────────────────
    @Override
    public void add(Commentaire commentaire) {
        String req =
                "INSERT INTO commentaire (contenu, date_commentaire, id_post, user_id) " +
                        "VALUES (?, ?, ?, ?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, commentaire.getContenu());
            ps.setTimestamp(2, Timestamp.valueOf(commentaire.getDateCommentaire()));
            ps.setInt(3, commentaire.getIdPost());
            ps.setInt(4, commentaire.getIdEtudiant()); // contient user_id depuis la session
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ─── Commentaires d'un post avec JOIN user ────────────────────────────────
    public List<Commentaire> getCommentairesByPost(int idPost) {
        List<Commentaire> commentaires = new ArrayList<>();
        String req =
                "SELECT c.*, " +
                        "       u.user_nom, u.user_prenom, u.user_image_path " +
                        "FROM commentaire c " +
                        "JOIN user u ON c.user_id = u.user_id " +
                        "WHERE c.id_post = ? " +
                        "ORDER BY c.date_commentaire ASC";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, idPost);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                commentaires.add(mapCommentaire(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return commentaires;
    }

    // ─── Tous les commentaires avec JOIN user ─────────────────────────────────
    @Override
    public List<Commentaire> getAll() {
        List<Commentaire> commentaires = new ArrayList<>();
        String req =
                "SELECT c.*, p.titre, " +
                        "       u.user_nom, u.user_prenom, u.user_image_path " +
                        "FROM commentaire c " +
                        "JOIN post p ON c.id_post = p.id_post " +
                        "JOIN user u ON c.user_id = u.user_id";
        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()) {
                Commentaire c = mapCommentaire(rs);
                try { c.setTitrePost(rs.getString("titre")); } catch (Exception ignored) {}
                commentaires.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return commentaires;
    }

    // ─── Update ───────────────────────────────────────────────────────────────
    @Override
    public void update(Commentaire commentaire) {
        String req = "UPDATE commentaire SET contenu=? WHERE id_commentaire=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setString(1, commentaire.getContenu());
            ps.setInt(2, commentaire.getIdCommentaire());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────
    @Override
    public void delete(Commentaire commentaire) {
        String req = "DELETE FROM commentaire WHERE id_commentaire=?";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, commentaire.getIdCommentaire());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ─── Mapper ResultSet → Commentaire ──────────────────────────────────────
    private Commentaire mapCommentaire(ResultSet rs) throws SQLException {
        Commentaire c = new Commentaire();
        c.setIdCommentaire(rs.getInt("id_commentaire"));
        c.setContenu(rs.getString("contenu"));
        c.setIdPost(rs.getInt("id_post"));
        c.setIdEtudiant(rs.getInt("user_id"));

        String nomComplet = rs.getString("user_prenom") + " " + rs.getString("user_nom");
        c.setNomEtudiant(nomComplet);
        c.setAuteurImagePath(rs.getString("user_image_path"));

        Timestamp ts = rs.getTimestamp("date_commentaire");
        if (ts != null) c.setDateCommentaire(ts.toLocalDateTime());

        return c;
    }
}
