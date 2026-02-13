package org.example;

import models.Categorie;
import models.Commentaire;
import models.Post;
import services.ServiceCategorie;
import services.ServiceCommentaire;
import services.ServicePost;
import utils.MyDataBase;

import java.time.LocalDateTime;

public class Main {
    public static void main(String[] args) {


        //ServiceCategorie sc = new ServiceCategorie();

        //sc.add(new Categorie("java"," cours de jave " ));

        //for (Categorie c : sc.getAll()) {
        //    System.out.println(c);
        //}
        //Categorie c1 = new Categorie();
        //c1.setIdCategorie(1);
        //c1.setNomCategorie("Java Avancé");
        //c1.setDescription("Cours Java approfondi");

        //sc.update(c1);

        //for (Categorie c : sc.getAll()) {
         //   System.out.println(c);
       // }
        //sc.delete(c1);

        //-------------------------------------------test post----------------------------------------------------------


//        System.out.println("===== TEST SERVICE POST =====");
//
        //ServicePost servicePost = new ServicePost();

/* =======================
   TEST ADD POST
======================= */

        //Post p = new Post();
//        p.setTitre("Aide Java");
//        p.setContenu("Qui peut expliquer JDBC ?");
//        p.setDateCreation(LocalDateTime.now());
//        p.setIdEtudiant(1);
//        p.setIdCategorie(1);
//
//        servicePost.add(p);
//        System.out.println("Post ajouté");


/* =======================
   TEST GET ALL
======================= */
//
//        System.out.println("\nListe des posts :");
//        for(Post post : servicePost.getAll()){
//            System.out.println(post);
//        }
        /* =======================
   TEST UPDATE
======================= */

//        p.setTitre("Aide JDBC");
//        p.setContenu("Besoin d'aide pour PreparedStatement");
//        p.setIdPost(3); // ⚠️ Mets un ID existant
//
//        servicePost.update(p);
//        System.out.println("Post modifié");
        /* =======================
   TEST DELETE
======================= */

//        ServicePost sp = new ServicePost();
//
//        sp.deleteById(4);

        //------------------------------------------------test commentaire ---------------------------------------------------

        System.out.println("\n===== TEST SERVICE COMMENTAIRE =====");

        ServiceCommentaire serviceCommentaire = new ServiceCommentaire();

/* =======================
   TEST ADD COMMENTAIRE
======================= */

        Commentaire c = new Commentaire();
        c.setContenu("Très bon sujet !");
        c.setDateCommentaire(LocalDateTime.now());
        c.setIdPost(5);
        c.setIdEtudiant(1);

        serviceCommentaire.add(c);
        System.out.println("Commentaire ajouté");





    }
}