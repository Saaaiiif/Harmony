package interfaces;

import models.*;

import java.util.List;

public interface services<T> {
    void add(T t);
    List<T> getAll();
    T getOneById(int id);
    void deleteById(int id);
    
    // Signature modifiée pour inclure les nouveaux champs
    void updateById(int id, 
                    String nom, 
                    String prenom, 
                    String email, 
                    String password, 
                    String dateNaissance, 
                    String dateInscription, 
                    Role role,
                    Sexe sexe,
                    Double poids,
                    Integer taille,
                    NiveauActivitePhysique niveauActivite,
                    NiveauScolaire niveauScolaire,
                    String etablissement);
}
