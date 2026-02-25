package services;

import models.Categorie;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServiceCategorie {

    private final Connection cnx;

    public ServiceCategorie() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    public void add(Categorie categorie) {
        String req = "INSERT INTO categorie(nom_categorie, description, date_creation) VALUES (?, ?, ?)";
        try (PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, categorie.getNomCategorie());
            ps.setString(2, categorie.getDescription());
            LocalDateTime date = categorie.getDateCreation();
            if(date == null){
                date = LocalDateTime.now();
            }
            ps.setTimestamp(3, Timestamp.valueOf(date));
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if(rs.next()){
                categorie.setIdCategorie(rs.getInt(1));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void update(Categorie categorie) {
        String req = "UPDATE categorie SET nom_categorie = ?, description = ? WHERE id_categorie = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, categorie.getNomCategorie());
            ps.setString(2, categorie.getDescription());
            ps.setInt(3, categorie.getIdCategorie());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void delete(Categorie categorie) {
        String req = "DELETE FROM categorie WHERE id_categorie = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, categorie.getIdCategorie());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void deleteAll() throws SQLException {
        String sql = "DELETE FROM categorie";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }

    public List<Categorie> getAll() {
        List<Categorie> categories = new ArrayList<>();
        String req = "SELECT * FROM categorie";
        try (Statement stm = cnx.createStatement();
             ResultSet rs = stm.executeQuery(req)) {
            while (rs.next()){
                Categorie c = new Categorie();
                c.setIdCategorie(rs.getInt("id_categorie"));
                c.setNomCategorie(rs.getString("nom_categorie"));
                c.setDescription(rs.getString("description"));
                Timestamp ts = rs.getTimestamp("date_creation");
                if (ts != null) {
                    c.setDateCreation(ts.toLocalDateTime());
                }
                categories.add(c);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return categories;
    }
}

