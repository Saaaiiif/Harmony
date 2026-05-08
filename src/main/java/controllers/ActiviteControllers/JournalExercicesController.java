package controllers.ActiviteControllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import models.ActiviteModels.Activite;
import models.ActiviteModels.Exercice;
import services.ActiviteServices.ServiceActivite;
import services.ActiviteServices.ServiceExercice;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

// NOUVEAUX IMPORTS POUR LA SAUVEGARDE (Sans utiliser java.prefs)
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JournalExercicesController {

    // ── MODIFICATION: Propriété AccueilController pour navigation ──
    private controllers.UserControlleers.modifControl.AccueilController accueilController;

    public void setAccueilController(controllers.UserControlleers.modifControl.AccueilController controller) {
        this.accueilController = controller;
    }


    @FXML private Region bgTint;
    @FXML private Button btnFemme;
    @FXML private Button btnHomme;
    @FXML private DatePicker datePickerSeance;
    @FXML private HBox hboxCategories;
    @FXML private FlowPane flowPaneExercices;
    @FXML private FlowPane flowPaneHistorique;
    @FXML private StackPane coachOverlayPane;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInput;
    // ── Bouton 3D Blessures (nouvelle fonctionnalité) ──
    @FXML private Button btnBlessures3D;

    private ServiceActivite serviceActivite = new ServiceActivite();
    private ServiceExercice serviceExercice = new ServiceExercice();
    private Map<Integer, Exercice> cacheExercices = new HashMap<>();

    private boolean isModeFemme = true;
    private String activeColor = "#e91e63";

    // Fichier de sauvegarde local pour éviter l'erreur de module
    private static final String PREF_FILE = "harmony_mode.txt";

    @FXML
    public void initialize() {
        datePickerSeance.setValue(LocalDate.now());

        // Sécurisation du chargement des exercices
        for (Exercice ex : serviceExercice.afficherTout()) {
            if (ex != null) cacheExercices.put(ex.getId_exercice(), ex);
        }

        // Restaurer l'état sauvegardé depuis le fichier
        isModeFemme = loadPreference();
        if (isModeFemme) setModeFemmeUI(); else setModeHommeUI();

        chargerHistoriqueInnovant();
        // ── Initialiser le bouton Blessures 3D ──
        if (btnBlessures3D != null) {
            btnBlessures3D.setOnAction(e -> ouvrirBlessures3D(e));
        }
    }

    // =======================================================================
    // NOUVEAU SYSTEME DE SAUVEGARDE (SANS ERREUR MODULE)
    // =======================================================================
    private void savePreference(boolean estFemme) {
        try {
            Files.writeString(Paths.get(PREF_FILE), String.valueOf(estFemme));
        } catch (Exception e) {
            System.err.println("Impossible de sauvegarder la préférence.");
        }
    }

    private boolean loadPreference() {
        try {
            Path path = Paths.get(PREF_FILE);
            if (Files.exists(path)) {
                String content = Files.readString(path).trim();
                return Boolean.parseBoolean(content);
            }
        } catch (Exception e) {
            System.err.println("Impossible de lire la préférence.");
        }
        return true; // Femme par défaut
    }

    // --- Evénements de clics ---
    @FXML void actionSetModeFemme(ActionEvent event) { setModeFemmeUI(); }
    @FXML void actionSetModeHomme(ActionEvent event) { setModeHommeUI(); }

    private void setModeFemmeUI() {
        isModeFemme = true;
        savePreference(true); // On sauvegarde
        activeColor = "#e91e63";
        bgTint.setStyle("-fx-background-color: rgba(233, 30, 99, 0.05);");
        btnFemme.setStyle("-fx-background-color: " + activeColor + "; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 12 30; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(233, 30, 99, 0.4), 10, 0, 0, 4); -fx-cursor: hand;");
        btnHomme.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; -fx-background-radius: 25; -fx-padding: 12 30; -fx-font-weight: bold; -fx-cursor: hand;");
        chargerCategoriesSport();
    }

    private void setModeHommeUI() {
        isModeFemme = false;
        savePreference(false); // On sauvegarde
        activeColor = "#1e88e5";
        bgTint.setStyle("-fx-background-color: rgba(30, 136, 229, 0.05);");
        btnHomme.setStyle("-fx-background-color: " + activeColor + "; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 12 30; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(30, 136, 229, 0.4), 10, 0, 0, 4); -fx-cursor: hand;");
        btnFemme.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; -fx-background-radius: 25; -fx-padding: 12 30; -fx-font-weight: bold; -fx-cursor: hand;");
        chargerCategoriesSport();
    }

    private void chargerCategoriesSport() {
        hboxCategories.getChildren().clear();
        String[] categories = {"Force", "Cardio", "Souplesse", "Endurance", "Perte_Poids"};
        String[] labels = {"Force", "Cardio", "Souplesse", "Endurance", "Perte de poids"};
        String[] urls3D = {
                "https://cdn-icons-png.flaticon.com/512/3043/3043888.png",
                "https://cdn-icons-png.flaticon.com/512/8306/8306906.png",
                "https://cdn-icons-png.flaticon.com/512/2964/2964514.png",
                "https://cdn-icons-png.flaticon.com/512/3043/3043884.png",
                "https://cdn-icons-png.flaticon.com/512/785/785116.png"
        };

        for (int i = 0; i < categories.length; i++) {
            String catCode = categories[i];
            Button btnCat = new Button();

            btnCat.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 20; -fx-padding: 15 25; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

            VBox conteneur = new VBox(8);
            conteneur.setAlignment(Pos.CENTER);

            ImageView icon3D = new ImageView();
            try { icon3D.setImage(new Image(urls3D[i], true)); } catch(Exception e){}
            icon3D.setFitWidth(55); icon3D.setFitHeight(55);

            Label lbl = new Label(labels[i]);
            lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #555;");

            conteneur.getChildren().addAll(icon3D, lbl);
            btnCat.setGraphic(conteneur);

            // Animation fluide
            btnCat.setOnMouseEntered(e -> { btnCat.setScaleX(1.08); btnCat.setScaleY(1.08); });
            btnCat.setOnMouseExited(e -> { btnCat.setScaleX(1.0); btnCat.setScaleY(1.0); });

            btnCat.setOnAction(e -> {
                // Reset tous les boutons au style par défaut
                hboxCategories.getChildren().forEach(n ->
                        n.setStyle("-fx-background-color: rgba(255,255,255,0.9); " +
                                "-fx-background-radius: 20; -fx-padding: 15 25; " +
                                "-fx-cursor: hand; " +
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"));
                // Style du bouton sélectionné — dropshadow avec couleur hex valide
                btnCat.setStyle("-fx-background-color: white; " +
                        "-fx-border-color: " + activeColor + "; " +
                        "-fx-border-width: 3; -fx-border-radius: 17; " +
                        "-fx-background-radius: 20; -fx-padding: 15 25; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(three-pass-box, " + activeColor + ", 15, 0.4, 0, 5);");
                // Met à jour le label en couleur active
                conteneur.getChildren().stream()
                        .filter(child -> child instanceof Label)
                        .map(child -> (Label) child)
                        .forEach(l -> l.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; " +
                                "-fx-text-fill: " + activeColor + ";"));
                chargerExercices3D(catCode);
            });
            hboxCategories.getChildren().add(btnCat);
        }
        if(!hboxCategories.getChildren().isEmpty()) ((Button)hboxCategories.getChildren().get(0)).fire();
    }

    private void chargerExercices3D(String typeBase) {
        flowPaneExercices.getChildren().clear();
        String typeRecherche = typeBase + (isModeFemme ? "_Femme" : "_Homme");

        List<Exercice> exos = cacheExercices.values().stream()
                .filter(e -> e.getType_exercice() != null && e.getType_exercice().equalsIgnoreCase(typeRecherche))
                .collect(Collectors.toList());

        for (Exercice ex : exos) {
            VBox card = new VBox(10);
            card.setAlignment(Pos.CENTER);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 20; -fx-pref-width: 180; -fx-pref-height: 220; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 15, 0, 0, 5);");

            card.setOnMouseEntered(e -> { card.setTranslateY(-5); card.setScaleX(1.02); card.setScaleY(1.02); });
            card.setOnMouseExited(e -> { card.setTranslateY(0); card.setScaleX(1.0); card.setScaleY(1.0); });

            HBox topRow = new HBox();
            topRow.setAlignment(Pos.CENTER_RIGHT);
            Button btnEye = new Button("⏯");
            btnEye.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: " + activeColor + "; -fx-background-radius: 50; -fx-cursor: hand; -fx-font-size: 18px; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);");
            btnEye.setTooltip(new Tooltip("Voir la vidéo de l'exercice"));
            btnEye.setOnAction(e -> showVideoPopup(ex));
            topRow.getChildren().add(btnEye);

            Label iconVideo = new Label("🎬");
            iconVideo.setStyle("-fx-font-size: 50px;");

            Label lblNom = new Label(ex.getNom_exercice());
            lblNom.setStyle("-fx-font-weight: bold; -fx-text-alignment: center; -fx-text-fill: #333; -fx-font-size: 15px;");
            lblNom.setWrapText(true); lblNom.setAlignment(Pos.CENTER);

            Button btnAdd = new Button("➕ Ajouter");
            btnAdd.setStyle("-fx-background-color: linear-gradient(to right, " + activeColor + ", " + activeColor + "dd); -fx-text-fill: white; -fx-background-radius: 15; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20;");
            btnAdd.setOnAction(e -> showAddOrEditDialog(ex, null));

            card.getChildren().addAll(topRow, iconVideo, lblNom, new Region(), btnAdd);
            flowPaneExercices.getChildren().add(card);
        }
    }

    private void showAddOrEditDialog(Exercice ex, Activite activiteExistante) {
        Stage dialog = new Stage();
        try {
            if (flowPaneExercices != null && flowPaneExercices.getScene() != null)
                dialog.initOwner(flowPaneExercices.getScene().getWindow());
        } catch (Exception ignored) {}
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        boolean isEditMode = (activiteExistante != null);

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: white; -fx-padding: 35; -fx-background-radius: 25; -fx-border-color: " + activeColor + "; -fx-border-width: 3; -fx-border-radius: 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 20, 0, 0, 0);");
        root.setAlignment(Pos.CENTER);

        Label title = new Label(isEditMode ? "✏️ Modifier : " + ex.getNom_exercice() : "✨ Ajouter : " + ex.getNom_exercice());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 22px; -fx-text-fill: " + activeColor + ";");

        GridPane grid = new GridPane(); grid.setHgap(15); grid.setVgap(15); grid.setAlignment(Pos.CENTER);

        TextField tf1 = new TextField(); tf1.setStyle("-fx-background-radius: 10; -fx-padding: 8; -fx-font-size: 14px;");
        TextField tf2 = new TextField(); tf2.setStyle("-fx-background-radius: 10; -fx-padding: 8; -fx-font-size: 14px;");
        TextField tf3 = new TextField(); tf3.setStyle("-fx-background-radius: 10; -fx-padding: 8; -fx-font-size: 14px;");
        TextArea taNote = new TextArea(); taNote.setPromptText("Notes personnelles..."); taNote.setPrefRowCount(3);
        taNote.setStyle("-fx-background-radius: 10; -fx-font-size: 14px; -fx-padding: 5;");

        boolean isCardio = false;
        if (ex.getType_exercice() != null) {
            isCardio = ex.getType_exercice().contains("Cardio") || ex.getType_exercice().contains("Perte");
        }

        if (isEditMode) {
            taNote.setText(activiteExistante.getNotes() != null ? activiteExistante.getNotes() : "");
            if (isCardio) {
                tf1.setText(String.valueOf(activiteExistante.getDuree_minutes()));
                tf2.setText(String.valueOf(activiteExistante.getCalories_brulees()));
            } else {
                tf1.setText(String.valueOf(activiteExistante.getNb_series()));
                tf2.setText(String.valueOf(activiteExistante.getNb_repetitions()));
                tf3.setText(String.valueOf(activiteExistante.getPoids()));
            }
        }

        if (isCardio) {
            grid.add(new Label("⏱ Durée (min):"), 0, 0); grid.add(tf1, 1, 0);
            grid.add(new Label("🔥 Calories:"), 0, 1); grid.add(tf2, 1, 1);
        } else {
            grid.add(new Label("🔢 Séries:"), 0, 0); grid.add(tf1, 1, 0);
            grid.add(new Label("🔁 Reps:"), 0, 1); grid.add(tf2, 1, 1);
            grid.add(new Label("⚖️ Poids (kg):"), 0, 2); grid.add(tf3, 1, 2);
        }

        Label lblErreur = new Label(""); lblErreur.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        Button btnSave = new Button(isEditMode ? "💾 Mettre à jour" : "➕ Valider");
        btnSave.setStyle("-fx-background-color: " + activeColor + "; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 30; -fx-cursor: hand;");

        boolean finalIsCardio = isCardio;
        btnSave.setOnAction(e -> {
            boolean hasError = false;
            tf1.setStyle("-fx-background-radius: 10; -fx-border-width: 0;");
            tf2.setStyle("-fx-background-radius: 10; -fx-border-width: 0;");
            tf3.setStyle("-fx-background-radius: 10; -fx-border-width: 0;");
            lblErreur.setText("");

            // ✅ CORRECTION CONTRÔLE DE SAISIE : normalise la virgule en point
            String val1 = tf1.getText().trim().replace(",", ".");
            String val2 = tf2.getText().trim().replace(",", ".");
            String val3 = tf3.getText().trim().replace(",", ".");

            if (finalIsCardio) {
                if (!val1.matches("\\d+")) { tf1.setStyle("-fx-border-color: red; -fx-border-radius: 10; -fx-border-width: 2;"); hasError = true; }
                if (!val2.matches("\\d+")) { tf2.setStyle("-fx-border-color: red; -fx-border-radius: 10; -fx-border-width: 2;"); hasError = true; }
            } else {
                if (!val1.matches("\\d+")) { tf1.setStyle("-fx-border-color: red; -fx-border-radius: 10; -fx-border-width: 2;"); hasError = true; }
                if (!val2.matches("\\d+")) { tf2.setStyle("-fx-border-color: red; -fx-border-radius: 10; -fx-border-width: 2;"); hasError = true; }
                // ✅ Accepte entier ou décimal (ex: 75 ou 75.5 ou 75,5)
                if (!val3.matches("\\d+(\\.\\d+)?")) { tf3.setStyle("-fx-border-color: red; -fx-border-radius: 10; -fx-border-width: 2;"); hasError = true; }
            }

            if (hasError) {
                lblErreur.setText("⚠️ Veuillez entrer des valeurs numériques valides (ex: 10 ou 75,5).");
                return;
            }

            Activite act = isEditMode ? activiteExistante : new Activite();
            act.setId_exercice(ex.getId_exercice());
            act.setNotes(taNote.getText());

            if (!isEditMode) {
                // ✅ CORRECTION : injecter user_id depuis la Session
                try {
                    if (models.UserModels.Session.getInstance() != null
                            && models.UserModels.Session.getInstance().getUser() != null) {
                        act.setUser_id(models.UserModels.Session.getInstance().getUser().getUser_id());
                    }
                } catch (Exception ignored) {}

                LocalDate date = datePickerSeance.getValue() != null ? datePickerSeance.getValue() : LocalDate.now();
                act.setDate_activite(Timestamp.valueOf(date.atStartOfDay()));
            }

            if (finalIsCardio) {
                act.setDuree_minutes(Integer.parseInt(val1));
                act.setCalories_brulees(Integer.parseInt(val2));
                act.setNb_series(0); act.setNb_repetitions(0); act.setPoids(0f);
            } else {
                act.setNb_series(Integer.parseInt(val1));
                act.setNb_repetitions(Integer.parseInt(val2));
                act.setPoids(Float.parseFloat(val3));
                act.setDuree_minutes(0); act.setCalories_brulees(0);
            }

            if (isEditMode) serviceActivite.modifier(act);
            else serviceActivite.ajouter(act);

            dialog.close();
            chargerHistoriqueInnovant();
        });

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: #f1f2f6; -fx-text-fill: #555; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 10 30; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        root.getChildren().addAll(title, grid, taNote, lblErreur, new HBox(20, btnCancel, btnSave));

        Scene scene = new Scene(root); scene.setFill(Color.TRANSPARENT); dialog.setScene(scene);

        FadeTransition ft = new FadeTransition(Duration.millis(300), root);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        dialog.showAndWait();
    }

    private void chargerHistoriqueInnovant() {
        flowPaneHistorique.getChildren().clear();

        Map<LocalDate, List<Activite>> seancesParJour = serviceActivite.afficherTout().stream()
                .filter(a -> a.getDate_activite() != null)
                .collect(Collectors.groupingBy(a -> a.getDate_activite().toLocalDateTime().toLocalDate()));

        seancesParJour.entrySet().stream().sorted(Map.Entry.<LocalDate, List<Activite>>comparingByKey().reversed()).forEach(entry -> {
            LocalDate date = entry.getKey();
            List<Activite> activites = entry.getValue();

            VBox card = new VBox(15);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 25; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 15, 0, 0, 5); -fx-border-color: " + activeColor + "33; -fx-border-width: 2; -fx-border-radius: 20;");
            card.setPrefWidth(450);

            HBox header = new HBox(15); header.setAlignment(Pos.CENTER_LEFT);
            Label lblDate = new Label("📅 " + date.toString());
            lblDate.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

            Button btnQR = new Button("📱 QR");
            btnQR.setStyle("-fx-background-color: #4a148c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 15; -fx-cursor: hand;");
            btnQR.setOnAction(e -> preparerEtAfficherQR(date, activites));

            Button btnDelSession = new Button("🗑️");
            btnDelSession.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #d32f2f; -fx-font-size: 14px; -fx-background-radius: 50; -fx-cursor: hand;");
            btnDelSession.setTooltip(new Tooltip("Supprimer toute la séance"));
            btnDelSession.setOnAction(e -> showDeleteConfirmation("cette séance complète", () -> {
                for(Activite a : activites) serviceActivite.supprimer(a.getId_activite());
                chargerHistoriqueInnovant();
            }));

            header.getChildren().addAll(lblDate, sp, btnQR, btnDelSession);
            card.getChildren().add(header);
            card.getChildren().add(new Separator());

            for (Activite act : activites) {
                Exercice ex = cacheExercices.get(act.getId_exercice()); if (ex == null) continue;

                VBox rowContainer = new VBox(5);
                rowContainer.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 15; -fx-border-color: #eee; -fx-border-radius: 15;");

                HBox row = new HBox(15); row.setAlignment(Pos.CENTER_LEFT);
                Label icon = new Label("⚡"); icon.setStyle("-fx-font-size: 24px;");

                VBox info = new VBox(2);
                Label name = new Label(ex.getNom_exercice()); name.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #222;");
                String detail = (act.getDuree_minutes() > 0) ? "⏱ " + act.getDuree_minutes() + " min | 🔥 " + act.getCalories_brulees() + " kcal" : "🔢 " + act.getNb_series() + "x" + act.getNb_repetitions() + " | ⚖️ " + act.getPoids() + " kg";
                Label lblDetail = new Label(detail); lblDetail.setStyle("-fx-text-fill: #666; -fx-font-size: 13px;");
                info.getChildren().addAll(name, lblDetail);

                Region rsp = new Region(); HBox.setHgrow(rsp, Priority.ALWAYS);

                Button bEdit = new Button("✏️");
                bEdit.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1e88e5; -fx-background-radius: 50; -fx-cursor: hand;");
                bEdit.setOnAction(e -> showAddOrEditDialog(ex, act));

                Button bDel = new Button("❌");
                bDel.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #e53935; -fx-background-radius: 50; -fx-cursor: hand;");
                bDel.setOnAction(e -> showDeleteConfirmation("cet exercice", () -> {
                    serviceActivite.supprimer(act.getId_activite()); chargerHistoriqueInnovant();
                }));

                row.getChildren().addAll(icon, info, rsp, bEdit, bDel);
                rowContainer.getChildren().add(row);

                if (act.getNotes() != null && !act.getNotes().trim().isEmpty()) {
                    Label lblNote = new Label("📝 Note: " + act.getNotes());
                    lblNote.setStyle("-fx-text-fill: black; -fx-font-style: italic; -fx-font-size: 13px; -fx-padding: 5 0 0 10;");
                    lblNote.setWrapText(true);
                    rowContainer.getChildren().add(lblNote);
                }

                card.getChildren().add(rowContainer);
            }
            flowPaneHistorique.getChildren().add(card);
        });
    }

    private void showDeleteConfirmation(String element, Runnable onConfirm) {
        Stage dialog = new Stage();
        try {
            if (flowPaneHistorique != null && flowPaneHistorique.getScene() != null)
                dialog.initOwner(flowPaneHistorique.getScene().getWindow());
        } catch (Exception ignored) {}
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 20; -fx-border-color: #ef4444; -fx-border-width: 3; -fx-border-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(239, 68, 68, 0.4), 20, 0, 0, 0);");
        root.setAlignment(Pos.CENTER);

        Label icon = new Label("⚠️"); icon.setStyle("-fx-font-size: 40px;");
        Label title = new Label("Confirmation"); title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
        Label desc = new Label("Êtes-vous sûr de vouloir supprimer " + element + " ?\nCette action est irréversible.");
        desc.setStyle("-fx-text-alignment: center; -fx-font-size: 14px; -fx-text-fill: #333;");
        desc.setWrapText(true);

        Button btnConf = new Button("Oui, supprimer");
        btnConf.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 15; -fx-padding: 10 20; -fx-cursor: hand;");
        btnConf.setOnAction(e -> { onConfirm.run(); dialog.close(); });

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.setStyle("-fx-background-color: #f1f2f6; -fx-text-fill: #555; -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 15; -fx-padding: 10 20; -fx-cursor: hand;");
        btnAnnuler.setOnAction(e -> dialog.close());

        root.getChildren().addAll(icon, title, desc, new HBox(15, btnAnnuler, btnConf));
        Scene scene = new Scene(root); scene.setFill(Color.TRANSPARENT); dialog.setScene(scene);

        FadeTransition ft = new FadeTransition(Duration.millis(200), root);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        dialog.showAndWait();
    }

    private void showVideoPopup(Exercice ex) {
        Stage dialog = new Stage();
        try {
            if (flowPaneExercices != null && flowPaneExercices.getScene() != null)
                dialog.initOwner(flowPaneExercices.getScene().getWindow());
        } catch (Exception ignored) {}
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(15);
        root.setStyle("-fx-background-color: rgba(20, 20, 20, 0.95); -fx-padding: 20; -fx-background-radius: 15; -fx-border-color: " + activeColor + "; -fx-border-width: 3; -fx-border-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.7), 20, 0, 0, 0);");
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
                // ✅ CORRECTION CHEMIN VIDÉO : dans le projet d'intégration,
                // les vidéos sont sous /views/ActiviteViews/videos/ et NON sous /videos/
                java.net.URL videoUrl = getClass().getResource("/views/ActiviteViews/videos/" + path);
                // Fallback : tente aussi la racine /videos/ (compatibilité)
                if (videoUrl == null) videoUrl = getClass().getResource("/videos/" + path);
                if (videoUrl == null) throw new Exception("Introuvable : " + path);
                media = new Media(videoUrl.toExternalForm());
            }
            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);
            mediaView.setFitWidth(800); mediaView.setFitHeight(500); mediaView.setPreserveRatio(true);
            mediaPlayer.play();
        } catch (Exception e) {
            title.setText("Vidéo Introuvable ⚠️"); title.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 22px; -fx-font-weight: bold;");
            System.err.println("Erreur vidéo : " + e.getMessage());
        }

        HBox controls = new HBox(20); controls.setAlignment(Pos.CENTER);
        MediaPlayer finalMediaPlayer = mediaPlayer;

        Button btnPlayPause = new Button("⏸ Pause");
        btnPlayPause.setStyle("-fx-background-color: " + activeColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand;");
        Button btnReplay = new Button("🔄 Rejouer");
        btnReplay.setStyle("-fx-background-color: #0288d1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand;");

        if (finalMediaPlayer != null) {
            btnPlayPause.setOnAction(e -> {
                if (finalMediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) { finalMediaPlayer.pause(); btnPlayPause.setText("▶ Reprendre"); }
                else { finalMediaPlayer.play(); btnPlayPause.setText("⏸ Pause"); }
            });
            btnReplay.setOnAction(e -> { finalMediaPlayer.seek(Duration.ZERO); finalMediaPlayer.play(); btnPlayPause.setText("⏸ Pause"); });
        }

        Button btnClose = new Button("❌ Fermer");
        btnClose.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand;");
        btnClose.setOnAction(e -> { if (finalMediaPlayer != null) finalMediaPlayer.stop(); dialog.close(); });

        controls.getChildren().addAll(btnPlayPause, btnReplay, btnClose);
        root.getChildren().addAll(title, mediaView, controls);

        Scene scene = new Scene(root); scene.setFill(Color.TRANSPARENT); dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void preparerEtAfficherQR(LocalDate date, List<Activite> activites) {
        // Construit le message texte de la séance
        StringBuilder sb = new StringBuilder("🏋️ Séance Harmony du : " + date.toString() + "\n\n");
        for (Activite act : activites) {
            Exercice ex = cacheExercices.get(act.getId_exercice());
            if (ex != null) {
                sb.append("✅ ").append(ex.getNom_exercice()).append("\n");
                if (act.getDuree_minutes() > 0) {
                    sb.append("   ⏱ ").append(act.getDuree_minutes()).append(" min | 🔥 ").append(act.getCalories_brulees()).append(" kcal\n");
                } else {
                    sb.append("   🔢 ").append(act.getNb_series()).append("x").append(act.getNb_repetitions()).append(" | ⚖️ ").append(act.getPoids()).append(" kg\n");
                }
            }
        }
        sb.append("\n💪 Partagé via Harmony App !");

        try {
            // ── CORRECTION PROBLÈME 3 : Encode l'URL WhatsApp dans le QR code ──
            // Quand l'étudiant scanne ce QR, son téléphone ouvre WhatsApp directement
            // avec le message pré-rempli pour partager à un contact.
            String messageText = sb.toString();
            String whatsappUrl = "https://wa.me/?text=" +
                    java.net.URLEncoder.encode(messageText, java.nio.charset.StandardCharsets.UTF_8);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(
                    whatsappUrl, BarcodeFormat.QR_CODE, 300, 300);
            java.awt.image.BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            javafx.scene.image.Image image = javafx.embed.swing.SwingFXUtils.toFXImage(bufferedImage, null);

            Stage dialog = new Stage();
            try {
                if (flowPaneHistorique != null && flowPaneHistorique.getScene() != null)
                    dialog.initOwner(flowPaneHistorique.getScene().getWindow());
            } catch (Exception ignored) {}
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.TRANSPARENT);

            VBox vbox = new VBox(18);
            vbox.setAlignment(Pos.CENTER);
            vbox.setStyle("-fx-padding: 30; -fx-background-color: white; -fx-background-radius: 20; " +
                    "-fx-border-color: " + activeColor + "; -fx-border-width: 3; " +
                    "-fx-border-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 20, 0, 0, 0);");

            Label title = new Label("📱 Partager sur WhatsApp");
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 20px; -fx-text-fill: " + activeColor + ";");

            Label instruction = new Label("Scannez ce QR code avec votre téléphone :\nWhatsApp s'ouvre directement avec votre séance prête à envoyer !");
            instruction.setStyle("-fx-text-fill: #555; -fx-font-size: 13px; -fx-text-alignment: center; -fx-alignment: center;");
            instruction.setWrapText(true);
            instruction.setAlignment(Pos.CENTER);

            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(270); imageView.setFitHeight(270);

            // ── Bouton pour ouvrir WhatsApp directement depuis l'appli (Desktop) ──
            Button btnWhatsApp = new Button("💬 Ouvrir WhatsApp sur cet appareil");
            btnWhatsApp.setStyle("-fx-background-color: #25d366; -fx-text-fill: white; " +
                    "-fx-font-weight: bold; -fx-font-size: 14px; " +
                    "-fx-background-radius: 15; -fx-padding: 10 20; -fx-cursor: hand; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(37,211,102,0.4), 8, 0, 0, 3);");
            btnWhatsApp.setOnAction(ev -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(whatsappUrl));
                } catch (Exception ex) {
                    System.err.println("Impossible d'ouvrir WhatsApp : " + ex.getMessage());
                }
            });

            Button btnClose = new Button("Fermer");
            btnClose.setStyle("-fx-background-color: #f1f2f6; -fx-text-fill: #333; " +
                    "-fx-font-weight: bold; -fx-background-radius: 15; " +
                    "-fx-padding: 10 30; -fx-cursor: hand;");
            btnClose.setOnAction(e -> dialog.close());

            HBox btnBox = new HBox(15, btnWhatsApp, btnClose);
            btnBox.setAlignment(Pos.CENTER);

            vbox.getChildren().addAll(title, imageView, instruction, btnBox);
            Scene scene = new Scene(vbox);
            scene.setFill(Color.TRANSPARENT);
            dialog.setScene(scene);

            FadeTransition ft = new FadeTransition(Duration.millis(300), vbox);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
            dialog.showAndWait();

        } catch (Exception e) {
            System.err.println("Erreur de génération QR Code: " + e.getMessage());
        }
    }

    @FXML void goToAccueil(ActionEvent event) { if (accueilController != null) accueilController.loadActivityPage("/views/ActiviteViews/AccueilActivite.fxml"); }
    @FXML void goToAliments(ActionEvent event) { if (accueilController != null) accueilController.loadActivityPage("/views/ActiviteViews/JournalAlimentaire.fxml"); }
    @FXML void goToSommeil(ActionEvent event) { if (accueilController != null) accueilController.loadActivityPage("/views/ActiviteViews/JournalSommeil.fxml"); }

    @FXML void ouvrirCoach(ActionEvent event) { coachOverlayPane.setVisible(true); }
    @FXML void fermerCoach(ActionEvent event) { coachOverlayPane.setVisible(false); }

    @FXML void envoyerMessageCoach(ActionEvent event) {
        String q = chatInput.getText().trim();
        if (q.isEmpty()) return;
        chatArea.appendText("👤 Vous: " + q + "\n");
        chatInput.clear();
        new Thread(() -> appelerApiGemini(q)).start();
    }

    private void appelerApiGemini(String question) {
        String API_KEY = "AIzaSyBtBKGk6TkcQKB5qvXV6pOg0S7GqZXkLes";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;
        String context = isModeFemme ? "Tu es une coach experte en fitness féminin." : "Tu es un coach expert en musculation masculine.";
        String jsonBody = "{\"contents\": [{\"parts\":[{\"text\": \"" + context + " Réponds brièvement en français à la question suivante : " + question + "\"}]}]}";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8)).build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String res = extraireTexteDeLaReponseGemini(response.body());
            Platform.runLater(() -> chatArea.appendText("🤖 Coach: " + res + "\n\n"));
        } catch (Exception e) {
            Platform.runLater(() -> chatArea.appendText("⚠️ Erreur Coach IA.\n"));
        }
    }

    private String extraireTexteDeLaReponseGemini(String json) {
        try {
            int start = json.indexOf("\"text\": \"") + 9;
            int end = json.indexOf("\"", start);
            return json.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
        } catch (Exception e) { return "Erreur d'analyse."; }
    }

    // ═══════════════════════════════════════════════════════════
    //  NOUVELLE FONCTIONNALITÉ : Visualisation 3D des Blessures
    // ═══════════════════════════════════════════════════════════
    @FXML
    void ouvrirBlessures3D(ActionEvent event) {
        BlessureViewerController.openBlessureViewer(
                flowPaneExercices.getScene().getWindow()
        );
    }
}