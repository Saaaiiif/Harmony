package com.example.harmony.user.interfaces;

import com.example.harmony.user.models.Role;

import java.util.List;

public interface services<T> {
    void add(T t);
    List<T> getAll();
    T getOneById(int id);
    void deleteById(int id);
    void updateById(int id, String nom, String prenom, String email, String password, String dateNaissance, String dateInscription, Role role);

}
