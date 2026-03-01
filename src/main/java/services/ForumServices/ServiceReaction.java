package services.ForumServices;

import utils.MyDataBase;
import java.sql.*;

public class ServiceReaction {

    Connection cnx = MyDataBase.getInstance().getCnx();

    public int countLikes(int idPost) {
        String req = "SELECT COUNT(*) FROM reaction WHERE id_post=? AND type_reaction='LIKE'";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, idPost);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean isLiked(int idPost, int userId) {
        String req = "SELECT COUNT(*) FROM reaction WHERE id_post=? AND id_user=? AND type_reaction='LIKE'";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, idPost);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void likePost(int idPost, int userId) {
        String req = "INSERT INTO reaction (type_reaction, id_post, id_user) VALUES ('LIKE', ?, ?)";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, idPost);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void unlikePost(int idPost, int userId) {
        String req = "DELETE FROM reaction WHERE id_post=? AND id_user=? AND type_reaction='LIKE'";
        try {
            PreparedStatement ps = cnx.prepareStatement(req);
            ps.setInt(1, idPost);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
