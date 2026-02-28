package com.example.harmony.user.models;

public class user {   // je garde "user" pour l'instant pour ne pas casser tes imports

    private int user_id;
    private String user_nom, user_prenom, user_email, user_password,user_date_de_naissance,date_inscription;
    private Role type_utilisateur;

    public user() {}

    public user(String nom, String prenom, String email, String password,
                String dateNaissance, String dateInscription, Role role) {
        this.user_nom = nom;
        this.user_prenom = prenom;
        this.user_email = email;
        this.user_password = password;
        this.user_date_de_naissance = dateNaissance;
        this.date_inscription = dateInscription;
        this.type_utilisateur = role;
        this.user_id = 0;
    }

    public String getUser_nom() {
        return user_nom;
    }

    public void setUser_nom(String user_nom) {
        this.user_nom = user_nom;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getUser_prenom() {
        return user_prenom;
    }

    public void setUser_prenom(String user_prenom) {
        this.user_prenom = user_prenom;
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public String getUser_password() {
        return user_password;
    }

    public void setUser_password(String user_password) {
        this.user_password = user_password;
    }

    public String getDate_inscription() {
        return date_inscription;
    }

    public void setDate_inscription(String date_inscription) {
        this.date_inscription = date_inscription;
    }

    public String getUser_date_de_naissance() {
        return user_date_de_naissance;
    }

    public void setUser_date_de_naissance(String user_date_de_naissance) {
        this.user_date_de_naissance = user_date_de_naissance;
    }

    public Role getType_utilisateur() {
        return type_utilisateur;
    }

    public void setType_utilisateur(Role type_utilisateur) {
        this.type_utilisateur = type_utilisateur;
    }

    @Override
    public String toString() {
        return "user{" +
                "user_id=" + user_id +
                ", user_nom='" + user_nom + '\'' +
                ", user_prenom='" + user_prenom + '\'' +
                ", user_email='" + user_email + '\'' +
                ", user_password='" + user_password + '\'' +
                ", user_date_de_naissance=" + user_date_de_naissance +
                ", date_inscription=" + date_inscription +
                ", type_utilisateur=" + type_utilisateur +
                "} \n";
    }


}