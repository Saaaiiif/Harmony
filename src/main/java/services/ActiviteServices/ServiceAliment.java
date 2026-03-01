package services.ActiviteServices;

import models.ActiviteModels.Aliment;
import interfaces.ActiviteServices;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceAliment implements ActiviteServices<Aliment> {
    private Connection cnx;

    public ServiceAliment() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void ajouter(Aliment a) {
        String qry = "INSERT INTO `aliment` (`nom_aliment`, `calories_pour_100g`, `proteines`, `glucides`, `lipides`) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, a.getNom_aliment());
            pstm.setInt(2, a.getCalories_pour_100g());
            pstm.setDouble(3, a.getProteines());
            pstm.setDouble(4, a.getGlucides());
            pstm.setDouble(5, a.getLipides());
            pstm.executeUpdate();
            System.out.println("✅ Aliment ajouté avec macros !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout aliment : " + e.getMessage());
        }
    }

    @Override
    public List<Aliment> afficherTout() {
        List<Aliment> liste = new ArrayList<>();
        String qry = "SELECT * FROM `aliment`";
        try (Statement stm = cnx.createStatement(); ResultSet rs = stm.executeQuery(qry)) {
            while (rs.next()) {
                liste.add(new Aliment(
                        rs.getInt("id_aliment"),
                        rs.getString("nom_aliment"),
                        rs.getInt("calories_pour_100g"),
                        rs.getDouble("proteines"),
                        rs.getDouble("glucides"),
                        rs.getDouble("lipides")
                ));
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur affichage aliments : " + e.getMessage());
        }
        return liste;
    }

    @Override
    public void modifier(Aliment a) {
        String qry = "UPDATE `aliment` SET `nom_aliment` = ?, `calories_pour_100g` = ?, `proteines` = ?, `glucides` = ?, `lipides` = ? WHERE `id_aliment` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, a.getNom_aliment());
            pstm.setInt(2, a.getCalories_pour_100g());
            pstm.setDouble(3, a.getProteines());
            pstm.setDouble(4, a.getGlucides());
            pstm.setDouble(5, a.getLipides());
            pstm.setInt(6, a.getId_aliment());
            pstm.executeUpdate();
            System.out.println("✅ Aliment mis à jour avec ses macros !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur modification aliment : " + e.getMessage());
        }
    }

    @Override
    public void supprimer(Aliment a) {
        supprimerParId(a.getId_aliment());
    }

    public void supprimerParId(int id) {
        String qry = "DELETE FROM `aliment` WHERE `id_aliment` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
            System.out.println("✅ Aliment supprimé !");
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression aliment : " + e.getMessage());
        }
    }
}