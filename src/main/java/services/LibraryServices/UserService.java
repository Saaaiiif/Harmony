package services.LibraryServices;

import com.example.harmony.DB;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    public record UserRow(
            int id,
            String nom,
            String prenom,
            String email,
            String imagePath,
            String typeUtilisateur,
            boolean isActive
    ) {
        public String fullName() { return prenom + " " + nom; }
    }

    public List<UserRow> listUsers() throws Exception {
        List<UserRow> out = new ArrayList<>();
        String sql = "SELECT user_id, user_nom, user_prenom, user_email, user_image_path, type_utilisateur, is_active FROM user WHERE is_active = 1 ORDER BY user_prenom";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new UserRow(
                        rs.getInt("user_id"),
                        rs.getString("user_nom"),
                        rs.getString("user_prenom"),
                        rs.getString("user_email"),
                        rs.getString("user_image_path"),
                        rs.getString("type_utilisateur"),
                        rs.getBoolean("is_active")
                ));
            }
        }
        return out;
    }

    public UserRow createUser(String nom, String prenom, String email) throws Exception {
        String sql = "INSERT INTO user (user_nom, user_prenom, user_email, user_password, user_date_de_naissance) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nom);
            ps.setString(2, prenom);
            ps.setString(3, email);
            ps.setString(4, "temp_password");   // placeholder until auth is done
            ps.setString(5, "2000-01-01");       // placeholder DOB
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return new UserRow(keys.getInt(1), nom, prenom, email, null, "ETUDIANT", true);
                }
            }
        }
        throw new Exception("User creation failed");
    }
}
