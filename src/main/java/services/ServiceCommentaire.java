package services;

import interfaces.Services;
import models.Commentaire;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


import java.util.List;

public class ServiceCommentaire implements Services<Commentaire> {

    Connection cnx = MyDataBase.getInstance().getCnx();






//    public boolean postExiste(int idPost){
//
//        String req = "SELECT id_post FROM post WHERE id_post = ?";
//
//        try{
//            PreparedStatement ps = cnx.prepareStatement(req);
//            ps.setInt(1, idPost);
//
//            ResultSet rs = ps.executeQuery();
//
//            return rs.next();
//
//        }catch(SQLException e){
//            System.out.println(e.getMessage());
//        }
//
//        return false;
//    }




    @Override
    public void add(Commentaire commentaire) {

        String req = "INSERT INTO commentaire (contenu, date_commentaire, id_post, id_etudiant) VALUES (?, ?, ?, ?)";

        try {

            PreparedStatement ps = cnx.prepareStatement(req);

            ps.setString(1, commentaire.getContenu());


            ps.setTimestamp(2, Timestamp.valueOf(commentaire.getDateCommentaire()));
//            ps.setInt(3, commentaire.getIdEtudiant());
//            ps.setInt(4, commentaire.getIdPost());
            ps.setInt(3, commentaire.getIdPost());       // ✅ BON
            ps.setInt(4, commentaire.getIdEtudiant());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(Commentaire commentaire) {

        String req = "UPDATE commentaire SET contenu=? WHERE id_commentaire=?";

        try {

            PreparedStatement ps = cnx.prepareStatement(req);

            ps.setString(1, commentaire.getContenu());
            ps.setInt(2, commentaire.getIdCommentaire());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(Commentaire commentaire) {

        String req = "DELETE FROM commentaire WHERE id_commentaire=?";

        try {

            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, commentaire.getIdCommentaire());

            ps.executeUpdate();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<Commentaire> getAll() {

        List<Commentaire> commentaires = new ArrayList<>();
        String req = "SELECT * FROM commentaire";

        try {

            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);

            while (rs.next()) {

                Commentaire c = new Commentaire();

                c.setIdCommentaire(rs.getInt("id_commentaire"));
                c.setContenu(rs.getString("contenu"));
                c.setIdEtudiant(rs.getInt("id_etudiant"));
                c.setIdPost(rs.getInt("id_post"));

                Timestamp ts = rs.getTimestamp("date_commentaire");
                if (ts != null)
                    c.setDateCommentaire(ts.toLocalDateTime());

                commentaires.add(c);
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return commentaires;
    }
}
