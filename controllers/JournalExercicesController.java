package controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;
import javafx.stage.StageStyle;
import models.Activite;
import models.Exercice;
import services.ServiceActivite;
import services.ServiceExercice;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class JournalExercicesController {

    @FXML private FlowPane flowPaneHistorique;

    private ServiceActivite serviceActivite = new ServiceActivite();
    private ServiceExercice serviceExercice = new ServiceExercice();
    private Map<Integer, Exercice> cacheExercices = new HashMap<>();

    // Chemins SVG
    private static final String SVG_EDIT = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    private static final String SVG_DELETE = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";

    @FXML
    public void initialize() {
        for (Exercice e : serviceExercice.afficherTout()) {
            cacheExercices.put(e.getId_exercice(), e);
        }
        chargerHistoriqueInnovant();
    }

    private void chargerHistoriqueInnovant() {
        flowPaneHistorique.getChildren().clear();
        List<Activite> activites = serviceActivite.afficherTout();

        if (activites.isEmpty()) {
            afficherMessageVide();
            return;
        }

        activites.sort((a1, a2) -> a2.getDate_activite().compareTo(a1.getDate_activite()));

        Map<LocalDate, List<Activite>> activitesParJour = new LinkedHashMap<>();
        for (Activite a : activites) {
            activitesParJour.computeIfAbsent(a.getDate_activite().toLocalDateTime().toLocalDate(), k -> new ArrayList<>()).add(a);
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale.FRENCH);

        for (Map.Entry<LocalDate, List<Activite>> entry : activitesParJour.entrySet()) {
            LocalDate dateSeance = entry.getKey();
            List<Activite> exosDuJour = entry.getValue();

            VBox sessionCard = new VBox(15);
            sessionCard.getStyleClass().add("session-card-square");
            sessionCard.setPrefSize(460, 460);
            sessionCard.setMaxSize(460, 460);

            BorderPane header = new BorderPane();
            header.getStyleClass().add("session-header");

            String dateStr = dateSeance.format(dtf);
            Label lblDate = new Label("✨ " + dateStr.substring(0, 1).toUpperCase() + dateStr.substring(1));
            lblDate.getStyleClass().add("session-date-label");

            Button btnDeleteSession = new Button("Supprimer Séance");
            btnDeleteSession.getStyleClass().add("btn-delete-session");
            btnDeleteSession.setOnAction(e -> supprimerTouteLaSeance(dateSeance, exosDuJour));

            header.setLeft(lblDate);
            header.setRight(btnDeleteSession);

            VBox exercisesList = new VBox(12);
            for (Activite a : exosDuJour) {
                exercisesList.getChildren().add(creerLigneExerciceInnovante(a));
            }

            ScrollPane scrollExercises = new ScrollPane(exercisesList);
            scrollExercises.setFitToWidth(true);
            scrollExercises.getStyleClass().add("scroll-invisible");
            VBox.setVgrow(scrollExercises, Priority.ALWAYS);

            sessionCard.getChildren().addAll(header, scrollExercises);
            flowPaneHistorique.getChildren().add(sessionCard);
        }
    }

    private HBox creerLigneExerciceInnovante(Activite a) {
        Exercice ex = cacheExercices.get(a.getId_exercice());
        String type = ex != null ? ex.getType_exercice() : "AUTRE";
        String nom = ex != null ? ex.getNom_exercice() : "Exercice inconnu";
        boolean isCardio = "CARDIO".equalsIgnoreCase(type);

        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("exercise-row-modern", isCardio ? "row-cardio" : "row-muscu");

        VBox infoBox = new VBox(6);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        // Correction de l'émoji ici aussi pour la muscu (🏋 au lieu de 🏋️)
        Label lblNom = new Label((isCardio ? "🏃 " : "🏋 ") + nom);
        lblNom.getStyleClass().add("exercise-name");

        Label lblStats = new Label(isCardio ?
                String.format("⏱ %d min   |   🔥 %d kcal", a.getDuree_minutes(), a.getCalories_brulees()) :
                String.format("🔄 %d séries x %d reps   |   ⚖ %.1f kg", a.getNb_series(), a.getNb_repetitions(), a.getPoids()));
        lblStats.getStyleClass().add("exercise-stats");

        infoBox.getChildren().addAll(lblNom, lblStats);

        if (a.getNotes() != null && !a.getNotes().trim().isEmpty()) {
            Label lblNote = new Label("« " + a.getNotes() + " »");
            lblNote.getStyleClass().add("exercise-note");
            infoBox.getChildren().add(lblNote);
        }

        StackPane btnEdit = creerBoutonIcone(SVG_EDIT, "icon-btn-edit", "icon-svg-edit");
        btnEdit.setOnMouseClicked(e -> modifierActivite(a, ex));

        StackPane btnDelete = creerBoutonIcone(SVG_DELETE, "icon-btn-delete", "icon-svg-delete");
        btnDelete.setOnMouseClicked(e -> supprimerActivite(a));

        row.getChildren().addAll(infoBox, btnEdit, btnDelete);
        return row;
    }

    private StackPane creerBoutonIcone(String svgContent, String containerClass, String svgClass) {
        SVGPath path = new SVGPath();
        path.setContent(svgContent);
        path.getStyleClass().add(svgClass);
        path.setScaleX(1.1); path.setScaleY(1.1);

        StackPane container = new StackPane(path);
        container.getStyleClass().addAll("icon-btn-modern", containerClass);
        return container;
    }

    private void afficherMessageVide() {
        VBox emptyBox = new VBox(20);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(50));
        emptyBox.setPrefWidth(900);
        Label title = new Label("Système en attente de données 🌌");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #9c27b0;");
        Label subtitle = new Label("Initiez votre première séance d'entraînement pour activer le tableau de bord.");
        subtitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #ab47bc;");
        emptyBox.getChildren().addAll(title, subtitle);
        flowPaneHistorique.getChildren().add(emptyBox);
    }

    // ========================================================================
    // 🌟 NOUVEAUX CADRES (MODALS) : DESIGN "IA" SANS BORDURES WINDOWS
    // ========================================================================

    private void modifierActivite(Activite a, Exercice ex) {
        if(ex == null) return;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialogPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #2a0845, #6441A5); " +
                "-fx-background-radius: 20; -fx-border-radius: 20; " +
                "-fx-border-color: #d500f9; -fx-border-width: 2; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(213, 0, 249, 0.5), 30, 0, 0, 10);");

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("✨ Configuration : " + ex.getNom_exercice());
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white; -fx-effect: dropshadow(one-pass-box, rgba(213, 0, 249, 0.8), 5, 0, 0, 0);");

        String labelStyle = "-fx-text-fill: #e1bee7; -fx-font-weight: bold; -fx-font-size: 14px;";
        String inputStyle = "-fx-background-color: rgba(255, 255, 255, 0.95); -fx-text-fill: #3a0a52; -fx-font-weight: bold; -fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #aa00ff; -fx-padding: 8;";

        TextField txtNotes = new TextField(a.getNotes() != null ? a.getNotes() : "");
        txtNotes.setPromptText("Vos sensations...");
        txtNotes.setStyle(inputStyle + " -fx-pref-width: 350px;");

        boolean isCardio = "CARDIO".equalsIgnoreCase(ex.getType_exercice());

        Spinner<Integer> spinDuree = new Spinner<>(1, 300, a.getDuree_minutes() > 0 ? a.getDuree_minutes() : 30);
        Spinner<Integer> spinCalories = new Spinner<>(0, 2000, a.getCalories_brulees());
        Spinner<Integer> spinSeries = new Spinner<>(1, 20, a.getNb_series() > 0 ? a.getNb_series() : 3);
        Spinner<Integer> spinReps = new Spinner<>(1, 100, a.getNb_repetitions() > 0 ? a.getNb_repetitions() : 10);
        Spinner<Double> spinPoids = new Spinner<>(0.0, 500.0, a.getPoids(), 1.0);

        spinDuree.setStyle(inputStyle); spinCalories.setStyle(inputStyle);
        spinSeries.setStyle(inputStyle); spinReps.setStyle(inputStyle); spinPoids.setStyle(inputStyle);

        if (isCardio) {
            Label l1 = new Label("Durée de l'effort (min) :"); l1.setStyle(labelStyle);
            Label l2 = new Label("Calories brûlées :"); l2.setStyle(labelStyle);
            Label l3 = new Label("Notes personnelles :"); l3.setStyle(labelStyle);
            content.getChildren().addAll(titleLabel, l1, spinDuree, l2, spinCalories, l3, txtNotes);
        } else {
            Label l1 = new Label("Nombre de Séries :"); l1.setStyle(labelStyle);
            Label l2 = new Label("Répétitions par série :"); l2.setStyle(labelStyle);
            Label l3 = new Label("Poids utilisé (kg) :"); l3.setStyle(labelStyle);
            Label l4 = new Label("Notes personnelles :"); l4.setStyle(labelStyle);
            content.getChildren().addAll(titleLabel, l1, spinSeries, l2, spinReps, l3, spinPoids, l4, txtNotes);
        }

        dialogPane.setContent(content);

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setText("Enregistrer les modifications");
        okButton.setStyle("-fx-background-color: linear-gradient(to right, #aa00ff, #d500f9); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 30; -fx-padding: 10 20; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(213, 0, 249, 0.6), 15, 0, 0, 0);");

        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Annuler");
        cancelButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #e1bee7; -fx-font-weight: bold; -fx-font-size: 14px; -fx-border-color: rgba(255, 255, 255, 0.3); -fx-border-radius: 30; -fx-padding: 10 20; -fx-cursor: hand;");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (isCardio) {
                a.setDuree_minutes(spinDuree.getValue());
                a.setCalories_brulees(spinCalories.getValue());
            } else {
                a.setNb_series(spinSeries.getValue());
                a.setNb_repetitions(spinReps.getValue());
                a.setPoids(spinPoids.getValue().floatValue());
            }
            a.setNotes(txtNotes.getText());
            serviceActivite.modifier(a);
            chargerHistoriqueInnovant();
        }
    }

    private void supprimerActivite(Activite a) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
        dialogPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #300018, #61042d); -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #ff1744; -fx-border-width: 2; -fx-effect: dropshadow(three-pass-box, rgba(255, 23, 68, 0.5), 30, 0, 0, 10);");

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER);

        Label iconLabel = new Label("⚠️");
        iconLabel.setStyle("-fx-font-size: 50px;");

        Label titleLabel = new Label("Suppression Irréversible");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label descLabel = new Label("Cette donnée sera extraite définitivement de votre base de données athlétique.");
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #ffb3c6; -fx-font-size: 15px; -fx-text-alignment: center;");
        descLabel.setPrefWidth(300);

        content.getChildren().addAll(iconLabel, titleLabel, descLabel);
        dialogPane.setContent(content);

        Button yesButton = (Button) dialogPane.lookupButton(ButtonType.YES);
        yesButton.setText("Confirmer la destruction");
        yesButton.setStyle("-fx-background-color: linear-gradient(to right, #d50000, #ff1744); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 30; -fx-padding: 10 20; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(255, 23, 68, 0.6), 15, 0, 0, 0);");

        Button noButton = (Button) dialogPane.lookupButton(ButtonType.NO);
        noButton.setText("Annuler");
        noButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffb3c6; -fx-font-weight: bold; -fx-font-size: 14px; -fx-border-color: rgba(255, 255, 255, 0.3); -fx-border-radius: 30; -fx-padding: 10 20; -fx-cursor: hand;");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            serviceActivite.supprimer(a.getId_activite());
            chargerHistoriqueInnovant();
        }
    }

    private void supprimerTouteLaSeance(LocalDate date, List<Activite> exosDuJour) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initStyle(StageStyle.UNDECORATED);

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO);
        dialogPane.setStyle("-fx-background-color: linear-gradient(to bottom right, #300018, #61042d); -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #ff1744; -fx-border-width: 2; -fx-effect: dropshadow(three-pass-box, rgba(255, 23, 68, 0.5), 30, 0, 0, 10);");

        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setAlignment(Pos.CENTER);

        Label iconLabel = new Label("🔥");
        iconLabel.setStyle("-fx-font-size: 50px;");

        Label titleLabel = new Label("Purger toute la séance ?");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label descLabel = new Label("Vous allez effacer la séance complète du " + date.format(DateTimeFormatter.ofPattern("dd MMMM")) + " (" + exosDuJour.size() + " exercices).");
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #ffb3c6; -fx-font-size: 15px; -fx-text-alignment: center;");
        descLabel.setPrefWidth(300);

        content.getChildren().addAll(iconLabel, titleLabel, descLabel);
        dialogPane.setContent(content);

        Button yesButton = (Button) dialogPane.lookupButton(ButtonType.YES);
        yesButton.setText("Oui, tout purger");
        yesButton.setStyle("-fx-background-color: linear-gradient(to right, #d50000, #ff1744); -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 30; -fx-padding: 10 20; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(255, 23, 68, 0.6), 15, 0, 0, 0);");

        Button noButton = (Button) dialogPane.lookupButton(ButtonType.NO);
        noButton.setText("Annuler");
        noButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #ffb3c6; -fx-font-weight: bold; -fx-font-size: 14px; -fx-border-color: rgba(255, 255, 255, 0.3); -fx-border-radius: 30; -fx-padding: 10 20; -fx-cursor: hand;");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            for(Activite a : exosDuJour) {
                serviceActivite.supprimer(a.getId_activite());
            }
            chargerHistoriqueInnovant();
        }
    }

    // --- NAVIGATION ---
    @FXML void ajouterCardio(MouseEvent event) { FrontLayoutController.instance.loadPage("/AjouterExerciceCardio.fxml"); }
    @FXML void ajouterMusculation(MouseEvent event) { FrontLayoutController.instance.loadPage("/AjouterExerciceMuscu.fxml"); }
    @FXML void goToAccueil(ActionEvent event) { FrontLayoutController.instance.loadPage("/AccueilActivite.fxml"); }
    @FXML void goToAliments(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml"); }
    @FXML void goToExercices(ActionEvent event) { /* Déjà dessus */ }
    @FXML void goToSommeil(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalSommeil.fxml"); }
}