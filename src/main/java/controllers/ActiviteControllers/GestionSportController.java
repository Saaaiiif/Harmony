package controllers.ActiviteControllers;

import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import models.ActiviteModels.Exercice;
import services.ActiviteServices.ServiceExercice;

public class GestionSportController {

    @FXML private TextField searchField;
    @FXML private ToggleGroup filterGroupSexe;
    @FXML private ToggleGroup filterGroupType;
    @FXML private ToggleButton btnSexeTous;
    @FXML private ToggleButton btnTypeTous;
    @FXML private SVGPath svgSortIcon;
    @FXML private FlowPane cardsFlowPane;
    @FXML private StackPane overlayPane;

    private ServiceExercice serviceExercice = new ServiceExercice();
    private ObservableList<Exercice> masterData = FXCollections.observableArrayList();
    private FilteredList<Exercice> filteredData;
    private SortedList<Exercice> sortedData;

    private boolean sortTypeAscending = true;

    private final String THEME_DARK  = "#4a148c";
    private final String THEME_LIGHT = "#8e24aa";

    @FXML
    public void initialize() {
        applyCustomToggleStyles();
        loadDataFromDatabase();
        setupSearchFilter();
        setupFilterListeners();
    }

    private void applyCustomToggleStyles() {
        filterGroupSexe.getToggles().forEach(toggle -> {
            ToggleButton btn = (ToggleButton) toggle;
            updateToggleStyle(btn);
            btn.selectedProperty().addListener((obs, oldVal, newVal) -> updateToggleStyle(btn));
        });
        filterGroupType.getToggles().forEach(toggle -> {
            ToggleButton btn = (ToggleButton) toggle;
            updateToggleStyle(btn);
            btn.selectedProperty().addListener((obs, oldVal, newVal) -> updateToggleStyle(btn));
        });
    }

    private void updateToggleStyle(ToggleButton btn) {
        if (btn.isSelected()) {
            btn.setStyle("-fx-background-color: " + THEME_LIGHT + "; -fx-text-fill: white; -fx-background-radius: 15; -fx-padding: 8 20; -fx-font-weight: bold; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(142,36,170,0.4), 10, 0, 0, 3);");
        } else {
            btn.setStyle("-fx-background-color: white; -fx-text-fill: " + THEME_DARK + "; -fx-border-color: #f3e5f5; -fx-border-radius: 15; -fx-background-radius: 15; -fx-padding: 8 20; -fx-font-weight: bold; -fx-cursor: hand;");
        }
    }

    private void loadDataFromDatabase() {
        masterData.setAll(serviceExercice.afficherTout());
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        setupSorting();
    }

    private void setupSearchFilter() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> filterData());
    }

    private void setupFilterListeners() {
        filterGroupSexe.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) btnSexeTous.setSelected(true);
            filterData();
        });
        filterGroupType.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) btnTypeTous.setSelected(true);
            filterData();
        });
    }

    private void filterData() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        ToggleButton selectedSexe = (ToggleButton) filterGroupSexe.getSelectedToggle();
        String filtreSexe = selectedSexe != null ? selectedSexe.getText().replaceAll("[^a-zA-Z]", "").trim() : "Tous";
        ToggleButton selectedType = (ToggleButton) filterGroupType.getSelectedToggle();
        String filtreType = selectedType != null ? selectedType.getText().replace(" ", "_") : "Toutes";

        filteredData.setPredicate(ex -> {
            boolean matchSearch = ex.getNom_exercice().toLowerCase().contains(searchText);
            boolean matchSexe   = filtreSexe.equals("Tous") || (ex.getType_exercice() != null && ex.getType_exercice().contains(filtreSexe));
            boolean matchType   = filtreType.equals("Toutes") || (ex.getType_exercice() != null && ex.getType_exercice().startsWith(filtreType));
            return matchSearch && matchSexe && matchType;
        });
        refreshCards();
    }

    private void setupSorting() {
        sortedData.setComparator((e1, e2) -> {
            int result = e1.getNom_exercice().compareToIgnoreCase(e2.getNom_exercice());
            return sortTypeAscending ? result : -result;
        });
        refreshCards();
    }

    @FXML
    void toggleSort(ActionEvent event) {
        sortTypeAscending = !sortTypeAscending;
        svgSortIcon.setContent(sortTypeAscending
                ? "M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z"
                : "M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z");
        setupSorting();
    }

    private void refreshCards() {
        cardsFlowPane.getChildren().clear();
        for (Exercice ex : sortedData) {
            cardsFlowPane.getChildren().add(createBeautifulCard(ex));
        }
    }

    // ── Carte exercice ────────────────────────────────────────────────────────

    private VBox createBeautifulCard(Exercice ex) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 20; " +
                "-fx-pref-width: 190; -fx-pref-height: 190; -fx-border-color: " + THEME_LIGHT + "33; " +
                "-fx-border-width: 2; -fx-border-radius: 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4);");
        card.setAlignment(Pos.CENTER);

        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 20; " +
                    "-fx-pref-width: 190; -fx-pref-height: 190; -fx-border-color: " + THEME_LIGHT + "; " +
                    "-fx-border-width: 3; -fx-border-radius: 20; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(142,36,170,0.3), 15, 0, 0, 6);");
            card.setTranslateY(-5);
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 20; " +
                    "-fx-pref-width: 190; -fx-pref-height: 190; -fx-border-color: " + THEME_LIGHT + "33; " +
                    "-fx-border-width: 2; -fx-border-radius: 20; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 10, 0, 0, 4);");
            card.setTranslateY(0);
        });

        // Emoji selon type
        String emoji = "💪";
        if (ex.getType_exercice() != null) {
            String t = ex.getType_exercice().toLowerCase();
            if (t.contains("cardio"))     emoji = "🏃";
            else if (t.contains("souplesse")) emoji = "🧘";
            else if (t.contains("endurance")) emoji = "🚴";
            else if (t.contains("perte"))     emoji = "🔥";
        }
        String sexeEmoji = (ex.getType_exercice() != null && ex.getType_exercice().contains("Femme")) ? "👩" : "👨";

        Label lblEmoji = new Label(emoji);
        lblEmoji.setStyle("-fx-font-size: 32px;");

        Label lblNom = new Label(ex.getNom_exercice());
        lblNom.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + THEME_DARK + ";");
        lblNom.setWrapText(true);
        lblNom.setAlignment(Pos.CENTER);

        Label lblSexe = new Label(sexeEmoji + " " + (ex.getType_exercice() != null ? ex.getType_exercice() : ""));
        lblSexe.setStyle("-fx-font-size: 11px; -fx-text-fill: #9e9e9e;");

        // Boutons action
        Button btnVideo = new Button("▶ Vidéo");
        btnVideo.setStyle("-fx-background-color: " + THEME_LIGHT + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-size: 11px; -fx-padding: 4 10; -fx-cursor: hand;");
        btnVideo.setOnAction(e -> showVideoPopup(ex));

        Button btnEdit = new Button("✏");
        btnEdit.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1565c0; -fx-background-radius: 8; -fx-font-size: 12px; -fx-padding: 4 8; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> showAddEditForm(ex));

        Button btnDel = new Button("🗑");
        btnDel.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-background-radius: 8; -fx-font-size: 12px; -fx-padding: 4 8; -fx-cursor: hand;");
        btnDel.setOnAction(e -> showDeleteConfirmation(ex));

        HBox actions = new HBox(6, btnEdit, btnDel);
        actions.setAlignment(Pos.CENTER);

        card.getChildren().addAll(lblEmoji, lblNom, lblSexe, btnVideo, actions);
        return card;
    }

    // ── Formulaire ajout / modification ──────────────────────────────────────

    @FXML
    void showAddForm(ActionEvent event) {
        showAddEditForm(null);
    }

    private void showAddEditForm(Exercice ex) {
        VBox dialog = new VBox(15);
        dialog.setMaxWidth(420);
        dialog.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 20; " +
                "-fx-border-color: " + THEME_LIGHT + "; -fx-border-width: 3; -fx-border-radius: 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(142,36,170,0.4), 20, 0, 0, 0);");
        dialog.setAlignment(Pos.CENTER);

        Label title = new Label(ex == null ? "✨ Nouvel Exercice" : "✏️ Modifier Exercice");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + THEME_DARK + ";");

        TextField tfNom = new TextField(ex != null ? ex.getNom_exercice() : "");
        tfNom.setPromptText("Nom (ex: Squats)");
        tfNom.setStyle("-fx-font-size: 14px; -fx-background-radius: 10; -fx-padding: 8;");

        ComboBox<String> comboSexe = new ComboBox<>(FXCollections.observableArrayList("Femme", "Homme"));
        comboSexe.setPromptText("Cible (Sexe)");
        comboSexe.setStyle("-fx-font-size: 14px; -fx-background-radius: 10;");

        ComboBox<String> comboType = new ComboBox<>(FXCollections.observableArrayList("Force", "Cardio", "Souplesse", "Endurance", "Perte_Poids"));
        comboType.setPromptText("Catégorie");
        comboType.setStyle("-fx-font-size: 14px; -fx-background-radius: 10;");

        if (ex != null && ex.getType_exercice() != null) {
            String[] parts = ex.getType_exercice().split("_");
            if (parts.length > 0) comboType.setValue(parts[0]);
            if (parts.length > 1) comboSexe.setValue(parts[parts.length - 1]);
        }

        TextField tfVideo = new TextField(ex != null ? ex.getVideo_exercice() : "");
        tfVideo.setPromptText("Nom vidéo (ex: squat.mp4)");
        tfVideo.setStyle("-fx-font-size: 14px; -fx-background-radius: 10; -fx-padding: 8;");

        Button btnSave = new Button("💾 Sauvegarder");
        btnSave.setStyle("-fx-background-color: " + THEME_LIGHT + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 25; -fx-background-radius: 15; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            String finalType = (comboType.getValue() != null ? comboType.getValue() : "") +
                    "_" + (comboSexe.getValue() != null ? comboSexe.getValue() : "");
            if (ex == null) {
                serviceExercice.ajouter(new Exercice(tfNom.getText(), finalType, tfVideo.getText()));
            } else {
                ex.setNom_exercice(tfNom.getText());
                ex.setType_exercice(finalType);
                ex.setVideo_exercice(tfVideo.getText());
                serviceExercice.modifier(ex);
            }
            closeDialog();
            loadDataFromDatabase();
        });

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 25; -fx-background-radius: 15; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> closeDialog());

        HBox combos = new HBox(15, comboType, comboSexe);
        combos.setAlignment(Pos.CENTER);
        HBox btnBox = new HBox(20, btnCancel, btnSave);
        btnBox.setAlignment(Pos.CENTER);

        dialog.getChildren().addAll(title, tfNom, combos, tfVideo, btnBox);
        showDialog(dialog);
    }

    private void showDeleteConfirmation(Exercice ex) {
        VBox dialog = new VBox(20);
        dialog.setMaxWidth(380);
        dialog.setMaxHeight(200);
        dialog.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 20; " +
                "-fx-border-color: #ef4444; -fx-border-width: 3; -fx-border-radius: 20; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(239,68,68,0.4), 20, 0, 0, 0);");
        dialog.setAlignment(Pos.CENTER);

        Label icon  = new Label("⚠️"); icon.setStyle("-fx-font-size: 35px;");
        Label title = new Label("Suppression"); title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
        Label desc  = new Label("Supprimer l'exercice '" + ex.getNom_exercice() + "' ?");
        desc.setStyle("-fx-text-alignment: center; -fx-font-size: 14px; -fx-text-fill: #333;");
        desc.setWrapText(true);

        Button btnConf = new Button("Confirmer");
        btnConf.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 15; -fx-padding: 8 25; -fx-cursor: hand;");
        btnConf.setOnAction(e -> { serviceExercice.supprimer(ex.getId_exercice()); closeDialog(); loadDataFromDatabase(); });

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #555; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 15; -fx-padding: 8 25; -fx-cursor: hand;");
        btnAnnuler.setOnAction(e -> closeDialog());

        HBox btnBox = new HBox(15, btnAnnuler, btnConf);
        btnBox.setAlignment(Pos.CENTER);
        dialog.getChildren().addAll(icon, title, desc, btnBox);
        showDialog(dialog);
    }

    // ── Lecteur vidéo ─────────────────────────────────────────────────────────

    private void showVideoPopup(Exercice ex) {
        Stage dialog = new Stage();
        dialog.initOwner(cardsFlowPane.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(15);
        root.setStyle("-fx-background-color: rgba(20,20,20,0.95); -fx-padding: 20; -fx-background-radius: 15; " +
                "-fx-border-color: " + THEME_LIGHT + "; -fx-border-width: 3; -fx-border-radius: 15; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.7), 20, 0, 0, 0);");
        root.setAlignment(Pos.CENTER);

        Label title = new Label("Vidéo : " + ex.getNom_exercice());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 22px; -fx-text-fill: white;");

        MediaView mediaView = new MediaView();
        MediaPlayer mediaPlayer = null;

        try {
            String path = ex.getVideo_exercice();
            if (path == null || path.trim().isEmpty()) throw new Exception("Aucune vidéo.");
            Media media;
            if (path.startsWith("http")) {
                media = new Media(path);
            } else {
                if (!path.toLowerCase().endsWith(".mp4")) path += ".mp4";
                java.net.URL videoUrl = getClass().getResource("/views/ActiviteViews/videos/" + path);
                if (videoUrl == null) throw new Exception("Introuvable.");
                media = new Media(videoUrl.toExternalForm());
            }
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaView.setFitWidth(800);
            mediaView.setFitHeight(500);
            mediaView.setPreserveRatio(true);
            mediaPlayer.play();
        } catch (Exception e) {
            title.setText("Vidéo Introuvable ⚠️");
            title.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 22px; -fx-font-weight: bold;");
        }

        MediaPlayer finalMediaPlayer = mediaPlayer;

        Button btnPlayPause = new Button("⏸ Pause");
        btnPlayPause.setStyle("-fx-background-color: " + THEME_LIGHT + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand;");

        Button btnReplay = new Button("🔄 Rejouer");
        btnReplay.setStyle("-fx-background-color: " + THEME_DARK + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand;");

        if (finalMediaPlayer != null) {
            btnPlayPause.setOnAction(e -> {
                if (finalMediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    finalMediaPlayer.pause(); btnPlayPause.setText("▶ Reprendre");
                } else {
                    finalMediaPlayer.play(); btnPlayPause.setText("⏸ Pause");
                }
            });
            btnReplay.setOnAction(e -> {
                finalMediaPlayer.seek(Duration.ZERO);
                finalMediaPlayer.play();
                btnPlayPause.setText("⏸ Pause");
            });
        }

        Button btnClose = new Button("❌ Fermer");
        btnClose.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand;");
        btnClose.setOnAction(e -> { if (finalMediaPlayer != null) finalMediaPlayer.stop(); dialog.close(); });

        HBox controls = new HBox(20, btnPlayPause, btnReplay, btnClose);
        controls.setAlignment(Pos.CENTER);
        root.getChildren().addAll(title, mediaView, controls);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ── Overlay dialog ────────────────────────────────────────────────────────

    private void showDialog(VBox dialog) {
        overlayPane.getChildren().setAll(dialog);
        overlayPane.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), dialog);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private void closeDialog() {
        overlayPane.setVisible(false);
    }
}