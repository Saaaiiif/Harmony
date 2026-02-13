package TestsUnitaires;
import org.junit.jupiter.api.*;

import models.Categorie;
import org.junit.jupiter.api.*;
import services.ServiceCategorie;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class ServiceCategorieTest {


    static ServiceCategorie service;
    static int idCategorieTest; // servira pour update et delete

    // =============================
    // Initialisation du service
    // =============================
    @BeforeAll
    static void setup() {
        service = new ServiceCategorie();
    }

    // =============================
    // TEST ADD
    // =============================
    @Test
    @Order(1)
    void testAjouterCategorie() {

        Categorie c = new Categorie();
        c.setNomCategorie("TestCategorie");
        c.setDescription("Description Test");
        c.setDateCreation(LocalDateTime.now());

        service.add(c);

        List<Categorie> categories = service.getAll();

        assertFalse(categories.isEmpty());

        // récupérer la catégorie ajoutée
        Categorie trouve = categories.stream()
                .filter(cat -> cat.getNomCategorie().equals("TestCategorie"))
                .findFirst()
                .orElse(null);

        assertNotNull(trouve);

        idCategorieTest = c.getIdCategorie();

    }

    // =============================
    // TEST UPDATE
    // =============================
    @Test
    @Order(2)
    void testModifierCategorie() {

        Categorie c = new Categorie();
        c.setIdCategorie(idCategorieTest);
        c.setNomCategorie("CategorieModifiee");
        c.setDescription("Nouvelle Description");

        service.update(c);

        List<Categorie> categories = service.getAll();

        boolean trouve = categories.stream()
                .anyMatch(cat ->
                        cat.getIdCategorie() == idCategorieTest &&
                                cat.getNomCategorie().equals("CategorieModifiee")
                );


        assertTrue(trouve);
    }

    // =============================
    // TEST DELETE
    // =============================
    @Test
    @Order(3)
    void testSupprimerCategorie() {

        Categorie c = new Categorie();
        c.setIdCategorie(idCategorieTest);

        service.delete(c);

        List<Categorie> categories = service.getAll();

        boolean existe = categories.stream()
                .anyMatch(cat -> cat.getIdCategorie() == idCategorieTest);

        assertFalse(existe);
    }

    // =============================
    // Nettoyage automatique
    // =============================
//    @AfterEach
//    void cleanUp() {
//
//        List<Categorie> categories = service.getAll();
//
//        categories.stream()
//                .filter(cat -> cat.getIdCategorie() == idCategorieTest)
//
//                .forEach(service::delete);
//    }
    @AfterAll
    static void cleanUp() throws SQLException {
        service.deleteAll();
    }


}
