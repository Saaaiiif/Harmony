package services.ForumServices;

import interfaces.ForumServices;
import models.ForumModels.Categorie;
import utils.MyDataBase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
public class ServiceCategorie implements ForumServices<Categorie> {

    private Connection cnx ;

    public ServiceCategorie(){
        this.cnx  = MyDataBase.getInstance().getCnx();
    }


    @Override
    public void add(Categorie categorie) {
        String req = "INSERT INTO categorie(nom_categorie, description, date_creation) VALUES (?, ?, ?)";
        //String req = "INSERT INTO `categorie`(`nom_categorie`, `description`, `date_creation`) VALUES ("+categorie.getNomCategorie()+" , "+categorie.getDescription()+" , "+categorie.getDateCreation()+" .";);
            Connection cnx = MyDataBase.getInstance().getCnx();

        try {

            //PreparedStatement ps = cnx.prepareStatement(req);
            PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);

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





            System.out.println("Categorie ajoutée avec succès");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    @Override
    public void update(Categorie categorie) {

        String req = "UPDATE categorie SET nom_categorie = ?, description = ? WHERE id_categorie = ?";

        Connection cnx = MyDataBase.getInstance().getCnx();

        try {

            PreparedStatement ps = cnx.prepareStatement(req);

            ps.setString(1, categorie.getNomCategorie());
            ps.setString(2, categorie.getDescription());
            ps.setInt(3, categorie.getIdCategorie());

            int rows = ps.executeUpdate();   // ✔ une seule exécution

            if(rows > 0){
                System.out.println("Categorie modifiée avec succès");
            } else {
                System.out.println("Aucune catégorie modifiée !");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }



    @Override
    public void delete(Categorie categorie) {

        String req = "DELETE FROM categorie WHERE id_categorie = ?";

        Connection cnx = MyDataBase.getInstance().getCnx();

        try {

            PreparedStatement ps = cnx.prepareStatement(req);

            ps.setInt(1, categorie.getIdCategorie());

            ps.executeUpdate();

            System.out.println("Categorie supprimée avec succès");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    public void deleteAll() throws SQLException {
        String sql = "DELETE FROM categorie  ";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.executeUpdate();
        }
    }


    @Override
    public List<Categorie> getAll() {

        List<Categorie> categories = new ArrayList<>();
        String req = "SELECT * FROM categorie";
        Connection cnx = MyDataBase.getInstance().getCnx();
        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            while (rs.next()){
                Categorie c = new Categorie();

                c.setIdCategorie(rs.getInt("id_categorie"));
                c.setNomCategorie(rs.getString("nom_categorie"));
                c.setDescription(rs.getString("description"));

                // récupération date
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
