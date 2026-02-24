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
        String qry = "INSERT INTO `exercice` (`nom_exercice`, `type_exercice`, `video_exercice`) VALUES (?, ?, ?)";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, e.getNom_exercice());
            pstm.setString(2, e.getType_exercice());
            pstm.setString(3, e.getVideo_exercice());
            pstm.executeUpdate();
            System.out.println("✅ Exercice ajouté !");
        } catch (SQLException ex) { System.err.println("❌ Erreur ajout : " + ex.getMessage()); }
    }

    public List<Exercice> afficherTout() {
        List<Exercice> liste = new ArrayList<>();
        String qry = "SELECT * FROM `exercice`";
        try (Statement stm = cnx.createStatement(); ResultSet rs = stm.executeQuery(qry)) {
            while (rs.next()) {
                liste.add(new Exercice(rs.getInt("id_exercice"), rs.getString("nom_exercice"), rs.getString("type_exercice"), rs.getString("video_exercice")));
            }
        } catch (SQLException ex) { System.err.println("❌ Erreur affichage : " + ex.getMessage()); }
        return liste;
    }

    public void modifier(Exercice e) {
        String qry = "UPDATE `exercice` SET `nom_exercice` = ?, `type_exercice` = ?, `video_exercice` = ? WHERE `id_exercice` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setString(1, e.getNom_exercice());
            pstm.setString(2, e.getType_exercice());
            pstm.setString(3, e.getVideo_exercice());
            pstm.setInt(4, e.getId_exercice());
            pstm.executeUpdate();
            System.out.println("✅ Exercice mis à jour !");
        } catch (SQLException ex) { System.err.println("❌ Erreur modification : " + ex.getMessage()); }
    }

    public void supprimer(int id) {
        String qry = "DELETE FROM `exercice` WHERE `id_exercice` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(qry)) {
            pstm.setInt(1, id); pstm.executeUpdate();
            System.out.println("✅ Exercice supprimé !");
        } catch (SQLException ex) { System.err.println("❌ Erreur suppression : " + ex.getMessage()); }
    }
}