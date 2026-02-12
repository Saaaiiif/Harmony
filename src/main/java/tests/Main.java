package tests;

import entities.*;
import services.*;
import java.sql.Timestamp;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        System.out.println("🚀 DÉMARRAGE DES TESTS DU SYSTÈME PI_CRUD...\n");

        // ==========================================
        // 1. TEST CRUD - ALIMENT
        // ==========================================
        System.out.println("--- [ALIMENT] ---");
        ServiceAliment sa = new ServiceAliment();

        // Ajout
        sa.ajouter(new Aliment("Banane", 89));
        sa.ajouter(new Aliment("mimo", 89));

        // Affichage & Modification
        List<Aliment> aliments = sa.afficherTout();
        if (!aliments.isEmpty()) {
            Aliment a = aliments.get(aliments.size() - 1); // On prend le dernier ajouté
            System.out.println("Dernier aliment trouvé : " + a);
           // a.setNom_aliment(a.getNom_aliment() + " Bio");
           // sa.modifier(a);
        }


        //supprimer un aliment id = 3
        // ==========================================
        // 2. TEST CRUD - EXERCICE
        // ==========================================
        System.out.println("\n--- [EXERCICE] ---");
        ServiceExercice se = new ServiceExercice();
        se.ajouter(new Exercice("Pompes", "MUSCULATION", "pompes.png"));

        List<Exercice> exercices = se.afficherTout();
        if(!exercices.isEmpty()){
            System.out.println("Exercice enregistré : " + exercices.get(0).getNom_exercice());
        }

        // ==========================================
        // 3. TEST CRUD - SOMMEIL
        // ==========================================
        System.out.println("\n--- [SOMMEIL] ---");
        ServiceSommeil ss = new ServiceSommeil();
        Timestamp coucher = Timestamp.valueOf("2024-05-20 22:30:00");
        Timestamp reveil = Timestamp.valueOf("2024-05-21 07:00:00");
        ss.ajouter(new Sommeil(coucher, reveil, "BON"));

        System.out.println("Nuits enregistrées : " + ss.afficherTout().size());

        // ==========================================
        // 4. TEST CRUD - CONSOMMATION (Liaison avec Aliment)
        // ==========================================
        System.out.println("\n--- [CONSOMMATION] ---");
        ServiceConsommation sc = new ServiceConsommation();
        if (!aliments.isEmpty()) {
            // On lie la consommation à l'ID du premier aliment trouvé
            int idAliment = aliments.get(0).getId_aliment();
            sc.ajouter(new Consommation(new Timestamp(System.currentTimeMillis()), "DEJEUNER", idAliment, 250, 150));
            System.out.println("✅ Consommation liée à l'aliment ID " + idAliment + " ajoutée.");
        }

        // ==========================================
        // 5. TEST CRUD - ACTIVITÉ (Liaison avec Exercice)
        // ==========================================
        System.out.println("\n--- [ACTIVITÉ] ---");
        ServiceActivite sact = new ServiceActivite();
        if (!exercices.isEmpty()) {
            int idExo = exercices.get(0).getId_exercice();
            sact.ajouter(new Activite(new Timestamp(System.currentTimeMillis()), idExo, 30, 200, 3, 15, 0));
            System.out.println("✅ Activité liée à l'exercice ID " + idExo + " ajoutée.");
        }

        System.out.println("\n🎯 TOUS LES TESTS SONT TERMINÉS AVEC SUCCÈS !");
        System.out.println("Vérifiez maintenant votre base de données dans PHPMyAdmin.");
    }
}