package services;

import entities.Aliment;
import interfaces.services;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceAliment implements services<Aliment> {
    private Connection cnx;

    public ServiceAliment() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    // CREATE
    public void ajouter(Aliment a) {
        String qry = "INSERT INTO `aliment` (`nom_aliment`, `calories_pour_100g`) VALUES (?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, a.getNom_aliment());
            pstm.setInt(2, a.getCalories_pour_100g());
            pstm.executeUpdate();
            System.out.println("✅ Aliment ajouté !");
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    // READ
    @Override
    public List<Aliment> afficherTout() {
        List<Aliment> liste = new ArrayList<>();
        String qry = "SELECT * FROM `aliment`";
        try (Statement stm = cnx.createStatement(); ResultSet rs = stm.executeQuery(qry)) {
            while (rs.next()) {
                liste.add(new Aliment(rs.getInt("id_aliment"), rs.getString("nom_aliment"), rs.getInt("calories_pour_100g")));
            }
        } catch (SQLException e) { System.err.println(e.getMessage()); }
        return liste;
    }

    // UPDATE
    @Override
    public void modifier(Aliment a) {
        String qry = "UPDATE `aliment` SET `nom_aliment` = ?, `calories_pour_100g` = ? WHERE `id_aliment` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, a.getNom_aliment());
            pstm.setInt(2, a.getCalories_pour_100g());
            pstm.setInt(3, a.getId_aliment());
            pstm.executeUpdate();
            System.out.println("✅ Aliment mis à jour !");
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

    // DELETE
    @Override
    public void supprimer(Aliment a) {
        String qry = "DELETE FROM `aliment` WHERE `id_aliment` = ?";
        /*
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(, a);
            pstm.executeUpdate();
            System.out.println("✅ Aliment supprimé !");
        } catch (SQLException e) { System.err.println(e.getMessage()); }

         */
    }
}