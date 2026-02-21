package services;

import models.Role;
import models.User;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {

    private final Connection connection;

    public UserService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void add(User user) throws SQLException {
        String query = "INSERT INTO user (nom, prenom, email, password, date_naissance, date_inscription, role) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        pstmt.setString(1, user.getNom());
        pstmt.setString(2, user.getPrenom());
        pstmt.setString(3, user.getEmail());
        pstmt.setString(4, user.getPassword());
        pstmt.setDate(5, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);
        pstmt.setDate(6, Date.valueOf(LocalDate.now()));
        pstmt.setString(7, user.getRole().name());
        pstmt.executeUpdate();

        ResultSet rs = pstmt.getGeneratedKeys();
        if (rs.next()) {
            user.setId(rs.getInt(1));
        }
        rs.close();
        pstmt.close();
    }

    @Override
    public void update(User user) throws SQLException {
        String query = "UPDATE user SET nom = ?, prenom = ?, email = ?, password = ?, date_naissance = ?, role = ? WHERE id = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, user.getNom());
        pstmt.setString(2, user.getPrenom());
        pstmt.setString(3, user.getEmail());
        pstmt.setString(4, user.getPassword());
        pstmt.setDate(5, user.getDateNaissance() != null ? Date.valueOf(user.getDateNaissance()) : null);
        pstmt.setString(6, user.getRole().name());
        pstmt.setInt(7, user.getId());
        pstmt.executeUpdate();
        pstmt.close();
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM user WHERE id = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, id);
        pstmt.executeUpdate();
        pstmt.close();
    }

    @Override
    public User getById(int id) throws SQLException {
        String query = "SELECT * FROM user WHERE id = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, id);
        ResultSet rs = pstmt.executeQuery();

        User user = null;
        if (rs.next()) {
            user = mapResultSetToUser(rs);
        }
        rs.close();
        pstmt.close();
        return user;
    }

    @Override
    public List<User> getAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM user";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        while (rs.next()) {
            users.add(mapResultSetToUser(rs));
        }
        rs.close();
        stmt.close();
        return users;
    }

    public User getByEmail(String email) throws SQLException {
        String query = "SELECT * FROM user WHERE email = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, email);
        ResultSet rs = pstmt.executeQuery();

        User user = null;
        if (rs.next()) {
            user = mapResultSetToUser(rs);
        }
        rs.close();
        pstmt.close();
        return user;
    }

    public List<User> getByRole(Role role) throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM user WHERE role = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, role.name());
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            users.add(mapResultSetToUser(rs));
        }
        rs.close();
        pstmt.close();
        return users;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        Date dateNaissance = rs.getDate("date_naissance");
        if (dateNaissance != null) {
            user.setDateNaissance(dateNaissance.toLocalDate());
        }
        Date dateInscription = rs.getDate("date_inscription");
        if (dateInscription != null) {
            user.setDateInscription(dateInscription.toLocalDate());
        }
        user.setRole(Role.valueOf(rs.getString("role")));
        return user;
    }
}
