package services;

import models.Humeur;
import models.JournalHumeur;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JournalHumeurService implements IService<JournalHumeur> {

    private final Connection connection;

    public JournalHumeurService() {
        this.connection = MyDatabase.getInstance().getConnection();
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
        if (rs.next()) {
            journal.setId(rs.getInt(1));
        }
        rs.close();
        pstmt.close();
    }

    @Override
    public void update(JournalHumeur journal) throws SQLException {
        String query = "UPDATE journal_humeur SET user_id = ?, date = ?, humeur = ?, score = ?, contenu = ? WHERE id = ?";
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
        String query = "DELETE FROM journal_humeur WHERE id = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, id);
        pstmt.executeUpdate();
        pstmt.close();
    }

    @Override
    public JournalHumeur getById(int id) throws SQLException {
        String query = "SELECT * FROM journal_humeur WHERE id = ?";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, id);
        ResultSet rs = pstmt.executeQuery();

        JournalHumeur journal = null;
        if (rs.next()) {
            journal = mapResultSetToJournal(rs);
        }
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

        while (rs.next()) {
            journals.add(mapResultSetToJournal(rs));
        }
        rs.close();
        stmt.close();
        return journals;
    }

    public List<JournalHumeur> getByUserId(int userId) throws SQLException {
        List<JournalHumeur> journals = new ArrayList<>();
        String query = "SELECT * FROM journal_humeur WHERE user_id = ? ORDER BY date DESC, id DESC";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, userId);
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            journals.add(mapResultSetToJournal(rs));
        }
        rs.close();
        pstmt.close();
        return journals;
    }

    public List<JournalHumeur> getByUserIdAndDateRange(int userId, LocalDate startDate, LocalDate endDate) throws SQLException {
        List<JournalHumeur> journals = new ArrayList<>();
        String query = "SELECT * FROM journal_humeur WHERE user_id = ? AND date BETWEEN ? AND ? ORDER BY date DESC";
        PreparedStatement pstmt = connection.prepareStatement(query);
        pstmt.setInt(1, userId);
        pstmt.setDate(2, Date.valueOf(startDate));
        pstmt.setDate(3, Date.valueOf(endDate));
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            journals.add(mapResultSetToJournal(rs));
        }
        rs.close();
        pstmt.close();
        return journals;
    }

    private JournalHumeur mapResultSetToJournal(ResultSet rs) throws SQLException {
        JournalHumeur journal = new JournalHumeur();
        journal.setId(rs.getInt("id"));
        journal.setUserId(rs.getInt("user_id"));
        Date date = rs.getDate("date");
        if (date != null) {
            journal.setDate(date.toLocalDate());
        }
        journal.setHumeur(Humeur.fromString(rs.getString("humeur")));
        journal.setScore(rs.getInt("score"));
        journal.setContenu(rs.getString("contenu"));
        return journal;
    }
}
