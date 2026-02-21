package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import models.Aliment;
import services.ServiceAliment;

import java.util.List;
import java.util.Locale;

public class GestionNutritionController {

    @FXML private TextField nomField;
    @FXML private TextField caloriesField;
    @FXML private TextField proteinesField;
    @FXML private TextField glucidesField;
    @FXML private TextField lipidesField;
    @FXML private FlowPane cardsContainer;

    @FXML private Label messageLabel;

    private ServiceAliment service = new ServiceAliment();

    private static final String SVG_EDIT = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    private static final String SVG_DELETE = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";

    @FXML
    public void initialize() {
        if(messageLabel != null) messageLabel.setText("");
        chargerDonnees();
    }

    private void chargerDonnees() {
        cardsContainer.getChildren().clear();
        List<Aliment> liste = service.afficherTout();

        for (Aliment al : liste) {
            VBox card = new VBox(15);
            card.getStyleClass().add("admin-food-card");
            card.setPrefWidth(300);
            card.setAlignment(Pos.TOP_CENTER);

            Label lblNom = new Label(al.getNom_aliment());
            lblNom.setStyle("-fx-font-weight: bold; -fx-font-size: 20px; -fx-text-fill: #3a0a52;");

            Label lblCal = new Label("🔥 " + al.getCalories_pour_100g() + " kcal / 100g");
            lblCal.getStyleClass().addAll("badge-macro", "badge-cal");

            HBox macrosBox = new HBox(8);
            macrosBox.setAlignment(Pos.CENTER);
            Label p = new Label(String.format(Locale.US, "P: %.1fg", al.getProteines())); p.getStyleClass().addAll("badge-macro", "badge-prot");
            Label g = new Label(String.format(Locale.US, "G: %.1fg", al.getGlucides())); g.getStyleClass().addAll("badge-macro", "badge-glu");
            Label l = new Label(String.format(Locale.US, "L: %.1fg", al.getLipides())); l.getStyleClass().addAll("badge-macro", "badge-lip");
            macrosBox.getChildren().addAll(p, g, l);

            HBox actionsBox = new HBox(20);
            actionsBox.setAlignment(Pos.CENTER);
            actionsBox.setPadding(new Insets(10, 0, 0, 0));

            StackPane btnEdit = creerBoutonIcone(SVG_EDIT, "btn-card-edit", "#29b6f6");
            btnEdit.setOnMouseClicked(e -> ouvrirModalModification(al));

            StackPane btnDelete = creerBoutonIcone(SVG_DELETE, "btn-card-delete", "#e53935");
            btnDelete.setOnMouseClicked(e -> ouvrirModalSuppression(al));

            actionsBox.getChildren().addAll(btnEdit, btnDelete);

            card.getChildren().addAll(lblNom, lblCal, macrosBox, actionsBox);
            cardsContainer.getChildren().add(card);
        }
    }

    private StackPane creerBoutonIcone(String svgContent, String containerClass, String colorHex) {
        SVGPath path = new SVGPath();
        path.setContent(svgContent);
        path.setStyle("-fx-fill: " + colorHex + ";");
        path.setScaleX(1.3); path.setScaleY(1.3);

        StackPane container = new StackPane(path);
        container.getStyleClass().addAll("btn-card-action", containerClass);

        container.setOnMouseEntered(e -> path.setStyle("-fx-fill: white;"));
        container.setOnMouseExited(e -> path.setStyle("-fx-fill: " + colorHex + ";"));

        return container;
    }

    @FXML
    void ajouterAliment(ActionEvent event) {
        if (nomField.getText().trim().isEmpty() || caloriesField.getText().trim().isEmpty() ||
                proteinesField.getText().trim().isEmpty() || glucidesField.getText().trim().isEmpty() ||
                lipidesField.getText().trim().isEmpty()) {
            afficherMessage("⚠️ Veuillez remplir tous les champs avant de créer l'aliment.", "#d32f2f");
            return;
        }

        try {
            String nom = nomField.getText().trim();

            // ✅ CORRECTION ICI : Accepte les virgules, les points, et arrondit les calories si c'est un nombre à virgule
            int cals = (int) Math.round(Double.parseDouble(caloriesField.getText().trim().replace(",", ".")));
            double prot = Double.parseDouble(proteinesField.getText().trim().replace(",", "."));
            double glu = Double.parseDouble(glucidesField.getText().trim().replace(",", "."));
            double lip = Double.parseDouble(lipidesField.getText().trim().replace(",", "."));

            if(cals < 0 || prot < 0 || glu < 0 || lip < 0) {
                afficherMessage("⚠️ Les valeurs nutritionnelles ne peuvent pas être négatives.", "#d32f2f");
                return;
            }

            Aliment a = new Aliment(nom, cals, prot, glu, lip);
            service.ajouter(a);

            afficherMessage("✅ L'aliment a été ajouté avec succès au catalogue !", "#00897b");
            viderChamps();
            chargerDonnees();

        } catch (NumberFormatException e) {
            afficherMessage("❌ Erreur : Saisie numérique invalide. Vérifiez vos nombres (ex: 15.5 ou 15,5).", "#d32f2f");
        }
    }

    private void afficherMessage(String message, String color) {
        if (messageLabel != null) {
            messageLabel.setText(message);
            messageLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        }
    }

    private void viderChamps() {
        nomField.clear(); caloriesField.clear(); proteinesField.clear(); glucidesField.clear(); lipidesField.clear();
    }

    private void ouvrirModalModification(Aliment al) {
        if (messageLabel != null) messageLabel.setText("");

        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox content = new VBox(25);
        content.setPadding(new Insets(30, 40, 30, 40));
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: linear-gradient(to bottom right, #2a0845, #6441A5); " +
                "-fx-background-radius: 25; -fx-border-radius: 25; -fx-border-color: #d500f9; -fx-border-width: 2; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(213, 0, 249, 0.4), 30, 0, 0, 15);");

        Label titleLabel = new Label("🛠️ Édition : " + al.getNom_aliment());
        titleLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: white;");

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        Label lNom = new Label("Nom :"); lNom.setStyle("-fx-text-fill: #e1bee7; -fx-font-weight: bold;");
        TextField tNom = new TextField(al.getNom_aliment()); tNom.setStyle("-fx-background-radius: 10; -fx-padding: 8;");

        Label lCal = new Label("Calories :"); lCal.setStyle("-fx-text-fill: #e1bee7; -fx-font-weight: bold;");
        TextField tCal = new TextField(String.valueOf(al.getCalories_pour_100g())); tCal.setStyle("-fx-background-radius: 10; -fx-padding: 8;");

        Label lProt = new Label("Protéines :"); lProt.setStyle("-fx-text-fill: #e1bee7; -fx-font-weight: bold;");
        TextField tProt = new TextField(String.valueOf(al.getProteines())); tProt.setStyle("-fx-background-radius: 10; -fx-padding: 8;");

        Label lGlu = new Label("Glucides :"); lGlu.setStyle("-fx-text-fill: #e1bee7; -fx-font-weight: bold;");
        TextField tGlu = new TextField(String.valueOf(al.getGlucides())); tGlu.setStyle("-fx-background-radius: 10; -fx-padding: 8;");

        Label lLip = new Label("Lipides :"); lLip.setStyle("-fx-text-fill: #e1bee7; -fx-font-weight: bold;");
        TextField tLip = new TextField(String.valueOf(al.getLipides())); tLip.setStyle("-fx-background-radius: 10; -fx-padding: 8;");

        grid.add(lNom, 0, 0); grid.add(tNom, 1, 0);
        grid.add(lCal, 0, 1); grid.add(tCal, 1, 1);
        grid.add(lProt, 0, 2); grid.add(tProt, 1, 2);
        grid.add(lGlu, 0, 3); grid.add(tGlu, 1, 3);
        grid.add(lLip, 0, 4); grid.add(tLip, 1, 4);

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-text-fill: #ff5252; -fx-font-weight: bold;");

        Button btnSave = new Button("💾 Sauvegarder");
        btnSave.setStyle("-fx-background-color: linear-gradient(to right, #00c6ff, #0072ff); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 10 25; -fx-cursor: hand;");

        btnSave.setOnAction(e -> {
            try {
                // ✅ CORRECTION ICI : Remplacement intelligent des virgules par des points et tolérance aux décimales
                al.setNom_aliment(tNom.getText().trim());
                al.setCalories_pour_100g((int) Math.round(Double.parseDouble(tCal.getText().trim().replace(",", "."))));
                al.setProteines(Double.parseDouble(tProt.getText().trim().replace(",", ".")));
                al.setGlucides(Double.parseDouble(tGlu.getText().trim().replace(",", ".")));
                al.setLipides(Double.parseDouble(tLip.getText().trim().replace(",", ".")));

                service.modifier(al);
                stage.close();
            } catch (NumberFormatException ex) {
                errorLabel.setText("Valeurs invalides. Veuillez vérifier vos nombres.");
            }
        });

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-border-color: rgba(255,255,255,0.5); -fx-border-radius: 20; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 25; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> stage.close());

        HBox btnBox = new HBox(20, btnCancel, btnSave);
        btnBox.setAlignment(Pos.CENTER);

        content.getChildren().addAll(titleLabel, grid, errorLabel, btnBox);

        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        stage.showAndWait();
        chargerDonnees();
    }

    private void ouvrirModalSuppression(Aliment al) {
        if (messageLabel != null) messageLabel.setText("");

        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: linear-gradient(to bottom right, #300018, #61042d); " +
                "-fx-background-radius: 20; -fx-border-color: #ff1744; -fx-border-radius: 20; -fx-border-width: 2; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(255, 23, 68, 0.5), 30, 0, 0, 10);");

        Label iconLabel = new Label("🗑️"); iconLabel.setStyle("-fx-font-size: 50px;");
        Label titleLabel = new Label("Supprimer l'aliment ?");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label descLabel = new Label("Voulez-vous vraiment retirer " + al.getNom_aliment() + " de la base ?");
        descLabel.setStyle("-fx-text-fill: #ffb3c6; -fx-font-size: 15px;");

        Button btnSupprimer = new Button("Oui, supprimer");
        btnSupprimer.setStyle("-fx-background-color: linear-gradient(to right, #d50000, #ff1744); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 30; -fx-padding: 10 20; -fx-cursor: hand;");
        btnSupprimer.setOnAction(e -> {
            service.supprimerParId(al.getId_aliment());
            stage.close();
        });

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffb3c6; -fx-font-weight: bold; -fx-font-size: 14px; -fx-border-color: rgba(255, 255, 255, 0.3); -fx-border-radius: 30; -fx-padding: 10 20; -fx-cursor: hand;");
        btnAnnuler.setOnAction(e -> stage.close());

        HBox boutonsBox = new HBox(15, btnAnnuler, btnSupprimer);
        boutonsBox.setAlignment(Pos.CENTER);

        content.getChildren().addAll(iconLabel, titleLabel, descLabel, boutonsBox);

        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        stage.showAndWait();
        chargerDonnees();
    }
}