package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;
import models.Aliment;
import services.ServiceAliment;

public class GestionNutritionController {

    @FXML private TextField nomField, caloriesField, proteinesField, glucidesField, lipidesField;
    @FXML private TextField searchField;
    @FXML private ToggleGroup filterGroupNutri;
    @FXML private ToggleButton btnFilterAll, btnFilterPro, btnFilterGlu, btnFilterLip;
    @FXML private SVGPath svgSortIcon;
    @FXML private FlowPane cardsContainer;
    @FXML private Label messageLabel;
    @FXML private StackPane overlayPane;

    private ServiceAliment service = new ServiceAliment();
    private ObservableList<Aliment> masterData = FXCollections.observableArrayList();
    private FilteredList<Aliment> filteredData;
    private SortedList<Aliment> sortedData;

    private boolean sortTypeAscending = true;

    // Icônes
    private final String SVG_SORT_AZ = "M20 12l-1.41-1.41L13 16.17V4h-2v12.17l-5.58-5.59L4 12l8 8 8-8z";
    private final String SVG_SORT_ZA = "M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z";
    private final String SVG_EDIT = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    private final String SVG_DEL = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";

    @FXML
    public void initialize() {
        svgSortIcon.setContent(SVG_SORT_AZ);
        loadDataFromDB();
        setupFiltersAndSearch();
        setupSort();
        refreshDisplay();
    }

    private void loadDataFromDB() {
        masterData.clear();
        masterData.addAll(service.afficherTout());
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
    }

    private void setupFiltersAndSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
        filterGroupNutri.selectedToggleProperty().addListener((obs, oldVal, newVal) -> updatePredicate());
    }

    private void updatePredicate() {
        String search = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        ToggleButton selected = (ToggleButton) filterGroupNutri.getSelectedToggle();

        filteredData.setPredicate(alim -> {
            String nom = alim.getNom_aliment() != null ? alim.getNom_aliment().toLowerCase() : "";
            boolean matchSearch = search.isEmpty() || nom.startsWith(search);

            boolean matchType = true;
            if (selected == btnFilterPro) matchType = alim.getProteines() >= 10.0;
            else if (selected == btnFilterGlu) matchType = alim.getGlucides() >= 10.0;
            else if (selected == btnFilterLip) matchType = alim.getLipides() >= 10.0;

            return matchSearch && matchType;
        });
        refreshDisplay();
    }

    private void setupSort() {
        sortedData.setComparator((a1, a2) -> {
            String n1 = a1.getNom_aliment() != null ? a1.getNom_aliment().toLowerCase() : "";
            String n2 = a2.getNom_aliment() != null ? a2.getNom_aliment().toLowerCase() : "";
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

    // =========================================================================
    // ======================== CARTES ET AFFICHAGE ============================
    // =========================================================================

    private void refreshDisplay() {
        cardsContainer.getChildren().clear();
        for (Aliment al : sortedData) {
            cardsContainer.getChildren().add(createAlimentCard(al));
        }
    }

    private VBox createAlimentCard(Aliment al) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15px; -fx-padding: 20px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
        card.setPrefWidth(220);
        card.setAlignment(Pos.CENTER);

        HBox actionsBox = new HBox(10);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnEdit = createIconButton(SVG_EDIT, "#3b82f6", "#eff6ff");
        btnEdit.setOnAction(e -> ouvrirModalModification(al));

        Button btnSupprimer = createIconButton(SVG_DEL, "#ef4444", "#fef2f2");
        btnSupprimer.setOnAction(e -> demanderSuppression(al));

        actionsBox.getChildren().addAll(btnEdit, btnSupprimer);

        Label lblNom = new Label(al.getNom_aliment());
        lblNom.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4a148c;");

        Label lblCal = new Label("🔥 " + al.getCalories_pour_100g() + " kcal");
        lblCal.setStyle("-fx-font-size: 14px; -fx-text-fill: #ff5252; -fx-font-weight: bold;");

        String macrosText = String.format("🥩 P: %.1f | 🍞 G: %.1f | 🥑 L: %.1f", al.getProteines(), al.getGlucides(), al.getLipides());
        Label lblMacros = new Label(macrosText);
        lblMacros.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        card.getChildren().addAll(actionsBox, lblNom, lblCal, lblMacros);
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
    void ajouterAliment(ActionEvent event) {
        if (nomField.getText() == null || nomField.getText().trim().isEmpty() ||
                caloriesField.getText() == null || caloriesField.getText().trim().isEmpty()) {
            messageLabel.setText("❌ Le nom et les calories sont obligatoires.");
            return;
        }

        try {
            String nom = nomField.getText().trim();
            int calories = Integer.parseInt(caloriesField.getText().trim());
            double pro = proteinesField.getText().isEmpty() ? 0.0 : Double.parseDouble(proteinesField.getText().replace(",", "."));
            double glu = glucidesField.getText().isEmpty() ? 0.0 : Double.parseDouble(glucidesField.getText().replace(",", "."));
            double lip = lipidesField.getText().isEmpty() ? 0.0 : Double.parseDouble(lipidesField.getText().replace(",", "."));

            VBox dialog = createPremiumDialog("✨", "Nouvel Aliment", "Confirmez-vous l'ajout de : " + nom + " ?", "#10b981", "#d1fae5");

            Button btnConfirmer = createPremiumButton("Confirmer l'ajout", "linear-gradient(to right, #10b981, #059669)");
            btnConfirmer.setOnAction(e -> {
                // Ajout effectif dans la BDD
                service.ajouter(new Aliment(nom, calories, pro, glu, lip));

                // Vider les champs et recharger la vue complète
                nomField.clear(); caloriesField.clear(); proteinesField.clear(); glucidesField.clear(); lipidesField.clear();
                messageLabel.setText("");

                loadDataFromDB();
                updatePredicate();
                setupSort();
                refreshDisplay();
                closeDialog();
            });

            Button btnAnnuler = createPremiumButton("Annuler", "#9ca3af");
            btnAnnuler.setOnAction(e -> closeDialog());

            HBox btnBox = new HBox(15, btnAnnuler, btnConfirmer);
            btnBox.setAlignment(Pos.CENTER);
            dialog.getChildren().add(btnBox);

            showDialog(dialog);

        } catch (NumberFormatException e) {
            messageLabel.setText("❌ Entrez des nombres valides pour les macros.");
        }
    }

    private void ouvrirModalModification(Aliment al) {
        VBox dialog = createPremiumDialog("✏️", "Modification", "Mettez à jour les valeurs de l'aliment", "#3b82f6", "#dbeafe");

        TextField editNom = new TextField(al.getNom_aliment());
        editNom.setPromptText("Nom");
        styleDialogInput(editNom);

        TextField editCal = new TextField(String.valueOf(al.getCalories_pour_100g()));
        editCal.setPromptText("Calories");
        styleDialogInput(editCal);

        TextField editPro = new TextField(String.valueOf(al.getProteines()));
        editPro.setPromptText("Protéines (g)");
        styleDialogInput(editPro);

        TextField editGlu = new TextField(String.valueOf(al.getGlucides()));
        editGlu.setPromptText("Glucides (g)");
        styleDialogInput(editGlu);

        TextField editLip = new TextField(String.valueOf(al.getLipides()));
        editLip.setPromptText("Lipides (g)");
        styleDialogInput(editLip);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setAlignment(Pos.CENTER);
        grid.add(new Label("Nom:"), 0, 0); grid.add(editNom, 1, 0);
        grid.add(new Label("Calories:"), 0, 1); grid.add(editCal, 1, 1);
        grid.add(new Label("Prot (g):"), 0, 2); grid.add(editPro, 1, 2);
        grid.add(new Label("Glu (g):"), 0, 3); grid.add(editGlu, 1, 3);
        grid.add(new Label("Lip (g):"), 0, 4); grid.add(editLip, 1, 4);

        Button btnSauvegarder = createPremiumButton("Mettre à jour", "linear-gradient(to right, #3b82f6, #2563eb)");
        btnSauvegarder.setOnAction(e -> {
            try {
                al.setNom_aliment(editNom.getText());
                al.setCalories_pour_100g(Integer.parseInt(editCal.getText()));
                al.setProteines(Double.parseDouble(editPro.getText().replace(",", ".")));
                al.setGlucides(Double.parseDouble(editGlu.getText().replace(",", ".")));
                al.setLipides(Double.parseDouble(editLip.getText().replace(",", ".")));

                service.modifier(al);
                loadDataFromDB(); updatePredicate(); setupSort(); refreshDisplay();
                closeDialog();
            } catch (Exception ex) {
                // En cas d'erreur de saisie
            }
        });

        Button btnAnnuler = createPremiumButton("Annuler", "#9ca3af");
        btnAnnuler.setOnAction(e -> closeDialog());

        HBox btnBox = new HBox(15, btnAnnuler, btnSauvegarder);
        btnBox.setAlignment(Pos.CENTER);

        dialog.getChildren().addAll(grid, btnBox);
        showDialog(dialog);
    }

    private void demanderSuppression(Aliment al) {
        VBox dialog = createPremiumDialog("🗑️", "Zone de danger", "La suppression de '" + al.getNom_aliment() + "' est irréversible. Continuer ?", "#ef4444", "#fee2e2");

        Button btnSupprimer = createPremiumButton("Oui, supprimer", "linear-gradient(to right, #ef4444, #dc2626)");
        btnSupprimer.setOnAction(e -> {
            service.supprimerParId(al.getId_aliment());
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

    private void styleDialogInput(TextField tf) {
        tf.setStyle("-fx-padding: 8; -fx-background-color: #f3f4f6; -fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");
    }

    private VBox createPremiumDialog(String emoji, String titleStr, String descStr, String iconColor, String iconBg) {
        VBox dialog = new VBox(25);
        dialog.setStyle("-fx-background-color: linear-gradient(to bottom right, #ffffff, #f9fafb); -fx-background-radius: 25px; -fx-padding: 40px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 30, 0, 10, 15);");
        dialog.setMaxWidth(450);
        dialog.setAlignment(Pos.CENTER);

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