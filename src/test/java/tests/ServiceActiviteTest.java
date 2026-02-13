package tests;

import entities.Activite;
import org.junit.jupiter.api.*;
import services.ServiceActivite;
import java.sql.Timestamp;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceActiviteTest {

    static ServiceActivite sa;
    static int idActiviteTest;

    @BeforeAll
    static void setup() {
        sa = new ServiceActivite();
    }

    @Test
    @Order(1)
    void testAjouterActivite() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        // id_exercice = 1 (doit exister dans ta BD)
        Activite a = new Activite(now, 1, 60, 999, 3, 10, 50.0f);
        sa.ajouter(a);

        List<Activite> liste = sa.afficherTout();
        assertFalse(liste.isEmpty());
        // On cherche par le nombre de calories_brulees (999) qu'on a mis pour le test
        assertTrue(liste.stream().anyMatch(act -> act.getCalories_brulees() == 999));

        Activite ajoute = liste.stream()
                .filter(act -> act.getCalories_brulees() == 999)
                .findFirst().orElse(null);

        if (ajoute != null) {
            idActiviteTest = ajoute.getId_activite();
        }
    }

    @Test
    @Order(2)
    void testModifierActivite() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Activite a = new Activite();
        a.setId_activite(idActiviteTest);
        a.setDate_activite(now);
        a.setId_exercice(1);
        a.setDuree_minutes(45);
        a.setCalories_brulees(888); // On modifie les calories pour vérifier
        a.setNb_series(4);
        a.setNb_repetitions(12);
        a.setPoids(55.0f);

        sa.modifier(a);

        List<Activite> liste = sa.afficherTout();
        boolean trouve = liste.stream().anyMatch(act -> act.getCalories_brulees() == 888);
        assertTrue(trouve);
    }

    @Test
    @Order(3)
    void testSupprimerActivite() {
        sa.supprimer(idActiviteTest);

        List<Activite> liste = sa.afficherTout();
        boolean existe = liste.stream().anyMatch(act -> act.getId_activite() == idActiviteTest);
        assertFalse(existe);
    }
}