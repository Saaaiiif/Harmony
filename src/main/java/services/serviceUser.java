package services;

import models.*;
import interfaces.services;
import utils.MyDataBase;
import utils.PasswordUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class serviceUser implements services<user> {
    private Connection cnx;

    public serviceUser() {
        this.cnx = MyDataBase.getInstance().getCnx();
    }

    @Override
    public void add(user user) {
        // Hachage du mot de passe AVANT insertion
        String hashedPassword = PasswordUtils.hashPassword(user.getUser_password());

        String req = "INSERT INTO `user`(" +
                "`user_nom`, `user_prenom`, `user_email`, `user_password`, " +
                "`user_date_de_naissance`, `date_inscription`, `type_utilisateur`, " +
                "`user_sexe`, `user_poids`, `user_taille`, `user_niveau_activite_physique`, " +
                "`user_niveau_scolaire`, `user_etablissement_scolaire`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, user.getUser_nom());
            pstm.setString(2, user.getUser_prenom());
            pstm.setString(3, user.getUser_email());
            pstm.setString(4, hashedPassword);
            pstm.setString(5, user.getUser_date_de_naissance());
            pstm.setString(6, user.getDate_inscription());
            pstm.setString(7, user.getType_utilisateur().name());
            
            // Nouveaux champs SANTÉ
            pstm.setString(8, user.getUser_sexe() != null ? user.getUser_sexe().name() : null);
            
            if (user.getUser_poids() != null) {
                pstm.setDouble(9, user.getUser_poids());
            } else {
                pstm.setNull(9, Types.DECIMAL);
            }
            
            if (user.getUser_taille() != null) {
                pstm.setInt(10, user.getUser_taille());
            } else {
                pstm.setNull(10, Types.INTEGER);
            }
            
            pstm.setString(11, user.getUser_niveau_activite_physique() != null ? 
                                user.getUser_niveau_activite_physique().name() : null);
            
            // Nouveaux champs SCOLAIRE
            pstm.setString(12, user.getUser_niveau_scolaire() != null ? 
                                user.getUser_niveau_scolaire().name() : null);
            pstm.setString(13, user.getUser_etablissement_scolaire());

            pstm.executeUpdate();
            System.out.println("USER ADDED SUCCESSFULLY (with health & education data) !!!");
        } catch (SQLException e) {
            System.out.println("Erreur lors de l'ajout : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<user> getAll() {
        List<user> users = new ArrayList<>();
        String req = "SELECT * FROM `user`";
        
        try {
            Statement stm = cnx.createStatement();
            ResultSet rs = stm.executeQuery(req);
            
            while (rs.next()) {
                user u = new user();
                u.setUser_id(rs.getInt("user_id"));
                u.setUser_nom(rs.getString("user_nom"));
                u.setUser_prenom(rs.getString("user_prenom"));
                u.setUser_email(rs.getString("user_email"));
                u.setUser_password(rs.getString("user_password"));
                u.setDate_inscription(rs.getString("date_inscription"));
                u.setUser_date_de_naissance(rs.getString("user_date_de_naissance"));
                
                // Role
                String roleStr = rs.getString("type_utilisateur");
                if (roleStr != null && !roleStr.isEmpty()) {
                    try {
                        u.setType_utilisateur(Role.valueOf(roleStr));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Valeur ENUM Role invalide: " + roleStr);
                        u.setType_utilisateur(Role.ETUDIANT);
                    }
                } else {
                    u.setType_utilisateur(Role.ETUDIANT);
                }
                
                // NOUVEAUX CHAMPS SANTÉ
                String sexeStr = rs.getString("user_sexe");
                if (sexeStr != null && !sexeStr.isEmpty()) {
                    try {
                        u.setUser_sexe(Sexe.valueOf(sexeStr));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Valeur ENUM Sexe invalide: " + sexeStr);
                    }
                }
                
                Double poids = rs.getDouble("user_poids");
                if (!rs.wasNull()) {
                    u.setUser_poids(poids);
                }
                
                int taille = rs.getInt("user_taille");
                if (!rs.wasNull()) {
                    u.setUser_taille(taille);
                }
                
                String niveauActiviteStr = rs.getString("user_niveau_activite_physique");
                if (niveauActiviteStr != null && !niveauActiviteStr.isEmpty()) {
                    try {
                        u.setUser_niveau_activite_physique(NiveauActivitePhysique.valueOf(niveauActiviteStr));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Valeur ENUM NiveauActivite invalide: " + niveauActiviteStr);
                    }
                }
                
                // NOUVEAUX CHAMPS SCOLAIRE
                String niveauScolaireStr = rs.getString("user_niveau_scolaire");
                if (niveauScolaireStr != null && !niveauScolaireStr.isEmpty()) {
                    try {
                        u.setUser_niveau_scolaire(NiveauScolaire.valueOf(niveauScolaireStr));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Valeur ENUM NiveauScolaire invalide: " + niveauScolaireStr);
                    }
                }
                
                u.setUser_etablissement_scolaire(rs.getString("user_etablissement_scolaire"));
                
                users.add(u);
            }
        } catch (SQLException e) {
            System.out.println("Erreur lors de la récupération : " + e.getMessage());
            e.printStackTrace();
        }
        
        return users;
    }

    @Override
    public void deleteById(int id) {
        if (id <= 0) {
            System.err.println("ID invalide pour suppression !!");
            return;
        }
        
        String req = "DELETE FROM `user` WHERE `user_id` = ?";
        
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, id);
            int affectedRows = pstm.executeUpdate();
            
            if (affectedRows > 0) {
                System.out.println("USER DELETED SUCCESSFULLY !!!");
            } else {
                System.out.println("USER NOT FOUND (ID: " + id + ")");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void updateById(int id, String nom, String prenom, String email, String password,
                           String dateNaissance, String dateInscription, Role role,
                           Sexe sexe, Double poids, Integer taille, NiveauActivitePhysique niveauActivite,
                           NiveauScolaire niveauScolaire, String etablissement) {
        
        if (id <= 0) {
            System.err.println("ID invalide pour mise à jour !!");
            return;
        }
        
        String req = "UPDATE `user` SET " +
                "`user_nom` = ?, `user_prenom` = ?, `user_email` = ?, `user_password` = ?, " +
                "`user_date_de_naissance` = ?, `date_inscription` = ?, `type_utilisateur` = ?, " +
                "`user_sexe` = ?, `user_poids` = ?, `user_taille` = ?, " +
                "`user_niveau_activite_physique` = ?, `user_niveau_scolaire` = ?, " +
                "`user_etablissement_scolaire` = ? " +
                "WHERE `user_id` = ?";
        
        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setString(1, nom);
            pstm.setString(2, prenom);
            pstm.setString(3, email);
            pstm.setString(4, password);
            pstm.setString(5, dateNaissance);
            pstm.setString(6, dateInscription);
            pstm.setString(7, role.name());
            
            // Nouveaux champs
            pstm.setString(8, sexe != null ? sexe.name() : null);
            
            if (poids != null) {
                pstm.setDouble(9, poids);
            } else {
                pstm.setNull(9, Types.DECIMAL);
            }
            
            if (taille != null) {
                pstm.setInt(10, taille);
            } else {
                pstm.setNull(10, Types.INTEGER);
            }
            
            pstm.setString(11, niveauActivite != null ? niveauActivite.name() : null);
            pstm.setString(12, niveauScolaire != null ? niveauScolaire.name() : null);
            pstm.setString(13, etablissement);
            pstm.setInt(14, id);

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
    public user getOneById(int id) {
        if (id <= 0) {
            System.err.println("ID invalide pour la recherche !!");
            return null;
        }

        String req = "SELECT * FROM `user` WHERE `user_id` = ?";

        try (PreparedStatement pstm = cnx.prepareStatement(req)) {
            pstm.setInt(1, id);
            ResultSet rs = pstm.executeQuery();

            if (rs.next()) {
                user u = new user();
                u.setUser_id(rs.getInt("user_id"));
                u.setUser_nom(rs.getString("user_nom"));
                u.setUser_prenom(rs.getString("user_prenom"));
                u.setUser_email(rs.getString("user_email"));
                u.setUser_password(rs.getString("user_password"));
                u.setUser_date_de_naissance(rs.getString("user_date_de_naissance"));
                u.setDate_inscription(rs.getString("date_inscription"));

                // Role
                String roleStr = rs.getString("type_utilisateur");
                if (roleStr != null && !roleStr.isEmpty()) {
                    try {
                        u.setType_utilisateur(Role.valueOf(roleStr));
                    } catch (IllegalArgumentException e) {
                        u.setType_utilisateur(Role.ETUDIANT);
                    }
                } else {
                    u.setType_utilisateur(Role.ETUDIANT);
                }
                
                // NOUVEAUX CHAMPS SANTÉ
                String sexeStr = rs.getString("user_sexe");
                if (sexeStr != null && !sexeStr.isEmpty()) {
                    try {
                        u.setUser_sexe(Sexe.valueOf(sexeStr));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Valeur ENUM Sexe invalide: " + sexeStr);
                    }
                }
                
                Double poids = rs.getDouble("user_poids");
                if (!rs.wasNull()) {
                    u.setUser_poids(poids);
                }
                
                int taille = rs.getInt("user_taille");
                if (!rs.wasNull()) {
                    u.setUser_taille(taille);
                }
                
                String niveauActiviteStr = rs.getString("user_niveau_activite_physique");
                if (niveauActiviteStr != null && !niveauActiviteStr.isEmpty()) {
                    try {
                        u.setUser_niveau_activite_physique(NiveauActivitePhysique.valueOf(niveauActiviteStr));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Valeur ENUM NiveauActivite invalide");
                    }
                }
                
                // NOUVEAUX CHAMPS SCOLAIRE
                String niveauScolaireStr = rs.getString("user_niveau_scolaire");
                if (niveauScolaireStr != null && !niveauScolaireStr.isEmpty()) {
                    try {
                        u.setUser_niveau_scolaire(NiveauScolaire.valueOf(niveauScolaireStr));
                    } catch (IllegalArgumentException e) {
                        System.err.println("Valeur ENUM NiveauScolaire invalide");
                    }
                }
                
                u.setUser_etablissement_scolaire(rs.getString("user_etablissement_scolaire"));

                return u;
            } else {
                System.out.println("Aucun utilisateur trouvé avec l'ID : " + id);
                return null;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération : " + e.getMessage());
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

                if (PasswordUtils.checkPassword(plainPassword, storedHashedPassword)) {
                    user u = new user();
                    u.setUser_id(rs.getInt("user_id"));
                    u.setUser_nom(rs.getString("user_nom"));
                    u.setUser_prenom(rs.getString("user_prenom"));
                    u.setUser_email(rs.getString("user_email"));
                    u.setUser_password(storedHashedPassword);
                    u.setUser_date_de_naissance(rs.getString("user_date_de_naissance"));
                    u.setDate_inscription(rs.getString("date_inscription"));
                    u.setType_utilisateur(Role.valueOf(rs.getString("type_utilisateur")));
                    
                    // Récupérer aussi les nouveaux champs si nécessaire
                    String sexeStr = rs.getString("user_sexe");
                    if (sexeStr != null && !sexeStr.isEmpty()) {
                        try {
                            u.setUser_sexe(Sexe.valueOf(sexeStr));
                        } catch (IllegalArgumentException e) {
                            // Ignorer si invalide
                        }
                    }
                    
                    return u;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
