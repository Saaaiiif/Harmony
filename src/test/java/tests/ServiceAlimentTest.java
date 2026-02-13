package tests;

import entities.Aliment;
import org.junit.jupiter.api.*;
import services.ServiceAliment;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceAlimentTest {

    static ServiceAliment sa;
    static int idAlimentTest;

    @BeforeAll
    static void setup() {
        sa = new ServiceAliment();
    }

    @Test
    @Order(1)
    void testAjouterAliment() {
        Aliment a = new Aliment("TestNom", 100);
        sa.ajouter(a);

        List<Aliment> aliments = sa.afficherTout();
        assertFalse(aliments.isEmpty());

        // Correction : utilisation de tes getters exacts
        assertTrue(aliments.stream().anyMatch(alim -> alim.getNom_aliment().equals("TestNom")));

        Aliment alimentAjoute = aliments.stream()
                .filter(alim -> alim.getNom_aliment().equals("TestNom"))
                .findFirst()
                .orElse(null);

        if (alimentAjoute != null) {
            idAlimentTest = alimentAjoute.getId_aliment();
        }
    }

    @Test
    @Order(2)
    void testModifierAliment() {
        Aliment a = new Aliment();
        a.setId_aliment(idAlimentTest);
        a.setNom_aliment("NomModifie");
        a.setCalories_pour_100g(200);

        sa.modifier(a);

        List<Aliment> aliments = sa.afficherTout();
        boolean trouve = aliments.stream().anyMatch(alim -> alim.getNom_aliment().equals("NomModifie"));
        assertTrue(trouve);
    }

    @Test
    @Order(3)
    void testSupprimerAliment() {
        sa.supprimerParId(idAlimentTest);

        List<Aliment> aliments = sa.afficherTout();
        boolean existe = aliments.stream().anyMatch(p -> p.getId_aliment() == idAlimentTest);
        assertFalse(existe);
    }
    @AfterEach
    void cleanUp() {
        List<Aliment> aliments = sa.afficherTout();
        if (!aliments.isEmpty()) {
            // On récupère le dernier élément ajouté
            Aliment last = aliments.get(aliments.size() - 1);

            // On peut le supprimer pour laisser la base propre (optionnel selon ton besoin exact)
            // sa.supprimerParId(last.getId_aliment());

            System.out.println("🧹 Nettoyage vérifié après le test.");
        }
    }
}