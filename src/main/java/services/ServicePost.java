package services;

import interfaces.Services;
import models.Post;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ServicePost implements Services<Post> {

    Connection cnx = MyDataBase.getInstance().getCnx();

    public List<Post> searchPosts(String keyword) {
        List<Post> posts = new ArrayList<>();

        String sql = "SELECT p.*, c.nom_categorie AS categorie_nom " +
                "FROM post p " +
                "JOIN categorie c ON p.id_categorie = c.id_categorie " +
                "WHERE LOWER(p.titre) LIKE ? " +
                "OR LOWER(p.contenu) LIKE ? " +
                "OR LOWER(c.nom_categorie) LIKE ?";

        try (
             PreparedStatement pst = cnx.prepareStatement(sql)) {

            String searchKeyword = "%" + keyword.toLowerCase() + "%";

            pst.setString(1, searchKeyword);
            pst.setString(2, searchKeyword);
            pst.setString(3, searchKeyword);

            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Post p = new Post();
                p.setIdPost(rs.getInt("id_post")); // ✔ correct
                p.setTitre(rs.getString("titre"));
                p.setContenu(rs.getString("contenu"));
                // seulement si image_path existe
                try {
                    p.setImagePath(rs.getString("image_path"));
                } catch (Exception ignored) {}
                p.setNomCategorie(rs.getString("categorie_nom"));

                posts.add(p);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return posts;
    }


    @Override
    public void add(Post post) {

        String req = "INSERT INTO post (titre, contenu, date_creation, id_etudiant, id_categorie, image_path) VALUES (?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);

            ps.setString(1, post.getTitre());
            ps.setString(2, post.getContenu());
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(post.getDateCreation()));
           // ps.setTimestamp(3, Timestamp.valueOf(post.getDateCreation()));
            ps.setInt(4, post.getIdEtudiant());
            ps.setInt(5, post.getIdCategorie());
            ps.setString(6, post.getImagePath());


            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

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
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(Post post) {

        String req = "DELETE FROM post WHERE id_post=?";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, post.getIdPost());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public void deleteById(int id) {

        String req = "DELETE FROM post WHERE id_post = ?";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, id);

            int rowsDeleted = ps.executeUpdate();

            if (rowsDeleted > 0) {
                System.out.println("Post supprimé avec succès");
            } else {
                System.out.println("Aucun post trouvé avec cet ID");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    @Override
    public List<Post> getAll() {

        List<Post> posts = new ArrayList<>();
        String req = "SELECT p.*, c.nom_categorie, e.nom, e.prenom " +
                "FROM post p " +
                "JOIN categorie c ON p.id_categorie = c.id_categorie " +
                "JOIN etudiant e ON p.id_etudiant = e.id_etudiant" ;

        try {

            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {

                Post p = new Post();

                p.setIdPost(rs.getInt("id_post"));
                p.setTitre(rs.getString("titre"));
                p.setContenu(rs.getString("contenu"));
                p.setIdEtudiant(rs.getInt("id_etudiant"));
                p.setIdCategorie(rs.getInt("id_categorie"));


                // 🔥 IMPORTANT
                p.setNomCategorie(rs.getString("nom_categorie"));

                String nomComplet = rs.getString("nom") + " " + rs.getString("prenom");
                p.setNomEtudiant(nomComplet);

                Timestamp ts = rs.getTimestamp("date_creation");
                if (ts != null)
                    p.setDateCreation(ts.toLocalDateTime());

                posts.add(p);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return posts;
    }

    public List<Post> getPostsByCategorie(int idCategorie) {

        List<Post> posts = new ArrayList<>();

        String req = "SELECT p.*, e.nom, e.prenom " +
                "FROM post p " +
                "JOIN etudiant e ON p.id_etudiant = e.id_etudiant " +
                "WHERE p.id_categorie = ? " +
                "ORDER BY p.date_creation DESC";

        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, idCategorie);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Post p = new Post();

                p.setIdPost(rs.getInt("id_post"));
                p.setTitre(rs.getString("titre"));
                p.setContenu(rs.getString("contenu"));
                p.setIdCategorie(rs.getInt("id_categorie"));
                p.setImagePath(rs.getString("image_path"));

                p.setIdEtudiant(rs.getInt("id_etudiant"));

                String nomComplet = rs.getString("prenom") + " " + rs.getString("nom");
                p.setNomEtudiant(nomComplet);

                Timestamp ts = rs.getTimestamp("date_creation");
                if (ts != null) {
                    p.setDateCreation(ts.toLocalDateTime());
                }


                posts.add(p);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return posts;
    }



}



