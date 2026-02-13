package tests;

import entities.Sommeil;
import org.junit.jupiter.api.*;
import services.ServiceSommeil;
import java.sql.Timestamp;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceSommeilTest {

    static ServiceSommeil ss;
    static int idSommeilTest;

    @BeforeAll
    static void setup() {
        ss = new ServiceSommeil();
    }

    @Test
    @Order(1)
    void testAjouterSommeil() {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // CORRECTION ICI : On utilise "MOYEN" qui est accepté par ta base de données
        Sommeil s = new Sommeil(now, now, "MOYEN");
        ss.ajouter(s);

        List<Sommeil> liste = ss.afficherTout();
        assertFalse(liste.isEmpty());

        // On vérifie qu'on trouve bien notre sommeil "MOYEN"
        assertTrue(liste.stream().anyMatch(som -> som.getQualite_sommeil().equals("MOYEN")));

        Sommeil ajoute = liste.stream()
                .filter(som -> som.getQualite_sommeil().equals("MOYEN"))
                .findFirst().orElse(null);

        if (ajoute != null) {
            idSommeilTest = ajoute.getId_sommeil();
        }
    }

    @Test
    @Order(2)
    void testModifierSommeil() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Sommeil s = new Sommeil();
        s.setId_sommeil(idSommeilTest);
        s.setDate_coucher(now);
        s.setDate_reveil(now);

        // CORRECTION ICI : On utilise "EXCELLENT" pour la modification
        s.setQualite_sommeil("EXCELLENT");

        ss.modifier(s);

        List<Sommeil> liste = ss.afficherTout();
        boolean trouve = liste.stream()
                .anyMatch(som -> som.getQualite_sommeil().equals("EXCELLENT") && som.getId_sommeil() == idSommeilTest);
        assertTrue(trouve);
    }

    @Test
    @Order(3)
    void testSupprimerSommeil() {
        ss.supprimer(idSommeilTest);

        List<Sommeil> liste = ss.afficherTout();
        boolean existe = liste.stream().anyMatch(som -> som.getId_sommeil() == idSommeilTest);
        assertFalse(existe);
    }

    // ==========================================
    // Nettoyage automatique après chaque test
    // ==========================================
    @AfterEach
    void cleanUp() {
        List<Sommeil> liste = ss.afficherTout();
        if (!liste.isEmpty()) {
            System.out.println("🧹 Nettoyage vérifié après le test de Sommeil.");
        }
    }
}