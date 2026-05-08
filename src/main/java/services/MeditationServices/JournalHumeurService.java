package services.MeditationServices;

import models.MeditationModels.Humeur;
import models.MeditationModels.JournalHumeur;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JournalHumeurService implements IService<JournalHumeur> {

    private final Connection connection;

    public JournalHumeurService() {
        this.connection = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(JournalHumeur journal) throws SQLException {
        String query = "INSERT INTO journal_humeur (user_id, date, humeur, score, contenu) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        pstmt.setInt(1, journal.getUserId());
        pstmt.setDate(2, journal.getDate() != null ? Date.valueOf(journal.getDate()) : Date.valueOf(LocalDate.now()));
        pstmt.setString(3, journal.getHumeur().name());
        pstmt.setInt(4, journal.getScore());
        pstmt.setString(5, journal.getContenu());
        pstmt.executeUpdate();
        ResultSet rs = pstmt.getGeneratedKeys();
        if (rs.next()) journal.setId(rs.getInt(1));
        rs.close();
        pstmt.close();
    }

    @Override
    public void update(JournalHumeur journal) throws SQLException {
        String query = "UPDATE journal_humeur SET user_id=?, date=?, humeur=?, score=?, contenu=? WHERE id=?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, journal.getUserId());
        pstmt.setDate(2, journal.getDate() != null ? Date.valueOf(journal.getDate()) : Date.valueOf(LocalDate.now()));
        pstmt.setString(3, journal.getHumeur().name());
        pstmt.setInt(4, journal.getScore());
        pstmt.setString(5, journal.getContenu());
        pstmt.setInt(6, journal.getId());
        pstmt.executeUpdate();
        pstmt.close();
    }

    @Override
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM journal_humeur WHERE id=?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, id);
        pstmt.executeUpdate();
        pstmt.close();
    }

    @Override
    public JournalHumeur getById(int id) throws SQLException {
        String query = "SELECT * FROM journal_humeur WHERE id=?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, id);
        ResultSet rs = pstmt.executeQuery();
        JournalHumeur journal = null;
        if (rs.next()) journal = mapResultSet(rs);
        rs.close();
        pstmt.close();
        return journal;
    }

    @Override
    public List<JournalHumeur> getAll() throws SQLException {
        List<JournalHumeur> journals = new ArrayList<>();
        String query = "SELECT * FROM journal_humeur ORDER BY date DESC, id DESC";
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()) journals.add(mapResultSet(rs));
        rs.close();
        stmt.close();
        return journals;
    }

    public List<JournalHumeur> getByUserId(int userId) throws SQLException {
        List<JournalHumeur> journals = new ArrayList<>();
        String query = "SELECT * FROM journal_humeur WHERE user_id=? ORDER BY date DESC, id DESC";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, userId);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) journals.add(mapResultSet(rs));
        rs.close();
        pstmt.close();
        return journals;
    }

    public List<JournalHumeur> getByUserIdAndDateRange(int userId, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<JournalHumeur> journals = new ArrayList<>();
        String query = "SELECT * FROM journal_humeur WHERE user_id=? AND date BETWEEN ? AND ? ORDER BY date DESC";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, userId);
        pstmt.setDate(2, Date.valueOf(startDate));
        pstmt.setDate(3, Date.valueOf(endDate));
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()) journals.add(mapResultSet(rs));
        rs.close();
        pstmt.close();
        return journals;
    }

    private JournalHumeur mapResultSet(ResultSet rs) throws SQLException {
        JournalHumeur j = new JournalHumeur();
        j.setId(rs.getInt("id"));
        j.setUserId(rs.getInt("user_id"));
        Date d = rs.getDate("date");
        if (d != null) j.setDate(d.toLocalDate());
        j.setHumeur(Humeur.fromString(rs.getString("humeur")));
        j.setScore(rs.getInt("score"));
        j.setContenu(rs.getString("contenu"));
        return j;
    }
}
