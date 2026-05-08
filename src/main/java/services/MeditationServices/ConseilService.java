package services.MeditationServices;

import models.MeditationModels.Conseil;
import utils.MyDataBase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConseilService implements IService<Conseil> {

    private final Connection connection;

    public ConseilService() {
        this.connection = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(Conseil conseil) throws SQLException {
        String query = "INSERT INTO conseil (session_id, contenu) VALUES (?, ?)";
        PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        pstmt.setInt(1, conseil.getSessionId());
        pstmt.setString(2, conseil.getContenu());
        pstmt.executeUpdate();
        ResultSet rs = pstmt.getGeneratedKeys();
        if (rs.next()) conseil.setId(rs.getInt(1));
        rs.close();
        pstmt.close();
    }

    @Override
    public void update(Conseil conseil) throws SQLException {
        String query = "UPDATE conseil SET session_id=?, contenu=? WHERE id=?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, conseil.getSessionId());
        pstmt.setString(2, conseil.getContenu());
        pstmt.setInt(3, conseil.getId());
        pstmt.executeUpdate();
        pstmt.close();
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM conseil WHERE id=?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, id);
        pstmt.executeUpdate();
        pstmt.close();
    }

    @Override
    public Conseil getById(int id) throws SQLException {
        String query = "SELECT * FROM conseil WHERE id=?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, id);
        ResultSet rs = pstmt.executeQuery();
        Conseil conseil = null;
        if (rs.next()) conseil = mapResultSet(rs);
        rs.close();
        pstmt.close();
        return conseil;
    }

    @Override
    public List<Conseil> getAll() throws SQLException {
        List<Conseil> conseils = new ArrayList<>();
        String query = "SELECT * FROM conseil ORDER BY id DESC";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()) conseils.add(mapResultSet(rs));
        rs.close();
        stmt.close();
        return conseils;
    }

    public List<Conseil> getBySessionId(int sessionId) throws SQLException {
        List<Conseil> conseils = new ArrayList<>();
        String query = "SELECT * FROM conseil WHERE session_id=? ORDER BY id";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, sessionId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) conseils.add(mapResultSet(rs));
        rs.close();
        pstmt.close();
        return conseils;
    }

    public void deleteBySessionId(int sessionId) throws SQLException {
        String query = "DELETE FROM conseil WHERE session_id=?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, sessionId);
        pstmt.executeUpdate();
        pstmt.close();
    }

    private Conseil mapResultSet(ResultSet rs) throws SQLException {
        Conseil c = new Conseil();
        c.setId(rs.getInt("id"));
        c.setSessionId(rs.getInt("session_id"));
        c.setContenu(rs.getString("contenu"));
        return c;
    }
}
