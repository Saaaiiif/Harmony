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

    @Override
    public void ajouter(Aliment a) {
        String qry = "INSERT INTO `aliment` (`nom_aliment`, `calories_pour_100g`) VALUES (?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, a.getNom_aliment());
            pstm.setInt(2, a.getCalories_pour_100g());
            pstm.executeUpdate();
            System.out.println("✅ Aliment ajouté !");
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }

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

    @Override
    public void supprimer(Aliment a) {
        supprimerParId(a.getId_aliment());
    }

    // Ajout de cette méthode pour le Workshop Test
    public void supprimerParId(int id) {
        String qry = "DELETE FROM `aliment` WHERE `id_aliment` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        } catch (SQLException e) { System.err.println(e.getMessage()); }
    }
}