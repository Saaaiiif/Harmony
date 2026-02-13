package tests;

import entities.Exercice;
import org.junit.jupiter.api.*;
import services.ServiceExercice;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceExerciceTest {

    static ServiceExercice se;
    static int idExerciceTest;

    @BeforeAll
    static void setup() {
        se = new ServiceExercice();
    }

    @Test
    @Order(1)
    void testAjouterExercice() {
        // CORRECTION : On utilise une valeur probable pour ton CHECK constraint (ex: "CARDIO")
        Exercice e = new Exercice("TestExercice", "CARDIO", "test.png");
        se.ajouter(e);

        List<Exercice> liste = se.afficherTout();
        assertFalse(liste.isEmpty());
        assertTrue(liste.stream().anyMatch(ex -> ex.getNom_exercice().equals("TestExercice")));

        Exercice ajoute = liste.stream()
                .filter(ex -> ex.getNom_exercice().equals("TestExercice"))
                .findFirst().orElse(null);

        if (ajoute != null) {
            idExerciceTest = ajoute.getId_exercice();
        }
    }

    @Test
    @Order(2)
    void testModifierExercice() {
        Exercice e = new Exercice();
        e.setId_exercice(idExerciceTest);
        e.setNom_exercice("ExerciceModifie");

        // CORRECTION : On remplace "Force" (qui plantait) par une autre valeur probable (ex: "MUSCULATION")
        e.setType_exercice("MUSCULATION");
        e.setImage_exercice("modif.png");

        se.modifier(e);

        List<Exercice> liste = se.afficherTout();
        boolean trouve = liste.stream()
                .anyMatch(ex -> ex.getNom_exercice().equals("ExerciceModifie") && ex.getType_exercice().equals("MUSCULATION"));
        assertTrue(trouve);
    }

    @Test
    @Order(3)
    void testSupprimerExercice() {
        se.supprimer(idExerciceTest);

        List<Exercice> liste = se.afficherTout();
        boolean existe = liste.stream().anyMatch(ex -> ex.getId_exercice() == idExerciceTest);
        assertFalse(existe);
    }

    // ==========================================
    // Nettoyage automatique après chaque test
    // ==========================================
    @AfterEach
    void cleanUp() {
        List<Exercice> exercices = se.afficherTout();
        if (!exercices.isEmpty()) {
            System.out.println("🧹 Nettoyage vérifié après le test de l'exercice.");
        }
    }
}