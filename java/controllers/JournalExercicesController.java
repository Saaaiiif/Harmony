package controllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
// Imports pour la vidéo
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import models.Activite;
import models.Exercice;
import services.ServiceActivite;
import services.ServiceExercice;

// Imports pour le QR Code
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import java.awt.image.BufferedImage;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class JournalExercicesController {

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

    private ServiceActivite serviceActivite = new ServiceActivite();
    private ServiceExercice serviceExercice = new ServiceExercice();
    private Map<Integer, Exercice> cacheExercices = new HashMap<>();

    private boolean isModeFemme = true;
    private String activeColor = "#e91e63";

    @FXML
    public void initialize() {
        datePickerSeance.setValue(LocalDate.now());
        for (Exercice ex : serviceExercice.afficherTout()) {
            cacheExercices.put(ex.getId_exercice(), ex);
        }
        setModeFemme();
        chargerHistoriqueInnovant();
    }

    @FXML
    void setModeFemme() {
        isModeFemme = true;
        activeColor = "#e91e63";
        bgTint.setStyle("-fx-background-color: rgba(233, 30, 99, 0.08);");
        btnFemme.setStyle("-fx-background-color: " + activeColor + "; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 10 30; -fx-font-weight: bold; -fx-cursor: hand;");
        btnHomme.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; -fx-background-radius: 25; -fx-padding: 10 30; -fx-font-weight: bold; -fx-cursor: hand;");
        chargerCategoriesSport();
    }

    @FXML
    void setModeHomme() {
        isModeFemme = false;
        activeColor = "#1e88e5";
        bgTint.setStyle("-fx-background-color: rgba(30, 136, 229, 0.08);");
        btnHomme.setStyle("-fx-background-color: " + activeColor + "; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 10 30; -fx-font-weight: bold; -fx-cursor: hand;");
        btnFemme.setStyle("-fx-background-color: transparent; -fx-text-fill: #999; -fx-background-radius: 25; -fx-padding: 10 30; -fx-font-weight: bold; -fx-cursor: hand;");
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
            btnCat.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 10 20; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

            VBox conteneur = new VBox(5);
            conteneur.setAlignment(Pos.CENTER);

            ImageView icon3D = new ImageView();
            try { icon3D.setImage(new Image(urls3D[i], true)); } catch(Exception e){}
            icon3D.setFitWidth(45); icon3D.setFitHeight(45);

            Label lbl = new Label(labels[i]);
            lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: " + activeColor + ";");

            conteneur.getChildren().addAll(icon3D, lbl);
            btnCat.setGraphic(conteneur);

            btnCat.setOnAction(e -> {
                hboxCategories.getChildren().forEach(n -> n.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 10 20; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);"));
                btnCat.setStyle("-fx-background-color: #f3e5f5; -fx-border-color: " + activeColor + "; -fx-border-width: 2; -fx-border-radius: 15; -fx-background-radius: 15; -fx-padding: 10 20; -fx-cursor: hand;");
                chargerExercices3D(catCode);
            });
            hboxCategories.getChildren().add(btnCat);
        }
        if(categories.length > 0) ((Button)hboxCategories.getChildren().get(0)).fire();
    }

    private void chargerExercices3D(String typeBase) {
        flowPaneExercices.getChildren().clear();
        String typeRecherche = typeBase + (isModeFemme ? "_Femme" : "_Homme");

        List<Exercice> exos = cacheExercices.values().stream()
                .filter(e -> e.getType_exercice().equalsIgnoreCase(typeRecherche))
                .collect(Collectors.toList());

        for (Exercice ex : exos) {
            VBox card = new VBox(8);
            card.setAlignment(Pos.CENTER);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 15; -fx-pref-width: 160; -fx-pref-height: 200; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4);");

            HBox topRow = new HBox();
            topRow.setAlignment(Pos.CENTER_RIGHT);
            Button btnEye = new Button("👁️");
            btnEye.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 50; -fx-cursor: hand; -fx-font-size: 14px;");
            btnEye.setTooltip(new Tooltip("Voir la vidéo de l'exercice"));
            btnEye.setOnAction(e -> showVideoPopup(ex));
            topRow.getChildren().add(btnEye);

            Label iconVideo = new Label("🎬");
            iconVideo.setStyle("-fx-font-size: 45px;");

            Label lblNom = new Label(ex.getNom_exercice());
            lblNom.setStyle("-fx-font-weight: bold; -fx-text-alignment: center; -fx-text-fill: #333; -fx-font-size: 13px;");
            lblNom.setWrapText(true); lblNom.setAlignment(Pos.CENTER);

            Button btnAdd = new Button("➕ Ajouter");
            btnAdd.setStyle("-fx-background-color: transparent; -fx-border-color: " + activeColor + "; -fx-border-radius: 10; -fx-text-fill: " + activeColor + "; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 15;");
            btnAdd.setOnAction(e -> showAddActivityDialog(ex));

            card.getChildren().addAll(topRow, iconVideo, lblNom, new Region(), btnAdd);
            flowPaneExercices.getChildren().add(card);
        }
    }

    // =======================================================================
    // LECTEUR VIDÉO AMÉLIORÉ : S'ADAPTE À TOUTES LES DIMENSIONS
    // =======================================================================
    private void showVideoPopup(Exercice ex) {
        Stage dialog = new Stage();
        dialog.initOwner(flowPaneExercices.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(15);
        // Design du popup
        root.setStyle("-fx-background-color: rgba(20, 20, 20, 0.95); -fx-padding: 20; -fx-background-radius: 15; -fx-border-color: " + activeColor + "; -fx-border-width: 3; -fx-border-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.7), 20, 0, 0, 0);");
        root.setAlignment(Pos.CENTER);

        Label title = new Label("Vidéo : " + ex.getNom_exercice());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 22px; -fx-text-fill: white;");

        MediaView mediaView = new MediaView();
        MediaPlayer mediaPlayer = null;

        try {
            // On récupère le chemin depuis l'objet (assurez-vous d'utiliser le bon getter)
            String path = ex.getVideo_exercice();
            if (path == null || path.trim().isEmpty()) {
                throw new Exception("Aucun fichier vidéo n'est associé à cet exercice.");
            }

            Media media;

            // Vidéo Web
            if (path.startsWith("http")) {
                media = new Media(path);
            }
            // Vidéo Locale (dans resources/videos/)
            else {
                if (!path.toLowerCase().endsWith(".mp4")) {
                    path += ".mp4";
                }

                java.net.URL videoUrl = getClass().getResource("/videos/" + path);
                if (videoUrl == null) {
                    throw new Exception("La vidéo '" + path + "' est introuvable dans le dossier resources/videos/.");
                }

                media = new Media(videoUrl.toExternalForm());
            }

            mediaPlayer = new MediaPlayer(media);
            mediaView.setMediaPlayer(mediaPlayer);

            // LA MAGIE EST ICI : On définit une boîte englobante maximale (800x500).
            // La vidéo s'adaptera automatiquement sans jamais dépasser ni être rognée.
            mediaView.setFitWidth(800);
            mediaView.setFitHeight(500);
            mediaView.setPreserveRatio(true); // Garde les proportions exactes !

            mediaPlayer.setOnError(() -> {
                System.err.println("Erreur de lecteur : " + mediaView.getMediaPlayer().getError().getMessage());
            });

            mediaPlayer.play(); // Auto-play

        } catch (Exception e) {
            System.err.println("Erreur vidéo : " + e.getMessage());
            title.setText("Vidéo Introuvable ⚠️");
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 20px; -fx-text-fill: #ef4444;");
        }

        HBox controls = new HBox(20);
        controls.setAlignment(Pos.CENTER);
        MediaPlayer finalMediaPlayer = mediaPlayer;

        Button btnPlayPause = new Button("⏸ Pause");
        btnPlayPause.setStyle("-fx-background-color: " + activeColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand; -fx-font-size: 14px;");

        Button btnReplay = new Button("🔄 Rejouer");
        btnReplay.setStyle("-fx-background-color: #0288d1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand; -fx-font-size: 14px;");

        if (finalMediaPlayer != null) {
            btnPlayPause.setOnAction(e -> {
                if (finalMediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                    finalMediaPlayer.pause();
                    btnPlayPause.setText("▶ Reprendre");
                } else {
                    finalMediaPlayer.play();
                    btnPlayPause.setText("⏸ Pause");
                }
            });
            btnReplay.setOnAction(e -> {
                finalMediaPlayer.seek(Duration.ZERO);
                finalMediaPlayer.play();
                btnPlayPause.setText("⏸ Pause");
            });
        }

        Button btnClose = new Button("❌ Fermer");
        btnClose.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand; -fx-font-size: 14px;");
        btnClose.setOnAction(e -> {
            if (finalMediaPlayer != null) finalMediaPlayer.stop();
            dialog.close();
        });

        controls.getChildren().addAll(btnPlayPause, btnReplay, btnClose);
        root.getChildren().addAll(title, mediaView, controls);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);

        FadeTransition ft = new FadeTransition(Duration.millis(300), root);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        dialog.showAndWait();
    }

    // =========================================================================
    // RESTE DU CODE
    // =========================================================================

    private void showAddActivityDialog(Exercice ex) {
        Stage dialog = new Stage();
        dialog.initOwner(flowPaneExercices.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox root = new VBox(15);
        root.setStyle("-fx-background-color: white; -fx-padding: 25; -fx-background-radius: 20; -fx-border-color: " + activeColor + "; -fx-border-width: 2; -fx-border-radius: 20;");
        root.setAlignment(Pos.CENTER);

        Label title = new Label("Ajouter : " + ex.getNom_exercice());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: " + activeColor + ";");

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(12); grid.setAlignment(Pos.CENTER);
        TextField tf1 = new TextField(); TextField tf2 = new TextField(); TextField tf3 = new TextField();
        TextArea taNote = new TextArea(); taNote.setPromptText("Notes..."); taNote.setPrefRowCount(2);

        boolean isCardio = ex.getType_exercice().contains("Cardio") || ex.getType_exercice().contains("Perte");

        if (isCardio) {
            grid.add(new Label("⏱ Durée (min):"), 0, 0); grid.add(tf1, 1, 0);
            grid.add(new Label("🔥 Calories:"), 0, 1); grid.add(tf2, 1, 1);
        } else {
            grid.add(new Label("🔢 Séries:"), 0, 0); grid.add(tf1, 1, 0);
            grid.add(new Label("🔁 Reps:"), 0, 1); grid.add(tf2, 1, 1);
            grid.add(new Label("⚖️ Poids (kg):"), 0, 2); grid.add(tf3, 1, 2);
        }

        Button btnSave = new Button("➕ Ajouter");
        btnSave.setStyle("-fx-background-color: " + activeColor + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            try {
                Activite act = new Activite();
                act.setId_exercice(ex.getId_exercice());
                act.setDate_activite(Timestamp.valueOf(datePickerSeance.getValue().atStartOfDay()));
                act.setNotes(taNote.getText());
                if (isCardio) {
                    act.setDuree_minutes(Integer.parseInt(tf1.getText())); act.setCalories_brulees(Integer.parseInt(tf2.getText()));
                } else {
                    act.setNb_series(Integer.parseInt(tf1.getText())); act.setNb_repetitions(Integer.parseInt(tf2.getText())); act.setPoids(Float.parseFloat(tf3.getText()));
                }
                serviceActivite.ajouter(act); dialog.close(); chargerHistoriqueInnovant();
            } catch (Exception excep) { System.out.println("Erreur saisie"); }
        });
        Button btnCancel = new Button("Annuler"); btnCancel.setOnAction(e -> dialog.close());
        root.getChildren().addAll(title, grid, taNote, new HBox(10, btnCancel, btnSave));

        Scene scene = new Scene(root); scene.setFill(Color.TRANSPARENT); dialog.setScene(scene); dialog.showAndWait();
    }

    private void chargerHistoriqueInnovant() {
        flowPaneHistorique.getChildren().clear();
        Map<LocalDate, List<Activite>> seancesParJour = serviceActivite.afficherTout().stream().collect(Collectors.groupingBy(a -> a.getDate_activite().toLocalDateTime().toLocalDate()));

        seancesParJour.entrySet().stream().sorted(Map.Entry.<LocalDate, List<Activite>>comparingByKey().reversed()).forEach(entry -> {
            LocalDate date = entry.getKey(); List<Activite> activites = entry.getValue();
            VBox card = new VBox(15); card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);"); card.setPrefWidth(400);

            HBox header = new HBox(10); header.setAlignment(Pos.CENTER_LEFT);
            Label lblDate = new Label("📅 " + date.toString()); lblDate.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
            Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

            Button btnQR = new Button("📱 QR");
            btnQR.setStyle("-fx-background-color: #6a1b9a; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand;");
            btnQR.setOnAction(e -> preparerEtAfficherQR(date, activites));

            Button btnDel = new Button("🗑"); btnDel.setStyle("-fx-background-color: #ffebee; -fx-text-fill: red; -fx-cursor: hand;");
            btnDel.setOnAction(e -> { for(Activite a:activites) serviceActivite.supprimer(a.getId_activite()); chargerHistoriqueInnovant(); });

            header.getChildren().addAll(lblDate, sp, btnQR, btnDel); card.getChildren().add(header);

            for (Activite act : activites) {
                Exercice ex = cacheExercices.get(act.getId_exercice()); if (ex == null) continue;
                HBox row = new HBox(10); row.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10; -fx-background-radius: 10;");
                Label icon = new Label("⚡"); icon.setStyle("-fx-font-size: 20px;");
                VBox info = new VBox(2); Label name = new Label(ex.getNom_exercice()); name.setStyle("-fx-font-weight: bold;");
                info.getChildren().addAll(name, new Label((act.getDuree_minutes()>0)? act.getDuree_minutes()+"min" : act.getNb_series()+"x"+act.getNb_repetitions()));
                Region rsp = new Region(); HBox.setHgrow(rsp, Priority.ALWAYS);
                Button bDel = new Button("❌"); bDel.setStyle("-fx-background-color: transparent; -fx-cursor: hand;"); bDel.setOnAction(e->{serviceActivite.supprimer(act.getId_activite()); chargerHistoriqueInnovant();});
                row.getChildren().addAll(icon, info, rsp, bDel); card.getChildren().add(row);
            }
            flowPaneHistorique.getChildren().add(card);
        });
    }

    private void preparerEtAfficherQR(LocalDate date, List<Activite> activites) {
        StringBuilder sb = new StringBuilder("🏋️ *Ma Séance Harmony (" + (isModeFemme ? "Femme" : "Homme") + ")*\n");
        sb.append("📅 Date : ").append(date).append("\n\n");

        for (Activite a : activites) {
            Exercice e = cacheExercices.get(a.getId_exercice());
            if (e != null) {
                sb.append("🔹 *").append(e.getNom_exercice()).append("*\n");
            }
        }
        sb.append("\n💪 _Généré avec Harmony_");

        try {
            String url = "https://wa.me/?text=" + URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8);
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, 250, 250);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            Image qrImage = SwingFXUtils.toFXImage(bufferedImage, null);

            Stage s = new Stage();
            s.initOwner(flowPaneHistorique.getScene().getWindow());
            s.initModality(Modality.APPLICATION_MODAL);
            s.initStyle(StageStyle.TRANSPARENT);

            VBox vb = new VBox(15);
            vb.setAlignment(Pos.CENTER);
            vb.setStyle("-fx-padding: 30; -fx-background-color: white; -fx-background-radius: 20; -fx-border-color: " + activeColor + "; -fx-border-width: 2; -fx-border-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 15, 0, 0, 0);");

            Label lblTitre = new Label("Partager la séance");
            lblTitre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + activeColor + ";");

            ImageView qrView = new ImageView(qrImage);

            Button btnC = new Button("Fermer");
            btnC.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #333; -fx-font-weight: bold; -fx-background-radius: 15; -fx-cursor: hand; -fx-padding: 8 20;");
            btnC.setOnAction(e -> s.close());

            vb.getChildren().addAll(lblTitre, new Label("Scannez ce QR Code pour envoyer la séance sur WhatsApp :"), qrView, btnC);

            Scene scene = new Scene(vb);
            scene.setFill(Color.TRANSPARENT);
            s.setScene(scene);
            s.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML void goToAccueil(ActionEvent event) { controllers.FrontLayoutController.instance.loadPage("/AccueilActivite.fxml"); }
    @FXML void goToAliments(ActionEvent event) { controllers.FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml"); }
    @FXML void goToSommeil(ActionEvent event) { controllers.FrontLayoutController.instance.loadPage("/JournalSommeil.fxml"); }

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
}