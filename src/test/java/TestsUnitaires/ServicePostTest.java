package TestsUnitaires;

import models.Categorie;
import models.Post;
import org.junit.jupiter.api.*;
import services.ServiceCategorie;
import services.ServicePost;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServicePostTest {
    static ServicePost servicePost;
    static ServiceCategorie serviceCategorie;

    static int idPostTest;
    static int idCategorieTest;

    // =============================
    // Initialisation
    // =============================
    @BeforeAll
    static void setup() {

        servicePost = new ServicePost();
        serviceCategorie = new ServiceCategorie();

        // 👉 créer une catégorie test
        Categorie c = new Categorie();
        c.setNomCategorie("CategoriePostTest");
        c.setDescription("Categorie pour test Post");
        c.setDateCreation(LocalDateTime.now());

        serviceCategorie.add(c);

        idCategorieTest = c.getIdCategorie();
    }

    // =============================
    // TEST ADD POST
    // =============================
    @Test
    @Order(1)
    void testAjouterPost() {

        Post p = new Post();

        p.setTitre("PostTest");
        p.setContenu("Contenu Test");
        p.setDateCreation(LocalDateTime.now());
        p.setIdCategorie(idCategorieTest);

        // ⚠️ Mets un id_etudiant existant dans ta BD
        p.setIdEtudiant(1);

        servicePost.add(p);

        List<Post> posts = servicePost.getAll();

        Post trouve = posts.stream()
                .filter(post -> post.getTitre().equals("PostTest"))
                .findFirst()
                .orElse(null);

        assertNotNull(trouve);

        idPostTest = trouve.getIdPost();
    }

    // =============================
    // TEST UPDATE POST
    // =============================
    @Test
    @Order(2)
    void testModifierPost() {

        Post p = new Post();

        p.setIdPost(idPostTest);
        p.setTitre("PostModifie");
        p.setContenu("Contenu Modifie");

        servicePost.update(p);

        List<Post> posts = servicePost.getAll();

        boolean trouve = posts.stream()
                .anyMatch(post ->
                        post.getIdPost() == idPostTest &&
                                post.getTitre().equals("PostModifie")
                );

        assertTrue(trouve);
    }

    // =============================
    // TEST DELETE POST
    // =============================
    @Test
    @Order(3)
    void testSupprimerPost() {

        servicePost.deleteById(idPostTest);

        List<Post> posts = servicePost.getAll();

        boolean existe = posts.stream()
                .anyMatch(post -> post.getIdPost() == idPostTest);

        assertFalse(existe);
    }

    // =============================
    // Nettoyage final
    // =============================
    @AfterAll
    static void cleanUp() {

        // supprimer la catégorie test
        Categorie c = new Categorie();
        c.setIdCategorie(idCategorieTest);

        serviceCategorie.delete(c);
    }


}
