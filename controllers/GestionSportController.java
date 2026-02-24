package controllers;

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
import models.Exercice;
import services.ServiceExercice;

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

    @FXML public void initialize() {
        loadDataFromDatabase();
        setupSearchFilter();
        setupFilterListeners();
        setupSorting();
    }

    private void loadDataFromDatabase() {
        masterData.setAll(serviceExercice.afficherTout());
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        refreshCards();
    }

    private void setupSearchFilter() { searchField.textProperty().addListener((observable, oldValue, newValue) -> filterData()); }
    private void setupFilterListeners() {
        filterGroupSexe.selectedToggleProperty().addListener((obs, oldVal, newVal) -> { if (newVal == null) btnSexeTous.setSelected(true); filterData(); });
        filterGroupType.selectedToggleProperty().addListener((obs, oldVal, newVal) -> { if (newVal == null) btnTypeTous.setSelected(true); filterData(); });
    }

    private void filterData() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        ToggleButton selectedSexe = (ToggleButton) filterGroupSexe.getSelectedToggle();
        String filtreSexe = selectedSexe != null ? selectedSexe.getText() : "Tous";
        ToggleButton selectedType = (ToggleButton) filterGroupType.getSelectedToggle();
        String filtreType = selectedType != null ? selectedType.getText() : "Tous";

        filteredData.setPredicate(ex -> {
            boolean matchSearch = ex.getNom_exercice().toLowerCase().contains(searchText);
            boolean matchSexe = filtreSexe.equals("Tous") || ex.getType_exercice().contains(filtreSexe.replace("👨 ", "").replace("👩 ", ""));
            boolean matchType = filtreType.equals("Tous") || ex.getType_exercice().startsWith(filtreType.replace(" ", "_"));
            return matchSearch && matchSexe && matchType;
        });
        refreshCards();
    }

    private void setupSorting() {
        sortedData.comparatorProperty().bind(javafx.beans.binding.Bindings.createObjectBinding(() -> (e1, e2) -> {
            int result = e1.getNom_exercice().compareToIgnoreCase(e2.getNom_exercice());
            return sortTypeAscending ? result : -result;
        }, filterGroupSexe.selectedToggleProperty(), filterGroupType.selectedToggleProperty()));
    }

    @FXML void toggleSort(ActionEvent event) {
        sortTypeAscending = !sortTypeAscending;
        svgSortIcon.setContent(sortTypeAscending ? "M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z" : "M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z");
        refreshCards();
    }

    private void refreshCards() {
        cardsFlowPane.getChildren().clear();
        for (Exercice ex : sortedData) cardsFlowPane.getChildren().add(createCard(ex));
    }

    private VBox createCard(Exercice ex) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 20; -fx-pref-width: 170; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setAlignment(Pos.CENTER);

        Label iconVideo = new Label("🎬"); iconVideo.setStyle("-fx-font-size: 40px;");
        Label nameLabel = new Label(ex.getNom_exercice());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #333;");
        nameLabel.setWrapText(true); nameLabel.setAlignment(Pos.CENTER);
        Label typeLabel = new Label(ex.getType_exercice().replace("_", " "));
        typeLabel.setStyle("-fx-text-fill: " + (ex.getType_exercice().contains("Femme") ? "#e91e63" : "#1e88e5") + "; -fx-font-weight: bold; -fx-background-color: #f3e5f5; -fx-padding: 3 10; -fx-background-radius: 10; -fx-font-size: 10px;");

        HBox actions = new HBox(10); actions.setAlignment(Pos.CENTER);
        Button btnEye = new Button("👁️"); btnEye.setStyle("-fx-background-color: #e0f7fa; -fx-text-fill: #0097a7; -fx-background-radius: 50; -fx-cursor: hand;");
        btnEye.setOnAction(e -> showVideoPopup(ex));
        Button btnEdit = new Button("✎"); btnEdit.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1976d2; -fx-background-radius: 50; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> showFormDialog(ex));
        Button btnDelete = new Button("🗑"); btnDelete.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #d32f2f; -fx-background-radius: 50; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> showDeleteConfirmation(ex));

        actions.getChildren().addAll(btnEye, btnEdit, btnDelete);
        card.getChildren().addAll(iconVideo, nameLabel, typeLabel, actions);
        return card;
    }

    // --- LE LECTEUR VIDÉO MP4 ---
    private void showVideoPopup(Exercice ex) {
        Stage dialog = new Stage();
        dialog.initOwner(cardsFlowPane.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: #111; -fx-padding: 30; -fx-background-radius: 20; -fx-border-color: #6a1b9a; -fx-border-width: 4; -fx-border-radius: 20;");
        root.setAlignment(Pos.CENTER);

        Label title = new Label("Modèle 3D : " + ex.getNom_exercice());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 24px; -fx-text-fill: white;");

        MediaView mediaView = new MediaView();
        MediaPlayer mediaPlayer = null;
        try {
            Media media = new Media(ex.getVideo_exercice());
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaView.setFitWidth(600); mediaView.setPreserveRatio(true);
            mediaPlayer.play(); // Auto-play
        } catch (Exception e) {
            System.err.println("URL de vidéo invalide : " + ex.getVideo_exercice());
        }

        HBox controls = new HBox(15); controls.setAlignment(Pos.CENTER);
        MediaPlayer finalMediaPlayer = mediaPlayer;

        Button btnPlayPause = new Button("⏸ Pause");
        btnPlayPause.setStyle("-fx-background-color: #6a1b9a; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand;");
        Button btnReplay = new Button("🔄 Rejouer");
        btnReplay.setStyle("-fx-background-color: #0288d1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand;");

        if (finalMediaPlayer != null) {
            btnPlayPause.setOnAction(e -> {
                if (finalMediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    finalMediaPlayer.pause(); btnPlayPause.setText("▶ Reprendre");
                } else {
                    finalMediaPlayer.play(); btnPlayPause.setText("⏸ Pause");
                }
            });
            btnReplay.setOnAction(e -> {
                finalMediaPlayer.seek(javafx.util.Duration.ZERO);
                finalMediaPlayer.play(); btnPlayPause.setText("⏸ Pause");
            });
        }

        Button btnClose = new Button("❌ Fermer");
        btnClose.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20; -fx-cursor: hand;");
        btnClose.setOnAction(e -> { if(finalMediaPlayer != null) finalMediaPlayer.stop(); dialog.close(); });

        controls.getChildren().addAll(btnPlayPause, btnReplay, btnClose);
        root.getChildren().addAll(title, mediaView, controls);

        Scene scene = new Scene(root); scene.setFill(Color.TRANSPARENT); dialog.setScene(scene);
        dialog.showAndWait();
    }

    @FXML void showAddForm(ActionEvent event) { showFormDialog(null); }

    private void showFormDialog(Exercice ex) {
        VBox dialog = new VBox(20);
        dialog.setStyle("-fx-background-color: white; -fx-padding: 35; -fx-background-radius: 25; -fx-max-width: 400; -fx-border-color: #6a1b9a; -fx-border-width: 2; -fx-border-radius: 25;");
        dialog.setAlignment(Pos.CENTER);

        Label title = new Label(ex == null ? "✨ Nouvel Exercice" : "✏️ Modifier Exercice");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #4a148c;");

        TextField tfNom = new TextField(ex != null ? ex.getNom_exercice() : ""); tfNom.setPromptText("Nom (ex: Squats)");
        ComboBox<String> comboSexe = new ComboBox<>(FXCollections.observableArrayList("Femme", "Homme")); comboSexe.setPromptText("Cible");
        ComboBox<String> comboType = new ComboBox<>(FXCollections.observableArrayList("Force", "Cardio", "Souplesse", "Endurance", "Perte_Poids")); comboType.setPromptText("Catégorie");

        if (ex != null && ex.getType_exercice() != null) {
            String[] parts = ex.getType_exercice().split("_");
            if (parts.length > 0) comboType.setValue(parts[0]);
            if (parts.length > 1) comboSexe.setValue(parts[parts.length - 1]);
        }

        TextField tfVideo = new TextField(ex != null ? ex.getVideo_exercice() : ""); tfVideo.setPromptText("URL Vidéo MP4 (ex: http...mp4)");

        Button btnSave = new Button("💾 Sauvegarder");
        btnSave.setStyle("-fx-background-color: #6a1b9a; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 20; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            String finalType = comboType.getValue() + "_" + comboSexe.getValue();
            if (ex == null) serviceExercice.ajouter(new Exercice(tfNom.getText(), finalType, tfVideo.getText()));
            else { ex.setNom_exercice(tfNom.getText()); ex.setType_exercice(finalType); ex.setVideo_exercice(tfVideo.getText()); serviceExercice.modifier(ex); }
            closeDialog(); loadDataFromDatabase();
        });
        Button btnCancel = new Button("Annuler"); btnCancel.setOnAction(e -> closeDialog());

        dialog.getChildren().addAll(title, tfNom, comboSexe, comboType, tfVideo, new HBox(15, btnCancel, btnSave));
        showDialog(dialog);
    }

    private void showDeleteConfirmation(Exercice ex) {
        // ... (Même code de suppression que précédemment) ...
        VBox dialog = new VBox(20); dialog.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 20; -fx-border-color: #ef4444; -fx-border-width: 2; -fx-border-radius: 20;"); dialog.setAlignment(Pos.CENTER);
        Label title = new Label("🗑 Suppression"); title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
        Button btnConf = new Button("Confirmer"); btnConf.setOnAction(e -> { serviceExercice.supprimer(ex.getId_exercice()); closeDialog(); loadDataFromDatabase(); });
        Button btnAnnuler = new Button("Annuler"); btnAnnuler.setOnAction(e -> closeDialog());
        dialog.getChildren().addAll(title, new HBox(15, btnAnnuler, btnConf)); showDialog(dialog);
    }

    private void showDialog(VBox dialog) { overlayPane.getChildren().setAll(dialog); overlayPane.setVisible(true); }
    private void closeDialog() { overlayPane.setVisible(false); }
}