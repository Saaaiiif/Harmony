package org.example;

import models.Categorie;
import services.ServiceCategorie;
import utils.MyDataBase;

public class Main {
    public static void main(String[] args) {

        ServiceCategorie sc = new ServiceCategorie();

        //sc.add(new Categorie("java"," cours de jave " ));

        for (Categorie c : sc.getAll()) {
            System.out.println(c);
        }
        Categorie c1 = new Categorie();
        //c1.setIdCategorie(1);
        //c1.setNomCategorie("Java Avancé");
        //c1.setDescription("Cours Java approfondi");

        //sc.update(c1);

        for (Categorie c : sc.getAll()) {
            System.out.println(c);
        }
        //sc.delete(c1);

    }
}