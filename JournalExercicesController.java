package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import models.Activite;
import models.Exercice;
import services.ServiceActivite;
import services.ServiceExercice;

// --- Imports pour ZXing (QR Code) ---
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class JournalExercicesController {

    @FXML private FlowPane flowPaneHistorique;
    @FXML private StackPane overlayPane;

    private ServiceActivite serviceActivite = new ServiceActivite();
    private ServiceExercice serviceExercice = new ServiceExercice();
    private Map<Integer, Exercice> cacheExercices = new HashMap<>();

    // --- VARIABLES POUR LE COACH IA ---
    @FXML private StackPane coachOverlayPane;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInput;

    @FXML
    public void initialize() {
        for (Exercice ex : serviceExercice.afficherTout()) {
            cacheExercices.put(ex.getId_exercice(), ex);
        }
        chargerHistoriqueInnovant();

        if(chatArea != null) {
            chatArea.setText("🤖 Coach: Bonjour ! Je suis votre coach virtuel Harmony. Prêt pour votre séance ?\n\n");
        }
    }

    private void chargerHistoriqueInnovant() {
        flowPaneHistorique.getChildren().clear();
        List<Activite> toutes = serviceActivite.afficherTout();

        Map<LocalDate, List<Activite>> seancesParJour = toutes.stream()
                .collect(Collectors.groupingBy(a -> a.getDate_activite().toLocalDateTime().toLocalDate()));

        seancesParJour.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, List<Activite>>comparingByKey().reversed())
                .forEach(entry -> {
                    LocalDate date = entry.getKey();
                    List<Activite> activites = entry.getValue();

                    VBox seanceCard = new VBox(15);
                    // Design de la carte amélioré (Bords plus ronds, ombre douce)
                    seanceCard.setStyle("-fx-background-color: white; -fx-background-radius: 25; -fx-padding: 22; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 15, 0, 0, 10); -fx-border-color: #f0f0f0; -fx-border-radius: 25;");
                    seanceCard.setPrefWidth(390);

                    HBox header = new HBox(12);
                    header.setAlignment(Pos.CENTER_LEFT);

                    // Icône de date stylisée
                    Label dateIcon = new Label("📅");
                    dateIcon.setStyle("-fx-font-size: 18px; -fx-background-color: #f3e5f5; -fx-padding: 5; -fx-background-radius: 10;");

                    Label lblDate = new Label(date.toString());
                    lblDate.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: #4a148c;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    // BOUTON QR CODE (Design Innovant)
                    Button btnQR = new Button("🚀 Partager");
                    btnQR.setStyle("-fx-background-color: linear-gradient(to right, #7b1fa2, #9c27b0); -fx-text-fill: white; -fx-background-radius: 12; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 6 15;");
                    btnQR.setOnAction(e -> preparerEtAfficherQR(date, activites));

                    // Bouton supprimer (Design Épuré)
                    Button btnSupprimerSeance = new Button("🗑");
                    btnSupprimerSeance.setStyle("-fx-background-color: #fff1f0; -fx-text-fill: #ff4d4f; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 16px; -fx-min-width: 35;");
                    btnSupprimerSeance.setOnAction(e -> showDeleteSeanceDialog(date, activites));

                    header.getChildren().addAll(dateIcon, lblDate, spacer, btnQR, btnSupprimerSeance);
                    seanceCard.getChildren().add(header);
                    seanceCard.getChildren().add(new Separator());

                    for (Activite act : activites) {
                        Exercice ex = cacheExercices.get(act.getId_exercice());
                        if (ex == null) continue;

                        VBox actRowContainer = new VBox(8); // Container pour l'exercice + la note
                        actRowContainer.setStyle("-fx-background-color: #fafafa; -fx-padding: 12; -fx-background-radius: 15; -fx-border-color: #f0f0f0; -fx-border-radius: 15;");

                        HBox actMainRow = new HBox(10);
                        actMainRow.setAlignment(Pos.CENTER_LEFT);

                        // Icône d'exercice stylisée (Cercle coloré)
                        Label iconType = new Label(ex.getType_exercice().equalsIgnoreCase("Cardio") ? "🏃" : "💪");
                        iconType.setStyle("-fx-font-size: 16px; -fx-background-color: white; -fx-text-fill: #4a148c; -fx-min-width: 34; -fx-min-height: 34; -fx-alignment: center; -fx-background-radius: 17; -fx-effect: dropshadow(small, rgba(0,0,0,0.1), 3,0,0,1);");

                        VBox infoBox = new VBox(2);
                        Label nomEx = new Label(ex.getNom_exercice());
                        nomEx.setStyle("-fx-font-weight: bold; -fx-text-fill: #333; -fx-font-size: 14px;");

                        String details = ex.getType_exercice().equalsIgnoreCase("Cardio")
                                ? act.getDuree_minutes() + " min • " + act.getCalories_brulees() + " kcal"
                                : act.getNb_series() + " x " + act.getNb_repetitions() + " • " + act.getPoids() + " kg";

                        Label descEx = new Label(details);
                        descEx.setStyle("-fx-text-fill: #7b1fa2; -fx-font-size: 12px; -fx-font-weight: bold;");

                        infoBox.getChildren().addAll(nomEx, descEx);

                        Region rowSpacer = new Region();
                        HBox.setHgrow(rowSpacer, Priority.ALWAYS);

                        // Icônes d'action (Modernes et minimalistes)
                        Button btnEdit = new Button("⚙");
                        btnEdit.setStyle("-fx-background-color: transparent; -fx-text-fill: #bfbfbf; -fx-cursor: hand; -fx-font-size: 18px;");
                        btnEdit.setOnAction(e -> showEditDialog(act, ex));

                        Button btnDel = new Button("✕");
                        btnDel.setStyle("-fx-background-color: transparent; -fx-text-fill: #bfbfbf; -fx-cursor: hand; -fx-font-size: 16px;");
                        btnDel.setOnAction(e -> showDeleteActivityDialog(act));

                        actMainRow.getChildren().addAll(iconType, infoBox, rowSpacer, btnEdit, btnDel);
                        actRowContainer.getChildren().add(actMainRow);

                        // --- AJOUT DU CHAMP NOTE DANS L'AFFICHAGE ---
                        if (act.getNotes() != null && !act.getNotes().trim().isEmpty()) {
                            HBox noteBox = new HBox(6);
                            noteBox.setAlignment(Pos.CENTER_LEFT);
                            noteBox.setStyle("-fx-background-color: #fffbe6; -fx-padding: 6 10; -fx-background-radius: 8; -fx-border-color: #ffe58f; -fx-border-width: 0.5; -fx-border-radius: 8;");

                            Label noteIcon = new Label("📝");
                            noteIcon.setStyle("-fx-font-size: 11px;");

                            Label lblNote = new Label(act.getNotes());
                            lblNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #856404; -fx-font-style: italic;");
                            lblNote.setWrapText(true);
                            lblNote.setMaxWidth(300);

                            noteBox.getChildren().addAll(noteIcon, lblNote);
                            actRowContainer.getChildren().add(noteBox);
                        }

                        seanceCard.getChildren().add(actRowContainer);
                    }
                    flowPaneHistorique.getChildren().add(seanceCard);
                });
    }

    // =========================================================================
    // === LOGIQUE QR CODE CONSERVÉE ET AMÉLIORÉE AVEC NOTES ===
    // =========================================================================

    private void preparerEtAfficherQR(LocalDate date, List<Activite> activites) {
        StringBuilder textePartage = new StringBuilder();
        textePartage.append("🏆 *BILAN HARMONY* 🏆\n");
        textePartage.append("📅 Date : ").append(date.toString()).append("\n\n");

        for (Activite act : activites) {
            Exercice ex = cacheExercices.get(act.getId_exercice());
            if (ex != null) {
                textePartage.append("✅ *").append(ex.getNom_exercice()).append("* : ");
                if (ex.getType_exercice().equalsIgnoreCase("Cardio")) {
                    textePartage.append(act.getDuree_minutes()).append("min (").append(act.getCalories_brulees()).append("kcal)\n");
                } else {
                    textePartage.append(act.getNb_series()).append("x").append(act.getNb_repetitions()).append(" @ ").append(act.getPoids()).append("kg\n");
                }
                // Inclusion des notes dans le QR pour le partage
                if(act.getNotes() != null && !act.getNotes().isEmpty()) {
                    textePartage.append("   └ 📝 _").append(act.getNotes()).append("_\n");
                }
            }
        }
        textePartage.append("\n💪 _Généré via Harmony_");

        try {
            String messageEncode = URLEncoder.encode(textePartage.toString(), StandardCharsets.UTF_8.toString());
            String lienFinal = "https://wa.me/?text=" + messageEncode;

            Image imageQR = genererImageQRCode(lienFinal);
            if (imageQR != null) {
                afficherFenetreQR(imageQR, date.toString());
            }
        } catch (Exception e) {
            System.err.println("Erreur QR : " + e.getMessage());
        }
    }

    private Image genererImageQRCode(String texte) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(texte, BarcodeFormat.QR_CODE, 350, 350);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            return null;
        }
    }

    private void afficherFenetreQR(Image imageQR, String date) {
        Stage dialog = new Stage();
        dialog.initOwner(flowPaneHistorique.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 30; -fx-padding: 35; -fx-border-color: #7b1fa2; -fx-border-width: 2; -fx-border-radius: 30;");

        Label title = new Label("Votre Séance Mobile");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #4a148c;");

        ImageView qrView = new ImageView(imageQR);
        qrView.setFitWidth(240);
        qrView.setFitHeight(240);

        StackPane qrContainer = new StackPane(qrView);
        qrContainer.setStyle("-fx-background-color: #f3e5f5; -fx-padding: 15; -fx-background-radius: 20;");

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #4a148c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 30; -fx-padding: 10 30; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(title, qrContainer, new Label("Scannez pour envoyer sur WhatsApp 📲"), closeBtn);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // =========================================================================
    // === CRUD ET DIALOGUES ===
    // =========================================================================

    private void showEditDialog(Activite act, Exercice ex) {
        VBox dialog = new VBox(15);
        dialog.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 15, 0, 0, 0);");
        dialog.setMaxWidth(400);
        dialog.setAlignment(Pos.CENTER);

        Label title = new Label("Édition : " + ex.getNom_exercice());
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4a148c;");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(12);
        grid.setAlignment(Pos.CENTER);

        TextField tf1 = new TextField();
        TextField tf2 = new TextField();
        TextField tf3 = new TextField();
        TextArea taNote = new TextArea(act.getNotes()); // Champ note
        taNote.setPrefRowCount(3);
        taNote.setPromptText("Ex: Très fatigué aujourd'hui...");
        taNote.setStyle("-fx-background-radius: 10;");

        boolean isCardio = ex.getType_exercice().equalsIgnoreCase("Cardio");

        if (isCardio) {
            grid.add(new Label("⏱ Durée :"), 0, 0); tf1.setText(String.valueOf(act.getDuree_minutes())); grid.add(tf1, 1, 0);
            grid.add(new Label("🔥 Calories :"), 0, 1); tf2.setText(String.valueOf(act.getCalories_brulees())); grid.add(tf2, 1, 1);
        } else {
            grid.add(new Label("🔢 Séries :"), 0, 0); tf1.setText(String.valueOf(act.getNb_series())); grid.add(tf1, 1, 0);
            grid.add(new Label("🔁 Reps :"), 0, 1); tf2.setText(String.valueOf(act.getNb_repetitions())); grid.add(tf2, 1, 1);
            grid.add(new Label("⚖️ Poids :"), 0, 2); tf3.setText(String.valueOf(act.getPoids())); grid.add(tf3, 1, 2);
        }

        VBox noteContainer = new VBox(5, new Label("📝 Notes personnelles :"), taNote);

        Button btnSave = new Button("Enregistrer les changements");
        btnSave.setStyle("-fx-background-color: #4a148c; -fx-text-fill: white; -fx-background-radius: 12; -fx-font-weight: bold; -fx-padding: 10 20; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            try {
                if (isCardio) {
                    act.setDuree_minutes(Integer.parseInt(tf1.getText()));
                    act.setCalories_brulees(Integer.parseInt(tf2.getText()));
                } else {
                    act.setNb_series(Integer.parseInt(tf1.getText()));
                    act.setNb_repetitions(Integer.parseInt(tf2.getText()));
                    act.setPoids(Float.parseFloat(tf3.getText()));
                }
                act.setNotes(taNote.getText()); // Sauvegarde de la note
                serviceActivite.modifier(act);
                closeDialog();
                chargerHistoriqueInnovant();
            } catch (Exception excep) { System.err.println("Erreur saisie"); }
        });

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: transparent; -fx-text-fill: #999;");
        btnCancel.setOnAction(e -> closeDialog());

        HBox btns = new HBox(15, btnCancel, btnSave);
        btns.setAlignment(Pos.CENTER);
        dialog.getChildren().addAll(title, grid, noteContainer, btns);
        showOverlay(dialog);
    }

    // --- ICI SE TROUVENT LES FENETRES MODIFIÉES POUR LA SUPPRESSION ---

    private void showDeleteActivityDialog(Activite act) {
        afficherFenetreConfirmation("Suppression", "Voulez-vous retirer cet exercice de votre historique ?", () -> {
            serviceActivite.supprimer(act.getId_activite());
            chargerHistoriqueInnovant();
        });
    }

    private void showDeleteSeanceDialog(LocalDate date, List<Activite> activites) {
        afficherFenetreConfirmation("Supprimer la journée", "Effacer toute la séance du " + date + " ?", () -> {
            for (Activite a : activites) serviceActivite.supprimer(a.getId_activite());
            chargerHistoriqueInnovant();
        });
    }

    private void afficherFenetreConfirmation(String titleText, String descText, Runnable onConfirm) {
        Stage dialogStage = new Stage();
        dialogStage.initOwner(flowPaneHistorique.getScene().getWindow());
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(15);
        root.setStyle("-fx-background-color: white; -fx-padding: 25; -fx-background-radius: 20; -fx-border-color: #ff4d4f; -fx-border-width: 2; -fx-border-radius: 20;");
        root.setAlignment(Pos.CENTER);

        Label t = new Label(titleText);
        t.setStyle("-fx-font-weight: bold; -fx-text-fill: #ff4d4f; -fx-font-size: 16px;");

        Label d = new Label(descText);
        d.setStyle("-fx-text-alignment: center;");

        Button bC = new Button("Confirmer");
        bC.setStyle("-fx-background-color: #ff4d4f; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 15;");
        bC.setOnAction(e -> {
            onConfirm.run();
            dialogStage.close();
        });

        Button bA = new Button("Annuler");
        bA.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 15;");
        bA.setOnAction(e -> dialogStage.close());

        HBox hb = new HBox(15, bA, bC);
        hb.setAlignment(Pos.CENTER);

        root.getChildren().addAll(t, d, hb);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialogStage.setScene(scene);

        FadeTransition ft = new FadeTransition(Duration.millis(200), root);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        dialogStage.showAndWait();
    }

    // L'overlay a été conservé car l'édition (showEditDialog) en a encore besoin
    private void showOverlay(VBox dialog) {
        overlayPane.getChildren().setAll(dialog);
        overlayPane.setVisible(true);
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), dialog);
        tt.setFromY(30); tt.setToY(0); tt.play();
    }

    private void closeDialog() { overlayPane.setVisible(false); }

    // --- NAVIGATION CONSERVÉE ---
    @FXML void ajouterCardio(MouseEvent event) { FrontLayoutController.instance.loadPage("/AjouterExerciceCardio.fxml"); }
    @FXML void ajouterMusculation(MouseEvent event) { FrontLayoutController.instance.loadPage("/AjouterExerciceMuscu.fxml"); }
    @FXML void goToAccueil(ActionEvent event) { FrontLayoutController.instance.loadPage("/AccueilActivite.fxml"); }
    @FXML void goToAliments(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml"); }
    @FXML void goToExercices(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalExercices.fxml"); }
    @FXML void goToSommeil(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalSommeil.fxml"); }

    // =========================================================================
    // === COACH IA GEMINI CONSERVÉ ===
    // =========================================================================

    @FXML void ouvrirCoach(ActionEvent event) { coachOverlayPane.setVisible(true); }
    @FXML void fermerCoach(ActionEvent event) { coachOverlayPane.setVisible(false); }

    @FXML
    void envoyerMessageCoach(ActionEvent event) {
        String q = chatInput.getText().trim();
        if (q.isEmpty()) return;
        chatArea.appendText("👤 Vous: " + q + "\n");
        chatInput.clear();
        new Thread(() -> appelerApiGemini(q)).start();
    }

    private void appelerApiGemini(String question) {
        String API_KEY = "AIzaSyBtBKGk6TkcQKB5qvXV6pOg0S7GqZXkLes";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;
        String jsonBody = "{\"contents\": [{\"parts\":[{\"text\": \"Réponds brièvement en français : " + question + "\"}]}]}";

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
}