package controllers.ForumControllers;

import controllers.UserControlleers.modifControl.AccueilController;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import models.ForumModels.Commentaire;
import models.UserModels.Session;
import models.UserModels.user;
import services.ForumServices.ServiceCommentaire;
import services.ForumServices.ServicePost;
import models.ForumModels.Post;

import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.concurrent.*;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import models.ForumModels.Categorie;
import services.ForumServices.ServiceReaction;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import services.ForumServices.TranslationService;


public class ForumPostsController {

    private ExecutorService executor = Executors.newFixedThreadPool(5);
    private Map<String, String> translationCache = new ConcurrentHashMap<>();

    @FXML private StackPane overlayContainer;
    private boolean isTranslatedSession = false;
    private TranslationService translationService = new TranslationService();
    private ServiceReaction serviceReaction = new ServiceReaction();

    @FXML private Label categorieTitle;
    @FXML private Button addPostBtn;
    @FXML private VBox postsContainer;
    @FXML private TextField searchField;

    private ServicePost servicePost = new ServicePost();
    private ServiceCommentaire serviceCommentaire = new ServiceCommentaire();
    private Categorie selectedCategorie;
    private AccueilController accueilController;

    // ✅ User connecté depuis la Session
    private int getCurrentUserId() {
        user u = Session.getInstance().getUser();
        return u != null ? u.getUser_id() : -1;
    }

    private String getCurrentUserName() {
        user u = Session.getInstance().getUser();
        if (u == null) return "Moi";
        return u.getUser_prenom() + " " + u.getUser_nom();
    }

    private String getCurrentUserImagePath() {
        user u = Session.getInstance().getUser();
        return u != null ? u.getUser_image_path() : null;
    }

    @FXML
    private void handleBack() {
        if (accueilController != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/views/ForumViews/ForumHome.fxml"));
                Parent forumHome = loader.load();
                ForumHomeController ctrl = loader.getController();
                ctrl.setAccueilController(accueilController);
                accueilController.setContent(forumHome);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void setAccueilController(AccueilController controller) { this.accueilController = controller; }

    public void setCategorie(Categorie categorie) {
        this.selectedCategorie = categorie;
        categorieTitle.setText(categorie.getNomCategorie());
        loadPostsFromDB();
    }

    @FXML
    public void initialize() {
        addPostBtn.setOnAction(e -> openAddPostForm());

        // ✅ Vérification null avant d'ajouter le listener
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.trim().isEmpty()) loadPostsFromDB();
                else displayPosts(servicePost.searchPosts(newVal));
            });
        }

        javafx.application.Platform.runLater(this::showModernTranslatePopup);
    }

    private void loadPostsFromDB() {
        postsContainer.getChildren().clear();
        if (selectedCategorie == null) return;
        for (Post p : servicePost.getPostsByCategorie(selectedCategorie.getIdCategorie()))
            addPostCard(p);
    }

    @FXML
    public void handleSearch() {
        if (searchField == null) return;
        String k = searchField.getText();
        if (k == null || k.trim().isEmpty()) loadPostsFromDB();
        else displayPosts(servicePost.searchPosts(k));
    }

    private void displayPosts(List<Post> posts) {
        postsContainer.getChildren().clear();
        for (Post p : posts) addPostCard(p);
    }

    private void openAddPostForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ForumViews/AddPost.fxml"));
            Parent root = loader.load();
            AddPostController ctrl = loader.getController();
            ctrl.setCategorie(selectedCategorie);
            ctrl.setCurrentUserId(getCurrentUserId()); // ✅ user connecté
            Stage stage = new Stage();
            stage.setTitle("Ajouter un Post");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadPostsFromDB();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void openAddCommentForm(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ForumViews/AddComment.fxml"));
            Parent root = loader.load();
            AddCommentController ctrl = loader.getController();
            ctrl.setPost(post);
            ctrl.setCurrentUserId(getCurrentUserId()); // ✅ user connecté
            Stage stage = new Stage();
            stage.setTitle("Commenter");
            stage.setScene(new Scene(root));
            stage.showAndWait();
            loadPostsFromDB();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ══════════════════════════════════════════════════════
    //  CARTE POST — Style Reddit
    // ══════════════════════════════════════════════════════
    private void addPostCard(Post post) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 18; " +
                        "-fx-background-radius: 12; -fx-border-radius: 12; " +
                        "-fx-border-color: #e5e7eb; " +
                        "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,3);");

        // Header auteur
        HBox authorRow = new HBox(10);
        authorRow.setAlignment(Pos.CENTER_LEFT);

        StackPane authorAvatar = buildAvatarPane(post.getAuteurImagePath(), post.getNomEtudiant(), 36);

        VBox authorInfo = new VBox(1);
        Label authorName = new Label(post.getNomEtudiant());
        authorName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #111827;");
        String dateStr = post.getDateCreation() != null ? post.getDateCreation().toLocalDate().toString() : "";
        Label dateLabel = new Label("• " + dateStr);
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
        authorInfo.getChildren().addAll(authorName, dateLabel);
        authorRow.getChildren().addAll(authorAvatar, authorInfo);

        // Titre
        Label titleLbl = new Label(post.getTitre());
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        titleLbl.setWrapText(true);

        // Contenu
        Label contentLbl = new Label(post.getContenu());
        contentLbl.setWrapText(true);
        contentLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");

        card.getChildren().addAll(authorRow, titleLbl, contentLbl);

        // Image
        if (post.getImagePath() != null && !post.getImagePath().isEmpty()) {
            try {
                ImageView img = new ImageView(new Image(new File(post.getImagePath()).toURI().toString()));
                img.setFitWidth(560); img.setPreserveRatio(true);
                card.getChildren().add(img);
            } catch (Exception ignored) {}
        }

        // Action bar
        int likesCount = serviceReaction.countLikes(post.getIdPost());
        boolean liked = serviceReaction.isLiked(post.getIdPost(), getCurrentUserId());
        List<Commentaire> comments = serviceCommentaire.getCommentairesByPost(post.getIdPost());

        Button likeBtn = new Button((liked ? "❤️" : "🤍") + "  " + likesCount);
        styleLikeBtn(likeBtn, liked);
        likeBtn.setOnAction(e -> {
            boolean nowLiked = serviceReaction.isLiked(post.getIdPost(), getCurrentUserId());
            if (nowLiked) serviceReaction.unlikePost(post.getIdPost(), getCurrentUserId());
            else serviceReaction.likePost(post.getIdPost(), getCurrentUserId());
            boolean newLiked = !nowLiked;
            likeBtn.setText((newLiked ? "❤️" : "🤍") + "  " + serviceReaction.countLikes(post.getIdPost()));
            styleLikeBtn(likeBtn, newLiked);
        });

        Button commentBtn = new Button("💬  " + comments.size() + " commentaire(s)");
        commentBtn.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280; " +
                "-fx-background-radius: 20; -fx-padding: 6 14; -fx-font-size: 12px; -fx-cursor: hand;");

        HBox actionBar = new HBox(10, likeBtn, commentBtn);
        actionBar.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(actionBar);

        // Section commentaires
        VBox commentsSection = buildCommentsSection(comments, post);
        commentsSection.setVisible(false);
        commentsSection.setManaged(false);

        commentBtn.setOnAction(e -> {
            commentsSection.setVisible(!commentsSection.isVisible());
            commentsSection.setManaged(commentsSection.isVisible());
        });

        card.getChildren().add(commentsSection);
        postsContainer.getChildren().add(card);
    }

    private void styleLikeBtn(Button btn, boolean liked) {
        btn.setStyle(
                "-fx-background-color: " + (liked ? "#fce7f3" : "#f3f4f6") + "; " +
                        "-fx-text-fill: " + (liked ? "#be185d" : "#6b7280") + "; " +
                        "-fx-background-radius: 20; -fx-padding: 6 14; -fx-font-size: 12px; -fx-cursor: hand;");
    }

    // ══════════════════════════════════════════════════════
    //  SECTION COMMENTAIRES COMPLÈTE
    // ══════════════════════════════════════════════════════
    private VBox buildCommentsSection(List<Commentaire> comments, Post post) {
        VBox section = new VBox(0);
        section.setStyle("-fx-padding: 10 0 0 0;");

        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
        section.getChildren().add(sep);

        // Commentaires
        for (Commentaire c : comments) {
            section.getChildren().add(buildCommentRow(c, post));
        }

        // Zone d'ajout avec avatar user connecté
        HBox addRow = new HBox(10);
        addRow.setAlignment(Pos.CENTER_LEFT);
        addRow.setStyle("-fx-padding: 10 0 4 0;");

        StackPane myAvatar = buildAvatarPane(getCurrentUserImagePath(), getCurrentUserName(), 30);

        Button writeBtn = new Button("✏️  Écrire un commentaire...");
        writeBtn.setStyle(
                "-fx-background-color: #f9fafb; -fx-text-fill: #9ca3af; " +
                        "-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #e5e7eb; " +
                        "-fx-padding: 7 18; -fx-font-size: 12px; -fx-cursor: hand;");
        HBox.setHgrow(writeBtn, Priority.ALWAYS);
        writeBtn.setMaxWidth(Double.MAX_VALUE);

        writeBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ForumViews/AddComment.fxml"));
                Parent root = loader.load();
                AddCommentController ctrl = loader.getController();
                ctrl.setPost(post);
                ctrl.setCurrentUserId(getCurrentUserId());
                Stage stage = new Stage();
                stage.setScene(new Scene(root));
                stage.showAndWait();

                // Refresh inline
                section.getChildren().clear();
                section.getChildren().add(new javafx.scene.control.Separator());
                List<Commentaire> updated = serviceCommentaire.getCommentairesByPost(post.getIdPost());
                for (Commentaire c : updated) section.getChildren().add(buildCommentRow(c, post));
                HBox newAddRow = buildAddRow(post, section);
                section.getChildren().add(newAddRow);
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        addRow.getChildren().addAll(myAvatar, writeBtn);
        section.getChildren().add(addRow);
        return section;
    }

    private HBox buildAddRow(Post post, VBox section) {
        HBox addRow = new HBox(10);
        addRow.setAlignment(Pos.CENTER_LEFT);
        addRow.setStyle("-fx-padding: 10 0 4 0;");
        StackPane myAvatar = buildAvatarPane(getCurrentUserImagePath(), getCurrentUserName(), 30);
        Button writeBtn = new Button("✏️  Écrire un commentaire...");
        writeBtn.setStyle("-fx-background-color: #f9fafb; -fx-text-fill: #9ca3af; " +
                "-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #e5e7eb; " +
                "-fx-padding: 7 18; -fx-font-size: 12px; -fx-cursor: hand;");
        HBox.setHgrow(writeBtn, Priority.ALWAYS);
        writeBtn.setMaxWidth(Double.MAX_VALUE);
        writeBtn.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ForumViews/AddComment.fxml"));
                Parent root = loader.load();
                AddCommentController ctrl = loader.getController();
                ctrl.setPost(post);
                ctrl.setCurrentUserId(getCurrentUserId());
                new Stage() {{ setScene(new Scene(root)); showAndWait(); }};
                section.getChildren().clear();
                section.getChildren().add(new javafx.scene.control.Separator());
                for (Commentaire c : serviceCommentaire.getCommentairesByPost(post.getIdPost()))
                    section.getChildren().add(buildCommentRow(c, post));
                section.getChildren().add(buildAddRow(post, section));
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        addRow.getChildren().addAll(myAvatar, writeBtn);
        return addRow;
    }

    // ══════════════════════════════════════════════════════
    //  ROW COMMENTAIRE
    // ══════════════════════════════════════════════════════
    private VBox buildCommentRow(Commentaire c, Post post) {
        VBox box = new VBox(4);
        box.setStyle(
                "-fx-padding: 10 10 8 12; -fx-background-color: #fafafa; " +
                        "-fx-background-radius: 0 8 8 0; -fx-border-radius: 0 8 8 0;" +
                        "-fx-border-color: transparent transparent transparent #ede9fe; " +
                        "-fx-border-width: 0 0 0 3;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane avatar = buildAvatarPane(c.getAuteurImagePath(), c.getNomEtudiant(), 30);
        Label name = new Label(c.getNomEtudiant() != null ? c.getNomEtudiant() : "Anonyme");
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #374151;");
        String date = c.getDateCommentaire() != null ? "• " + c.getDateCommentaire().toLocalDate() : "";
        Label dateL = new Label(date);
        dateL.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");
        header.getChildren().addAll(avatar, name, dateL);

        Label content = new Label(c.getContenu());
        content.setWrapText(true);
        content.setStyle("-fx-font-size: 13px; -fx-text-fill: #4b5563; -fx-padding: 2 0 0 38;");

        Button reply = new Button("↩ Répondre");
        reply.setStyle("-fx-background-color: transparent; -fx-text-fill: #7c3aed; " +
                "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 1 0 0 38;");
        reply.setOnAction(e -> openAddCommentForm(post));

        box.getChildren().addAll(header, content, reply);
        return box;
    }

    // ══════════════════════════════════════════════════════
    //  AVATAR avec initiale colorée
    // ══════════════════════════════════════════════════════
    private StackPane buildAvatarPane(String imagePath, String nom, int size) {
        StackPane pane = new StackPane();
        pane.setPrefWidth(size);
        pane.setPrefHeight(size);
        pane.setMinWidth(size);
        pane.setMinHeight(size);
        pane.setMaxWidth(size);
        pane.setMaxHeight(size);
        pane.setStyle("-fx-background-color: #cccccc; -fx-background-radius: " + (size / 2) + ";");

        String initial = (nom != null && !nom.isEmpty()) ? String.valueOf(nom.charAt(0)).toUpperCase() : "?";
        Label lbl = new Label(initial);
        lbl.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + (size / 2) + "px;");
        pane.getChildren().add(lbl);

        if (imagePath != null && !imagePath.isEmpty()) {
            File f = new File(imagePath);
            if (f.exists()) {
                try {
                    ImageView img = new ImageView(new Image(f.toURI().toString(), size, size, false, true));
                    img.setFitWidth(size);
                    img.setFitHeight(size);
                    img.setClip(new Circle(size / 2.0, size / 2.0, size / 2.0));
                    pane.getChildren().add(img);
                } catch (Exception ignored) {}
            }
        }

        pane.setClip(new Circle(size / 2.0, size / 2.0, size / 2.0));
        return pane;
    }

    // ══════════════════════════════════════════════════════
    //  POPUP TRADUCTION
    // ══════════════════════════════════════════════════════
    private void showModernTranslatePopup() {
        if (overlayContainer == null) return;
        VBox popup = new VBox(12);
        popup.setMaxWidth(240);
        popup.setPadding(new Insets(14));
        popup.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.18),16,0.2,0,4);");
        Label msg = new Label("🌍  Traduire en anglais ?");
        msg.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        Button yesBtn = new Button("✓  Oui");
        yesBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 14;");
        Button noBtn = new Button("Non");
        noBtn.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 14;");
        popup.getChildren().addAll(msg, new HBox(10, yesBtn, noBtn));
        overlayContainer.getChildren().add(popup);
        StackPane.setAlignment(popup, Pos.TOP_RIGHT);
        StackPane.setMargin(popup, new Insets(16, 16, 0, 0));
        popup.setTranslateX(280);
        javafx.animation.TranslateTransition tt =
                new javafx.animation.TranslateTransition(javafx.util.Duration.millis(300), popup);
        tt.setToX(0); tt.play();
        yesBtn.setOnAction(e -> { overlayContainer.getChildren().remove(popup); translateEntirePageOptimized(); });
        noBtn.setOnAction(e -> overlayContainer.getChildren().remove(popup));
    }

    private void translateEntirePageOptimized() {
        isTranslatedSession = true;
        postsContainer.getChildren().clear();
        for (Post post : servicePost.getPostsByCategorie(selectedCategorie.getIdCategorie())) {
            executor.submit(() -> {
                String t = translationCache.computeIfAbsent(post.getTitre(), x -> safeTranslate(x));
                String c = translationCache.computeIfAbsent(post.getContenu(), x -> safeTranslate(x));
                javafx.application.Platform.runLater(() -> { post.setTitre(t); post.setContenu(c); addPostCard(post); });
            });
        }
    }

    private String safeTranslate(String text) {
        try { return translationService.translate(text, "fr", "en"); } catch (Exception e) { return text; }
    }
}
