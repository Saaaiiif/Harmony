package controllers;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;
import models.Activite;
import models.Exercice;
import services.ServiceActivite;
import services.ServiceExercice;

import java.net.URI;
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
            chatArea.setText("🤖 Coach: Bonjour ! Je suis votre coach virtuel basé sur l'IA. Posez-moi vos questions sur vos entraînements, vos douleurs ou la nutrition sportive !\n\n");
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
                    seanceCard.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");
                    seanceCard.setPrefWidth(350);

                    HBox header = new HBox(10);
                    header.setAlignment(Pos.CENTER_LEFT);
                    Label lblDate = new Label("📅 " + date.toString());
                    lblDate.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4a148c;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Button btnSupprimerSeance = new Button("🗑️ Séance");
                    btnSupprimerSeance.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #d32f2f; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-weight: bold;");
                    btnSupprimerSeance.setOnAction(e -> showDeleteSeanceDialog(date, activites));

                    header.getChildren().addAll(lblDate, spacer, btnSupprimerSeance);
                    seanceCard.getChildren().add(header);
                    seanceCard.getChildren().add(new Separator());

                    for (Activite act : activites) {
                        Exercice ex = cacheExercices.get(act.getId_exercice());
                        if (ex == null) continue;

                        HBox actRow = new HBox(10);
                        actRow.setAlignment(Pos.CENTER_LEFT);
                        actRow.setStyle("-fx-background-color: #f3e5f5; -fx-padding: 10; -fx-background-radius: 10;");

                        VBox infoBox = new VBox(3);
                        Label nomEx = new Label((ex.getType_exercice().equalsIgnoreCase("Cardio") ? "⚡ " : "🦾 ") + ex.getNom_exercice());
                        nomEx.setStyle("-fx-font-weight: bold; -fx-text-fill: #311b92;");

                        String details = ex.getType_exercice().equalsIgnoreCase("Cardio")
                                ? act.getDuree_minutes() + " min | " + act.getCalories_brulees() + " kcal"
                                : act.getNb_series() + "x" + act.getNb_repetitions() + " | " + act.getPoids() + " kg";

                        Label descEx = new Label(details);
                        descEx.setStyle("-fx-text-fill: #7e57c2; -fx-font-size: 12px;");

                        infoBox.getChildren().addAll(nomEx, descEx);

                        Region rowSpacer = new Region();
                        HBox.setHgrow(rowSpacer, Priority.ALWAYS);

                        Button btnEdit = new Button("✏️");
                        btnEdit.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                        btnEdit.setOnAction(e -> showEditDialog(act, ex));

                        Button btnDel = new Button("❌");
                        btnDel.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                        btnDel.setOnAction(e -> showDeleteActivityDialog(act));

                        actRow.getChildren().addAll(infoBox, rowSpacer, btnEdit, btnDel);
                        seanceCard.getChildren().add(actRow);
                    }
                    flowPaneHistorique.getChildren().add(seanceCard);
                });
    }

    private void showEditDialog(Activite act, Exercice ex) {
        VBox dialog = new VBox(20);
        dialog.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 20, 0, 0, 0);");
        dialog.setMaxWidth(400);
        dialog.setAlignment(Pos.CENTER);

        Label title = new Label("Modifier " + ex.getNom_exercice());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #4a148c;");

        GridPane grid = new GridPane();
        grid.setHgap(15); grid.setVgap(15);
        grid.setAlignment(Pos.CENTER);

        TextField tf1 = new TextField(); tf1.setStyle("-fx-pref-width: 100;");
        TextField tf2 = new TextField(); tf2.setStyle("-fx-pref-width: 100;");
        TextField tf3 = new TextField(); tf3.setStyle("-fx-pref-width: 100;");

        boolean isCardio = ex.getType_exercice().equalsIgnoreCase("Cardio");

        if (isCardio) {
            grid.add(new Label("Durée (min) :"), 0, 0);
            tf1.setText(String.valueOf(act.getDuree_minutes()));
            grid.add(tf1, 1, 0);

            grid.add(new Label("Calories :"), 0, 1);
            tf2.setText(String.valueOf(act.getCalories_brulees()));
            grid.add(tf2, 1, 1);
        } else {
            grid.add(new Label("Séries :"), 0, 0);
            tf1.setText(String.valueOf(act.getNb_series()));
            grid.add(tf1, 1, 0);

            grid.add(new Label("Répétitions :"), 0, 1);
            tf2.setText(String.valueOf(act.getNb_repetitions()));
            grid.add(tf2, 1, 1);

            grid.add(new Label("Poids (kg) :"), 0, 2);
            tf3.setText(String.valueOf(act.getPoids()));
            grid.add(tf3, 1, 2);
        }

        Button btnSave = new Button("Sauvegarder");
        btnSave.setStyle("-fx-background-color: #7b1fa2; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15;");
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

                // 🚀 LA CORRECTION EST ICI : On envoie la modification à la base de données !
                serviceActivite.modifier(act);

                closeDialog();
                chargerHistoriqueInnovant();
            } catch (Exception excep) {
                System.out.println("Erreur de modification : " + excep.getMessage());
            }
        });

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: black; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 15;");
        btnCancel.setOnAction(e -> closeDialog());

        HBox btns = new HBox(15, btnCancel, btnSave);
        btns.setAlignment(Pos.CENTER);

        dialog.getChildren().addAll(title, grid, btns);
        showOverlay(dialog);
    }

    private void showDeleteActivityDialog(Activite act) {
        VBox dialog = buildConfirmDialog("Supprimer l'exercice", "Voulez-vous retirer cet exercice de la séance ?", () -> {
            serviceActivite.supprimer(act.getId_activite());
            closeDialog();
            chargerHistoriqueInnovant();
        });
        showOverlay(dialog);
    }

    private void showDeleteSeanceDialog(LocalDate date, List<Activite> activites) {
        VBox dialog = buildConfirmDialog("Supprimer la séance", "Voulez-vous supprimer toute la séance du " + date + " ?", () -> {
            for (Activite a : activites) {
                serviceActivite.supprimer(a.getId_activite());
            }
            closeDialog();
            chargerHistoriqueInnovant();
        });
        showOverlay(dialog);
    }

    private VBox buildConfirmDialog(String titleText, String descText, Runnable onConfirm) {
        VBox dialog = new VBox(20);
        dialog.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 20, 0, 0, 0);");
        dialog.setMaxWidth(400);
        dialog.setAlignment(Pos.CENTER);

        Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #d32f2f;");

        Label desc = new Label(descText);
        desc.setWrapText(true);
        desc.setStyle("-fx-text-alignment: center;");

        Button btnConf = new Button("Supprimer");
        btnConf.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15;");
        btnConf.setOnAction(e -> onConfirm.run());

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: black; -fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 8 15;");
        btnCancel.setOnAction(e -> closeDialog());

        HBox btns = new HBox(15, btnCancel, btnConf);
        btns.setAlignment(Pos.CENTER);

        dialog.getChildren().addAll(title, desc, btns);
        return dialog;
    }

    private void showOverlay(VBox dialog) {
        overlayPane.getChildren().setAll(dialog);
        overlayPane.setVisible(true);

        FadeTransition ft = new FadeTransition(Duration.millis(300), dialog);
        ft.setFromValue(0.0); ft.setToValue(1.0); ft.play();
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), dialog);
        tt.setFromY(50); tt.setToY(0); tt.play();
    }

    private void closeDialog() {
        overlayPane.setVisible(false);
    }

    // --- NAVIGATION ---
    @FXML void ajouterCardio(MouseEvent event) { FrontLayoutController.instance.loadPage("/AjouterExerciceCardio.fxml"); }
    @FXML void ajouterMusculation(MouseEvent event) { FrontLayoutController.instance.loadPage("/AjouterExerciceMuscu.fxml"); }

    @FXML void goToAccueil(ActionEvent event) { FrontLayoutController.instance.loadPage("/AccueilActivite.fxml"); }
    @FXML void goToAliments(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalAlimentaire.fxml"); }
    @FXML void goToExercices(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalExercices.fxml"); }
    @FXML void goToSommeil(ActionEvent event) { FrontLayoutController.instance.loadPage("/JournalSommeil.fxml"); }

    // =========================================================================
    // === METHODES POUR LE COACH IA ===
    // =========================================================================

    @FXML
    void ouvrirCoach(ActionEvent event) {
        coachOverlayPane.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), coachOverlayPane);
        ft.setFromValue(0.0); ft.setToValue(1.0); ft.play();
    }

    @FXML
    void fermerCoach(ActionEvent event) {
        FadeTransition ft = new FadeTransition(Duration.millis(300), coachOverlayPane);
        ft.setFromValue(1.0); ft.setToValue(0.0);
        ft.setOnFinished(e -> coachOverlayPane.setVisible(false));
        ft.play();
    }

    @FXML
    void envoyerMessageCoach(ActionEvent event) {
        String question = chatInput.getText().trim();
        if (question.isEmpty()) return;

        chatArea.appendText("👤 Vous: " + question + "\n");
        chatInput.clear();
        chatArea.appendText("🤖 Coach: (En train d'analyser votre question...)\n");

        new Thread(() -> appelerApiGemini(question)).start();
    }

    private void appelerApiGemini(String question) {
        String API_KEY = "AIzaSyBtBKGk6TkcQKB5qvXV6pOg0S7GqZXkLes";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

        String safeQuestion = question.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
        String promptInstruction = "Tu es un coach sportif expert et bienveillant. Réponds de façon très concise et motivante à : " + safeQuestion;

        String jsonBody = "{\"contents\": [{\"parts\":[{\"text\": \"" + promptInstruction + "\"}]}]}";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String reponseJSON = response.body();

            String texteReponse = extraireTexteDeLaReponseGemini(reponseJSON);

            Platform.runLater(() -> {
                String currentText = chatArea.getText();
                chatArea.setText(currentText.replace("🤖 Coach: (En train d'analyser votre question...)\n", ""));
                chatArea.appendText("🤖 Coach: " + texteReponse + "\n\n");
            });

        } catch (Exception e) {
            Platform.runLater(() -> {
                String currentText = chatArea.getText();
                chatArea.setText(currentText.replace("🤖 Coach: (En train d'analyser votre question...)\n", ""));
                chatArea.appendText("⚠️ Erreur réseau : impossible de joindre le serveur.\n\n");
            });
        }
    }

    private String extraireTexteDeLaReponseGemini(String json) {
        try {
            String searchKey = "\"text\":";
            int indexDebut = json.indexOf(searchKey);

            if (indexDebut == -1) {
                if(json.contains("API_KEY_INVALID")) return "Votre clé API n'est pas valide.";
                if(json.contains("NOT_FOUND")) return "Modèle d'IA non trouvé pour cette clé.";
                return "Désolé, problème technique avec la réponse de l'IA.";
            }

            indexDebut = json.indexOf("\"", indexDebut + searchKey.length());
            if (indexDebut == -1) return "Erreur format de réponse.";
            indexDebut++;

            int indexFin = indexDebut;
            while (indexFin < json.length()) {
                if (json.charAt(indexFin) == '"' && json.charAt(indexFin - 1) != '\\') {
                    break;
                }
                indexFin++;
            }

            String texteBrut = json.substring(indexDebut, indexFin);

            return texteBrut.replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\r", "")
                    .replace("\\*", "");
        } catch (Exception e) {
            return "Erreur lors de la lecture de la réponse.";
        }
    }
}