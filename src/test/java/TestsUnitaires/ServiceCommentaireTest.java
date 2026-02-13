package TestsUnitaires;


import models.Categorie;
import models.Commentaire;
import models.Post;
import org.junit.jupiter.api.*;
import services.ServiceCategorie;
import services.ServiceCommentaire;
import services.ServicePost;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceCommentaireTest {
    static ServiceCommentaire serviceCommentaire;
    static ServicePost servicePost;
    static ServiceCategorie serviceCategorie;

    static int idCategorieTest;
    static int idPostTest;
    static int idCommentaireTest;

    // =============================
    // Initialisation
    // =============================
    @BeforeAll
    static void setup() {

        serviceCommentaire = new ServiceCommentaire();
        servicePost = new ServicePost();
        serviceCategorie = new ServiceCategorie();

        // ---------- Créer categorie ----------
        Categorie c = new Categorie();
        c.setNomCategorie("CategorieCommentaireTest");
        c.setDescription("Test Commentaire");
        c.setDateCreation(LocalDateTime.now());

        serviceCategorie.add(c);
        idCategorieTest = c.getIdCategorie();

        // ---------- Créer post ----------
        Post p = new Post();
        p.setTitre("PostCommentaireTest");
        p.setContenu("Post pour test commentaire");
        p.setDateCreation(LocalDateTime.now());
        p.setIdCategorie(idCategorieTest);

        // ⚠️ doit exister dans ta BD
        p.setIdEtudiant(1);

        servicePost.add(p);

        // récupérer id post
        idPostTest = servicePost.getAll().stream()
                .filter(post -> post.getTitre().equals("PostCommentaireTest"))
                .findFirst()
                .get()
                .getIdPost();
    }

    // =============================
    // TEST ADD COMMENTAIRE
    // =============================
    @Test
    @Order(1)
    void testAjouterCommentaire() {

        Commentaire com = new Commentaire();

        com.setContenu("Commentaire Test");
        com.setDateCommentaire(LocalDateTime.now());
        com.setIdPost(idPostTest);

        // ⚠️ doit exister dans la BD
        com.setIdEtudiant(1);

        serviceCommentaire.add(com);

        List<Commentaire> commentaires = serviceCommentaire.getAll();

        Commentaire trouve = commentaires.stream()
                .filter(c -> c.getContenu().equals("Commentaire Test"))
                .findFirst()
                .orElse(null);

        assertNotNull(trouve);

        idCommentaireTest = trouve.getIdCommentaire();
    }

    // =============================
    // TEST UPDATE COMMENTAIRE
    // =============================
    @Test
    @Order(2)
    void testModifierCommentaire() {

        Commentaire com = new Commentaire();

        com.setIdCommentaire(idCommentaireTest);
        com.setContenu("Commentaire Modifie");

        serviceCommentaire.update(com);

        List<Commentaire> commentaires = serviceCommentaire.getAll();

        boolean trouve = commentaires.stream()
                .anyMatch(c ->
                        c.getIdCommentaire() == idCommentaireTest &&
                                c.getContenu().equals("Commentaire Modifie")
                );

        assertTrue(trouve);
    }

    // =============================
    // TEST DELETE COMMENTAIRE
    // =============================
    @Test
    @Order(3)
    void testSupprimerCommentaire() {

        Commentaire com = new Commentaire();
        com.setIdCommentaire(idCommentaireTest);

        serviceCommentaire.delete(com);

        List<Commentaire> commentaires = serviceCommentaire.getAll();

        boolean existe = commentaires.stream()
                .anyMatch(c -> c.getIdCommentaire() == idCommentaireTest);

        assertFalse(existe);
    }

    // =============================
    // Nettoyage final
    // =============================
    @AfterAll
    static void cleanUp() {

        // supprimer post
        servicePost.deleteById(idPostTest);

        // supprimer categorie
        Categorie c = new Categorie();
        c.setIdCategorie(idCategorieTest);

        serviceCategorie.delete(c);
    }

}
