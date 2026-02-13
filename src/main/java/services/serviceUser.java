package services;

import models.Role;
import models.user;
import interfaces.services;
import utils.MyDataBase;
import utils.PasswordUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class serviceUser implements services<user> {
    private Connection cnx;

    public serviceUser() {
        this.cnx=MyDataBase.getInstance().getCnx();
    }


    @Override
    public void add(user user) {
        // Hachage du mot de passe AVANT insertion
        String hashedPassword = PasswordUtils.hashPassword(user.getUser_password());

        String req = "INSERT INTO `user`(`user_nom`, `user_prenom`, `user_email`, `user_password`, `user_date_de_naissance`, `date_inscription`, `type_utilisateur`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, user.getUser_nom());
            pstm.setString(2, user.getUser_prenom());
            pstm.setString(3, user.getUser_email());
            pstm.setString(4, hashedPassword);                    // ← Hashé
            pstm.setString(5, user.getUser_date_de_naissance());
            pstm.setString(6, user.getDate_inscription());
            pstm.setString(7, user.getType_utilisateur().name());

            pstm.executeUpdate();
            System.out.println("USER ADDED SUCCESSFULLY (password hashed) !!!");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<user> getAll() {
       List<user>users=new ArrayList<>();
       String req = "SELECT * FROM `user`";
       try{
           Statement stm = cnx.createStatement();
           ResultSet rs = stm.executeQuery(req);
           while(rs.next()){
               user user=new user();
               user.setUser_id(rs.getInt("user_id"));
               user.setUser_nom(rs.getString("user_nom"));
               user.setUser_prenom(rs.getString("user_prenom"));
               user.setUser_email(rs.getString("user_email"));
               user.setUser_password(rs.getString("user_password"));
               user.setDate_inscription(rs.getString("date_inscription"));
               user.setUser_date_de_naissance(rs.getString("user_date_de_naissance"));
               String roleStr = rs.getString("type_utilisateur");
               if (roleStr != null && !roleStr.isEmpty()) {
                   try {
                       user.setType_utilisateur(Role.valueOf(roleStr));  // Convertit "ETUDIANT" → Role.ETUDIANT
                   } catch (IllegalArgumentException e) {
                       System.err.println("Valeur ENUM invalide en BD: " + roleStr + ". Assigné ADMIN par défaut.");
                       user.setType_utilisateur(Role.ETUDIANT);  // Fallback pour éviter crash
                   }
               } else {
                   user.setType_utilisateur(Role.ETUDIANT);  // Défaut si null
               }

                users.add(user);
           }
       }catch (SQLException e){
           System.out.println(e.getMessage());
       }
       return users;
    }

    @Override
    public void deleteById(int id) {
        if(id<=0){
            System.err.println("ID invalide pour suppression !!");
        }
        String req = "DELETE FROM `user` WHERE `user_id` = ?" ;
        try(PreparedStatement pstm = cnx.prepareStatement(req)){
            pstm.setInt(1,id);
            int affectedRows = pstm.executeUpdate();
            if(affectedRows > 0){
                System.out.println("USER DELETE SUCCESSFULLY !!!");

            }else {
                System.out.println("USER NOT DELETE SUCCESSFULLY !!!");
            }


        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
        }


    }

    @Override
    public void updateById(int id, String nom, String prenom, String email, String password,
                           String dateNaissance, String dateInscription, Role role) {

        if (id <=0){
            System.err.println("ID invalide pour mise a jour  !!");
            return;
        }
        String req = "UPDATE `user` SET " +
                "`user_nom` = ?, `user_prenom` = ?, `user_email` = ?, `user_password` = ?, " +
                "`user_date_de_naissance` = ?, `date_inscription` = ?, `type_utilisateur` = ? " +
                "WHERE `user_id` = ?";
        try (PreparedStatement pstm = cnx.prepareStatement(req)){
            pstm.setString(1,nom);
            pstm.setString(2,prenom);
            pstm.setString(3, email);
            pstm.setString(4, password);
            pstm.setString(5, dateNaissance);
            pstm.setString(6, dateInscription);
            pstm.setString(7, role.name());        // → "ETUDIANT" ou "ADMIN"
            pstm.setInt(8, id);

            int affectedRows = pstm.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("USER UPDATED SUCCESSFULLY !!!");
            } else {
                System.out.println("USER NOT UPDATED (ID introuvable) !!!");
            }

        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour : " + e.getMessage());
            e.printStackTrace();
        }

        }


        @Override
    public user getOneById(int id ){
            if (id <= 0) {
                System.err.println("ID invalide pour la recherche !!");
                return null;
            }

            String req = "SELECT * FROM `user` WHERE `user_id` = ?";

            try (PreparedStatement pstm = cnx.prepareStatement(req)) {

                pstm.setInt(1, id);
                ResultSet rs = pstm.executeQuery();

                if (rs.next()) {                    // Si on trouve une ligne
                    user u = new user();

                    u.setUser_id(rs.getInt("user_id"));
                    u.setUser_nom(rs.getString("user_nom"));
                    u.setUser_prenom(rs.getString("user_prenom"));
                    u.setUser_email(rs.getString("user_email"));
                    u.setUser_password(rs.getString("user_password"));
                    u.setUser_date_de_naissance(rs.getString("user_date_de_naissance"));
                    u.setDate_inscription(rs.getString("date_inscription"));

                    // Gestion du Role (comme dans getAll)
                    String roleStr = rs.getString("type_utilisateur");
                    if (roleStr != null && !roleStr.isEmpty()) {
                        try {
                            u.setType_utilisateur(Role.valueOf(roleStr));
                        } catch (IllegalArgumentException e) {
                            System.err.println("Role invalide : " + roleStr);
                            u.setType_utilisateur(Role.ETUDIANT); // fallback
                        }
                    } else {
                        u.setType_utilisateur(Role.ETUDIANT);
                    }

                    return u;
                } else {
                    System.out.println("Aucun utilisateur trouvé avec l'ID : " + id);
                    return null;
                }

            } catch (SQLException e) {
                System.err.println("Erreur lors de la récupération de l'utilisateur : " + e.getMessage());
                e.printStackTrace();
                return null;
            }

        }

    public user getByEmailAndPassword(String email, String plainPassword) {
        String req = "SELECT * FROM `user` WHERE `user_email` = ?";

        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, email);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                String storedHashedPassword = rs.getString("user_password");

                // Vérification du mot de passe avec le hash
                if (PasswordUtils.checkPassword(plainPassword, storedHashedPassword)) {
                    user u = new user();
                    u.setUser_id(rs.getInt("user_id"));
                    u.setUser_nom(rs.getString("user_nom"));
                    u.setUser_prenom(rs.getString("user_prenom"));
                    u.setUser_email(rs.getString("user_email"));
                    u.setUser_password(storedHashedPassword); // On garde le hash
                    u.setUser_date_de_naissance(rs.getString("user_date_de_naissance"));
                    u.setDate_inscription(rs.getString("date_inscription"));
                    u.setType_utilisateur(Role.valueOf(rs.getString("type_utilisateur")));
                    return u;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }







    }

