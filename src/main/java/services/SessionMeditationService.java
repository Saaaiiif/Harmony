package services;

import models.SessionMeditation;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SessionMeditationService implements IService<SessionMeditation> {

    private final Connection connection;

    public SessionMeditationService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void add(SessionMeditation session) throws SQLException {
        String query = "INSERT INTO session_meditation (user_id, auteur, duree, theme, audio_url) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        pstmt.setInt(1, session.getUserId());
        pstmt.setString(2, session.getAuteur());
        pstmt.setInt(3, session.getDuree());
        pstmt.setString(4, session.getTheme());
        pstmt.setString(5, session.getAudioUrl());
        pstmt.executeUpdate();

        ResultSet rs = pstmt.getGeneratedKeys();
        if (rs.next()) {
            session.setId(rs.getInt(1));
        }
        rs.close();
        pstmt.close();
    }

    @Override
    public void update(SessionMeditation session) throws SQLException {
        String query = "UPDATE session_meditation SET user_id = ?, auteur = ?, duree = ?, theme = ?, audio_url = ? WHERE id = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, session.getUserId());
        pstmt.setString(2, session.getAuteur());
        pstmt.setInt(3, session.getDuree());
        pstmt.setString(4, session.getTheme());
        pstmt.setString(5, session.getAudioUrl());
        pstmt.setInt(6, session.getId());
        pstmt.executeUpdate();
        pstmt.close();
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM session_meditation WHERE id = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, id);
        pstmt.executeUpdate();
        pstmt.close();
    }

    @Override
    public SessionMeditation getById(int id) throws SQLException {
        String query = "SELECT * FROM session_meditation WHERE id = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, id);
        ResultSet rs = pstmt.executeQuery();

        SessionMeditation session = null;
        if (rs.next()) {
            session = mapResultSetToSession(rs);
        }
        rs.close();
        pstmt.close();
        return session;
    }

    @Override
    public List<SessionMeditation> getAll() throws SQLException {
        List<SessionMeditation> sessions = new ArrayList<>();
        String query = "SELECT * FROM session_meditation ORDER BY id DESC";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        while (rs.next()) {
            sessions.add(mapResultSetToSession(rs));
        }
        rs.close();
        stmt.close();
        return sessions;
    }

    public List<SessionMeditation> getByUserId(int userId) throws SQLException {
        List<SessionMeditation> sessions = new ArrayList<>();
        String query = "SELECT * FROM session_meditation WHERE user_id = ? ORDER BY id DESC";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, userId);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            sessions.add(mapResultSetToSession(rs));
        }
        rs.close();
        pstmt.close();
        return sessions;
    }

    public List<SessionMeditation> getByTheme(String theme) throws SQLException {
        List<SessionMeditation> sessions = new ArrayList<>();
        String query = "SELECT * FROM session_meditation WHERE theme LIKE ? ORDER BY id DESC";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setString(1, "%" + theme + "%");
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            sessions.add(mapResultSetToSession(rs));
        }
        rs.close();
        pstmt.close();
        return sessions;
    }

    public List<String> getAllThemes() throws SQLException {
        List<String> themes = new ArrayList<>();
        String query = "SELECT DISTINCT theme FROM session_meditation ORDER BY theme";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);

        while (rs.next()) {
            themes.add(rs.getString("theme"));
        }
        rs.close();
        stmt.close();
        return themes;
    }

    private SessionMeditation mapResultSetToSession(ResultSet rs) throws SQLException {
        SessionMeditation session = new SessionMeditation();
        session.setId(rs.getInt("id"));
        session.setUserId(rs.getInt("user_id"));
        session.setAuteur(rs.getString("auteur"));
        session.setDuree(rs.getInt("duree"));
        session.setTheme(rs.getString("theme"));
        session.setAudioUrl(rs.getString("audio_url"));
        return session;
    }
}
