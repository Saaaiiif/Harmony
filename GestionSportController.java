package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import models.Exercice;
import services.ServiceExercice;

public class GestionSportController {

    @FXML private TextField tfNom;
    @FXML private ComboBox<String> comboType;
    @FXML private TextField tfImage;
    @FXML private Label lblErreurFormulaire;
    @FXML private TextField searchField;
    @FXML private ToggleGroup filterGroup;
    @FXML private ToggleButton btnFilterAll, btnFilterCardio, btnFilterMuscu;
    @FXML private SVGPath svgSortIcon;
    @FXML private FlowPane cardsFlowPane;
    @FXML private StackPane overlayPane;

    private ServiceExercice serviceExercice = new ServiceExercice();
    private ObservableList<Exercice> masterData = FXCollections.observableArrayList();
    private FilteredList<Exercice> filteredData;
    private SortedList<Exercice> sortedData;

    private boolean sortTypeAscending = true;

    // Icônes SVG
    private final String SVG_SORT_AZ = "M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z";
    private final String SVG_SORT_ZA = "M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z";
    private final String SVG_EDIT = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    private final String SVG_DEL = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";

    @FXML
    public void initialize() {
        comboType.setItems(FXCollections.observableArrayList("cardio", "musculation"));
        svgSortIcon.setContent(SVG_SORT_AZ);

        loadDataFromDB();
        setupFiltersAndSearch();
        setupSort();
        refreshDisplay();
    }

    private void loadDataFromDB() {
        masterData.clear();
        masterData.addAll(serviceExercice.afficherTout());
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
    }

    private void setupFiltersAndSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
        filterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
    }

    private void updatePredicate() {
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        ToggleButton selected = (ToggleButton) filterGroup.getSelectedToggle();

        filteredData.setPredicate(exo -> {
            String nom = exo.getNom_exercice() != null ? exo.getNom_exercice().toLowerCase() : "";
            boolean matchSearch = search.isEmpty() || nom.startsWith(search);

            String type = exo.getType_exercice() != null ? exo.getType_exercice().toLowerCase() : "";
            boolean matchType = true;
            if (selected == btnFilterCardio) matchType = type.equals("cardio");
            else if (selected == btnFilterMuscu) matchType = type.equals("musculation");

            return matchSearch && matchType;
        });
        refreshDisplay();
    }

    private void setupSort() {
        sortedData.setComparator((e1, e2) -> {
            String n1 = e1.getNom_exercice() != null ? e1.getNom_exercice().toLowerCase() : "";
            String n2 = e2.getNom_exercice() != null ? e2.getNom_exercice().toLowerCase() : "";
            return sortTypeAscending ? n1.compareTo(n2) : n2.compareTo(n1);
        });
    }

    @FXML
    void toggleSort(ActionEvent event) {
        sortTypeAscending = !sortTypeAscending;
        svgSortIcon.setContent(sortTypeAscending ? SVG_SORT_AZ : SVG_SORT_ZA);
        setupSort();
        refreshDisplay();
    }

    private void refreshDisplay() {
        cardsFlowPane.getChildren().clear();
        for (Exercice exo : sortedData) {
            cardsFlowPane.getChildren().add(createExerciseCard(exo));
        }
    }

    // =========================================================================
    // ======================== CARTES ET CRUD =================================
    // =========================================================================

    private VBox createExerciseCard(Exercice exo) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15px; -fx-padding: 15px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setPrefWidth(220);

        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = createIconButton(SVG_EDIT, "#3b82f6", "#eff6ff");
        btnEdit.setOnAction(e -> ouvrirModalModification(exo));

        Button btnDel = createIconButton(SVG_DEL, "#ef4444", "#fef2f2");
        btnDel.setOnAction(e -> demanderSuppression(exo));

        actionsBox.getChildren().addAll(btnEdit, btnDel);

        ImageView img = new ImageView();
        img.setFitWidth(190); img.setFitHeight(120);
        try {
            if (exo.getImage_exercice() != null && !exo.getImage_exercice().isEmpty())
                img.setImage(new Image(exo.getImage_exercice(), 190, 120, false, true));
            else img.setStyle("-fx-background-color: #f3f4f6;");
        } catch (Exception ex) { img.setStyle("-fx-background-color: #f3f4f6;"); }

        Label lblNom = new Label(exo.getNom_exercice());
        lblNom.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1f2937;");

        card.getChildren().addAll(actionsBox, img, lblNom);
        return card;
    }

    private Button createIconButton(String svg, String color, String hoverBg) {
        SVGPath path = new SVGPath();
        path.setContent(svg);
        path.setFill(Color.web(color));
        Button btn = new Button();
        btn.setGraphic(path);
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 50%; -fx-padding: 8;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + hoverBg + "; -fx-cursor: hand; -fx-background-radius: 50%; -fx-padding: 8;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-background-radius: 50%; -fx-padding: 8;"));
        return btn;
    }

    // =========================================================================
    // =================== MODALES PREMIUM INCROYABLES =========================
    // =========================================================================

    @FXML
    void demanderAjoutExercice(ActionEvent event) {
        String nom = tfNom.getText() != null ? tfNom.getText().trim() : "";
        String type = comboType.getValue();
        String img = tfImage.getText() != null ? tfImage.getText().trim() : "";

        if (nom.isEmpty() || type == null) {
            lblErreurFormulaire.setText("❌ Remplissez le nom et la catégorie.");
            return;
        }
        lblErreurFormulaire.setText("");

        VBox dialog = createPremiumDialog("✨", "Nouveau module", "Confirmez-vous l'ajout de : " + nom + " ?", "#10b981", "#d1fae5");

        Button btnConfirmer = createPremiumButton("Confirmer l'ajout", "linear-gradient(to right, #10b981, #059669)");
        btnConfirmer.setOnAction(e -> {
            serviceExercice.ajouter(new Exercice(nom, type, img));
            tfNom.clear(); comboType.setValue(null); tfImage.clear();
            loadDataFromDB(); updatePredicate(); setupSort(); refreshDisplay();
            closeDialog();
        });

        Button btnAnnuler = createPremiumButton("Annuler", "#9ca3af");
        btnAnnuler.setOnAction(e -> closeDialog());

        HBox btnBox = new HBox(15, btnAnnuler, btnConfirmer);
        btnBox.setAlignment(Pos.CENTER);
        dialog.getChildren().add(btnBox);

        showDialog(dialog);
    }

    private void ouvrirModalModification(Exercice exo) {
        VBox dialog = createPremiumDialog("✏️", "Modification", "Mettez à jour les données ci-dessous", "#3b82f6", "#dbeafe");

        TextField editNom = new TextField(exo.getNom_exercice());
        editNom.setStyle("-fx-padding: 12; -fx-background-color: #f3f4f6; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-size: 14px;");

        ComboBox<String> editType = new ComboBox<>(FXCollections.observableArrayList("cardio", "musculation"));
        editType.setValue(exo.getType_exercice() != null ? exo.getType_exercice().toLowerCase() : "musculation");
        editType.setStyle("-fx-padding: 8; -fx-background-color: #f3f4f6; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10;");
        editType.setPrefWidth(350);

        TextField editImg = new TextField(exo.getImage_exercice());
        editImg.setStyle("-fx-padding: 12; -fx-background-color: #f3f4f6; -fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-background-radius: 10; -fx-font-size: 14px;");

        VBox form = new VBox(15, editNom, editType, editImg);

        Button btnSauvegarder = createPremiumButton("Mettre à jour", "linear-gradient(to right, #3b82f6, #2563eb)");
        btnSauvegarder.setOnAction(e -> {
            exo.setNom_exercice(editNom.getText());
            exo.setType_exercice(editType.getValue());
            exo.setImage_exercice(editImg.getText());
            serviceExercice.modifier(exo);
            loadDataFromDB(); updatePredicate(); setupSort(); refreshDisplay();
            closeDialog();
        });

        Button btnAnnuler = createPremiumButton("Annuler", "#9ca3af");
        btnAnnuler.setOnAction(e -> closeDialog());

        HBox btnBox = new HBox(15, btnAnnuler, btnSauvegarder);
        btnBox.setAlignment(Pos.CENTER);

        dialog.getChildren().addAll(form, btnBox);
        showDialog(dialog);
    }

    private void demanderSuppression(Exercice exo) {
        VBox dialog = createPremiumDialog("🗑️", "Zone de danger", "La suppression de '" + exo.getNom_exercice() + "' est irréversible. Continuer ?", "#ef4444", "#fee2e2");

        Button btnSupprimer = createPremiumButton("Oui, supprimer", "linear-gradient(to right, #ef4444, #dc2626)");
        btnSupprimer.setOnAction(e -> {
            serviceExercice.supprimer(exo.getId_exercice());
            loadDataFromDB(); updatePredicate(); setupSort(); refreshDisplay();
            closeDialog();
        });

        Button btnAnnuler = createPremiumButton("Annuler", "#9ca3af");
        btnAnnuler.setOnAction(e -> closeDialog());

        HBox btnBox = new HBox(15, btnAnnuler, btnSupprimer);
        btnBox.setAlignment(Pos.CENTER);
        dialog.getChildren().add(btnBox);

        showDialog(dialog);
    }

    // --- MOTEUR DE DESIGN ET ANIMATION ---

    private VBox createPremiumDialog(String emoji, String titleStr, String descStr, String iconColor, String iconBg) {
        VBox dialog = new VBox(25);
        dialog.setStyle("-fx-background-color: linear-gradient(to bottom right, #ffffff, #f9fafb); -fx-background-radius: 25px; -fx-padding: 40px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 30, 0, 10, 15);");
        dialog.setMaxWidth(450);
        dialog.setAlignment(Pos.CENTER);

        // Badge Icone
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size: 35px; -fx-background-color: " + iconBg + "; -fx-text-fill: " + iconColor + "; -fx-padding: 15; -fx-background-radius: 50%;");

        Label title = new Label(titleStr);
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label desc = new Label(descStr);
        desc.setStyle("-fx-font-size: 15px; -fx-text-fill: #6b7280; -fx-text-alignment: center;");
        desc.setWrapText(true);
        desc.setAlignment(Pos.CENTER);

        dialog.getChildren().addAll(icon, title, desc);
        return dialog;
    }

    private Button createPremiumButton(String text, String bgStyle) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bgStyle + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 12 25; -fx-background-radius: 30px; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 4);");
        btn.setOnMouseEntered(e -> btn.setTranslateY(-2));
        btn.setOnMouseExited(e -> btn.setTranslateY(0));
        return btn;
    }

    private void showDialog(VBox dialog) {
        overlayPane.getChildren().setAll(dialog);
        overlayPane.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
        overlayPane.setVisible(true);

        // Animation d'entrée sublime
        FadeTransition ft = new FadeTransition(Duration.millis(300), dialog);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();

        TranslateTransition tt = new TranslateTransition(Duration.millis(300), dialog);
        tt.setFromY(50);
        tt.setToY(0);
        tt.play();
    }

    private void closeDialog() {
        overlayPane.setVisible(false);
    }
}