package tests;

import entities.Aliment;
import entities.Consommation;
import org.junit.jupiter.api.*;
import services.ServiceAliment;
import services.ServiceConsommation;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceConsommationTest {

    static ServiceConsommation sc;
    static ServiceAliment sa;
    static int idConsoTest;
    static int idAlimentTemp;

    @BeforeAll
    static void setup() {
        sc = new ServiceConsommation();
        sa = new ServiceAliment();

        // 1. On crée un aliment temporaire avec EXACTEMENT les bons paramètres de ta classe Aliment
        Aliment a = new Aliment("AlimentTest", 100);
        sa.ajouter(a);

        // 2. On récupère l'ID généré automatiquement par la base de données
        List<Aliment> aliments = sa.afficherTout();
        Aliment alimentAjoute = aliments.stream()
                .filter(alim -> alim.getNom_aliment().equals("AlimentTest"))
                .findFirst().orElse(null);

        if (alimentAjoute != null) {
            idAlimentTemp = alimentAjoute.getId_aliment();
            System.out.println("✅ Aliment temporaire créé avec l'ID : " + idAlimentTemp);
        } else {
            fail("❌ Impossible de créer l'aliment temporaire pour le test !");
        }
    }

    @Test
    @Order(1)
    void testAjouterConsommation() {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // On utilise l'ID de l'aliment réel qu'on vient de créer au lieu d'inventer "1"
        Consommation c = new Consommation(now, "DEJEUNER", idAlimentTemp, 500, 200);
        sc.ajouter(c);

        List<Consommation> liste = sc.afficherTout();
        assertFalse(liste.isEmpty(), "La liste des consommations ne doit pas être vide.");
        assertTrue(liste.stream().anyMatch(conso -> conso.getType_repas().equals("DEJEUNER")));

        Consommation ajoute = liste.stream()
                .filter(conso -> conso.getType_repas().equals("DEJEUNER") && conso.getId_aliment() == idAlimentTemp)
                .findFirst().orElse(null);

        if (ajoute != null) {
            idConsoTest = ajoute.getId_consommation();
        }
    }

    @Test
    @Order(2)
    void testModifierConsommation() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Consommation c = new Consommation();
        c.setId_consommation(idConsoTest);
        c.setDate_consommation(now);
        c.setType_repas("DINER");
        c.setId_aliment(idAlimentTemp); // On réutilise le même ID valide
        c.setQuantite_eau_ml(600);
        c.setPoids_grammes(300);

        sc.modifier(c);

        List<Consommation> liste = sc.afficherTout();
        boolean trouve = liste.stream()
                .anyMatch(conso -> conso.getType_repas().equals("DINER") && conso.getId_consommation() == idConsoTest);
        assertTrue(trouve, "La modification n'a pas été prise en compte.");
    }

    @Test
    @Order(3)
    void testSupprimerConsommation() {
        sc.supprimer(idConsoTest);

        List<Consommation> liste = sc.afficherTout();
        boolean existe = liste.stream().anyMatch(conso -> conso.getId_consommation() == idConsoTest);
        assertFalse(existe, "La suppression n'a pas fonctionné.");
    }

    // ==========================================
    // Nettoyage de la base à la fin du test
    // ==========================================
    @AfterAll
    static void cleanUpAll() {
        // On supprime l'aliment bidon de la base pour la garder propre !
        if (idAlimentTemp > 0) {
            sa.supprimerParId(idAlimentTemp);
            System.out.println("🧹 Aliment temporaire supprimé après les tests de consommation.");
        }
    }
}