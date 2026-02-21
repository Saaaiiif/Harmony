package services;

import models.Exercice;
import utils.MyDataBase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceExercice {
    private Connection cnx;

    public ServiceExercice() { this.cnx = MyDataBase.getInstance().getCnx(); }

    public void ajouter(Exercice e) {
        String qry = "INSERT INTO `exercice` (`nom_exercice`, `type_exercice`, `image_exercice`) VALUES (?, ?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, e.getNom_exercice());
            pstm.setString(2, e.getType_exercice());
            pstm.setString(3, e.getImage_exercice());
            pstm.executeUpdate();
        } catch (SQLException ex) { System.err.println(ex.getMessage()); }
    }

    public List<Exercice> afficherTout() {
        List<Exercice> liste = new ArrayList<>();
        try (Statement stm = cnx.createStatement(); ResultSet rs = stm.executeQuery("SELECT * FROM `exercice`")) {
            while (rs.next()) {
                liste.add(new Exercice(rs.getInt("id_exercice"), rs.getString("nom_exercice"), rs.getString("type_exercice"), rs.getString("image_exercice")));
            }
        } catch (SQLException ex) { System.err.println(ex.getMessage()); }
        return liste;
    }

    public void modifier(Exercice e) {
        String qry = "UPDATE `exercice` SET `nom_exercice` = ?, `type_exercice` = ?, `image_exercice` = ? WHERE `id_exercice` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, e.getNom_exercice());
            pstm.setString(2, e.getType_exercice());
            pstm.setString(3, e.getImage_exercice());
            pstm.setInt(4, e.getId_exercice());
            pstm.executeUpdate();
        } catch (SQLException ex) { System.err.println(ex.getMessage()); }
    }

    public void supprimer(int id) {
        try (PreparedStatement pstm = cnx.prepareStatement("DELETE FROM `exercice` WHERE `id_exercice` = ?")) {
            pstm.setInt(1, id);
            pstm.executeUpdate();
        } catch (SQLException ex) { System.err.println(ex.getMessage()); }
    }
}