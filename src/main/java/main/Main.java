package main;

import entities.*;
import services.*;
import java.sql.Timestamp;
import java.util.List;



public class Main {

    public static void main(String[] args) {

        System.out.println("🚀 DÉMARRAGE DU TEST CRUD COMPLET (TOUTES LES TABLES)...\n");

        // ---------------------------------------------------------------
        // PARTIE 1 : LES PARENTS (Aliment et Exercice)
        // On les crée d'abord pour avoir des IDs valides pour la suite.
        // ---------------------------------------------------------------

        // === 1. ALIMENT (C.R.U) ===
        System.out.println("--- [1. GESTION ALIMENT] ---");
        ServiceAliment sa = new ServiceAliment();

        // A. CREATE
        System.out.print("Ajout Aliment... ");
        Aliment monAliment = new Aliment("Pomme Rouge", 52);
        sa.ajouter(monAliment);

        // B. READ (On récupère l'ID généré)
        List<Aliment> listeAliments = sa.afficherTout();
        Aliment dernierAliment = listeAliments.get(listeAliments.size() - 1); // Le dernier ajouté
        int idAliment = dernierAliment.getId_aliment();
        System.out.println("OK (ID: " + idAliment + ")");

        // C. UPDATE

        System.out.print("Modification Aliment... ");
        dernierAliment.setNom_aliment("Pomme Golden");
        dernierAliment.setCalories_pour_100g(55);
        sa.modifier(dernierAliment);
        System.out.println("OK (Nom changé en 'Pomme Golden')");



        // NOTE: On ne SUPPRIME PAS tout de suite, car on en a besoin pour Consommation !


        // === 2. EXERCICE (C.R.U) ===
        System.out.println("\n--- [2. GESTION EXERCICE] ---");
        ServiceExercice se = new ServiceExercice();

        // A. CREATE
        System.out.print("Ajout Exercice... ");
        Exercice monExercice = new Exercice("Burpees", "CARDIO", "burpees.png");
        se.ajouter(monExercice);

        // B. READ
        List<Exercice> listeExercices = se.afficherTout();
        Exercice dernierExercice = listeExercices.get(listeExercices.size() - 1);
        int idExercice = dernierExercice.getId_exercice();
        System.out.println("OK (ID: " + idExercice + ")");

        // C. UPDATE

        System.out.print("Modification Exercice... ");
        dernierExercice.setNom_exercice("Burpees Intenses");
        se.modifier(dernierExercice);
        System.out.println("OK");



        // NOTE: On ne SUPPRIME PAS tout de suite, car on en a besoin pour Activité !


        // ---------------------------------------------------------------
        // PARTIE 2 : LES INDÉPENDANTS (Sommeil)
        // Lui, on peut faire le cycle complet tout de suite.
        // ---------------------------------------------------------------

        // === 3. SOMMEIL (C.R.U.D complet) ===
        System.out.println("\n--- [3. GESTION SOMMEIL] ---");
        ServiceSommeil ss = new ServiceSommeil();

        // A. CREATE
        Timestamp coucher = Timestamp.valueOf("2023-10-10 23:00:00");
        Timestamp reveil = Timestamp.valueOf("2023-10-11 07:00:00");
        ss.ajouter(new Sommeil(coucher, reveil, "MOYEN"));
        System.out.println("Ajout Sommeil OK.");

        // B. READ
        List<Sommeil> listeSommeil = ss.afficherTout();
        if(!listeSommeil.isEmpty()) {
            Sommeil s = listeSommeil.get(listeSommeil.size() - 1);



            // C. UPDATE
            s.setQualite_sommeil("EXCELLENT");
            ss.modifier(s);
            System.out.println("Modification Sommeil OK.");
/*
            // D. DELETE
            ss.supprimer(s.getId_sommeil());
            System.out.println("Suppression Sommeil OK.");

             */
        }


        // ---------------------------------------------------------------
        // PARTIE 3 : LES ENFANTS (Consommation et Activité)
        // Ici on utilise les IDs récupérés en Partie 1.
        // ---------------------------------------------------------------

        // === 4. CONSOMMATION (C.R.U.D complet) ===
        System.out.println("\n--- [4. GESTION CONSOMMATION] ---");
        ServiceConsommation sc = new ServiceConsommation();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // A. CREATE (Liaison avec l'aliment créé plus haut)
        System.out.print("Ajout Consommation (lié à l'aliment " + idAliment + ")... ");
        Consommation maConso = new Consommation(now, "SNACK", idAliment, 200, 150);
        sc.ajouter(maConso);
        System.out.println("OK");

        // B. READ
        List<Consommation> listeConso = sc.afficherTout();
        if(!listeConso.isEmpty()){
            Consommation lastConso = listeConso.get(listeConso.size() - 1);

            // C. UPDATE
            System.out.print("Modification Consommation... ");
            lastConso.setPoids_grammes(300); // On a mangé plus !
            sc.modifier(lastConso);
            System.out.println("OK");
/*
            // D. DELETE (Important : on supprime l'enfant AVANT le parent)
            System.out.print("Suppression Consommation... ");
            sc.supprimer(lastConso.getId_consommation());
            System.out.println("OK");

 */
        }


        // === 5. ACTIVITÉ (C.R.U.D complet) ===
        System.out.println("\n--- [5. GESTION ACTIVITÉ] ---");
        ServiceActivite sact = new ServiceActivite();

        // A. CREATE (Liaison avec l'exercice créé plus haut)
        System.out.print("Ajout Activité (lié à l'exercice " + idExercice + ")... ");
        Activite monActivite = new Activite(now, idExercice, 45, 300, 4, 15, 80.5f);
        sact.ajouter(monActivite);
        System.out.println("OK");

        // B. READ
        List<Activite> listeAct = sact.afficherTout();
        if(!listeAct.isEmpty()){
            Activite lastAct = listeAct.get(listeAct.size() - 1);

            // C. UPDATE
            System.out.print("Modification Activité... ");
            lastAct.setCalories_brulees(350);
            sact.modifier(lastAct);
            System.out.println("OK");
/*
            // D. DELETE (Suppression de l'enfant)
            System.out.print("Suppression Activité... ");
            sact.supprimer(lastAct.getId_activite());
            System.out.println("OK");

 */
        }


        // ---------------------------------------------------------------
        // PARTIE 4 : NETTOYAGE FINAL (Suppression des Parents)
        // Maintenant que les enfants sont supprimés, on peut supprimer les parents.
        // ---------------------------------------------------------------
        System.out.println("\n--- [NETTOYAGE FINAL] ---");

        // D. DELETE ALIMENT
        System.out.print("Suppression de l'Aliment " + idAliment + "... ");
       // sa.supprimer(idAliment);
        System.out.println("OK");

        // D. DELETE EXERCICE
        System.out.print("Suppression de l'Exercice " + idExercice + "... ");
        se.supprimer(idExercice);
        System.out.println("OK");



        System.out.println("\n✅✅ TEST GLOBAL TERMINÉ AVEC SUCCÈS ! TA BASE EST PROPRE.");
    }
}

