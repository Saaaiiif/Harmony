package controllers.admin;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Humeur;
import models.JournalHumeur;
import models.Role;
import models.User;
import services.GroqAIService;
import services.JournalHumeurService;
import services.UserService;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminJournalController implements Initializable {

    @FXML
    private VBox studentListContainer;

    @FXML
    private VBox emptyState;

    private final UserService userService = new UserService();
    private final JournalHumeurService journalService = new JournalHumeurService();
    private final GroqAIService groqService = new GroqAIService();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadStudents();
    }

    private void loadStudents() {
        try {
            List<User> students = userService.getByRole(Role.ETUDIANT);
            studentListContainer.getChildren().clear();

            if (students.isEmpty()) {
                emptyState.setVisible(true);
                emptyState.setManaged(true);
                studentListContainer.setVisible(false);
            } else {
                emptyState.setVisible(false);
                emptyState.setManaged(false);
                studentListContainer.setVisible(true);

                for (User student : students) {
                    studentListContainer.getChildren().add(createStudentRow(student));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createStudentRow(User student) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 15, 12, 15));
        row.getStyleClass().add("card");

        Label nameLabel = new Label("👤  " + student.getFullName());
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button rapportBtn = new Button("Rapport d'étudiant");
        rapportBtn.getStyleClass().add("btn-primary");
        rapportBtn.setOnAction(e -> showRapportModal(student));

        row.getChildren().addAll(nameLabel, spacer, rapportBtn);
        return row;
    }

    private void showRapportModal(User student) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Rapport - " + student.getFullName());
        modal.setMinWidth(600);
        modal.setMinHeight(500);

        VBox root = new VBox(15);
        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color: #ffffff;");

        Label titleLabel = new Label("Rapport de bien-être : " + student.getFullName());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #7c6daf;");

        Label statusLabel = new Label("⏳ Analyse en cours par l'IA...");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7c6daf;");

        TextArea rapportArea = new TextArea();
        rapportArea.setWrapText(true);
        rapportArea.setEditable(false);
        rapportArea.setPrefRowCount(20);
        rapportArea.setStyle("-fx-font-size: 14px; -fx-border-color: #c9c5d9; -fx-border-radius: 8; -fx-background-radius: 8;");
        VBox.setVgrow(rapportArea, Priority.ALWAYS);

        Button closeBtn = new Button("Fermer");
        closeBtn.setStyle("-fx-background-color: #7c6daf; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 30; -fx-background-radius: 8; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> modal.close());

        HBox btnBox = new HBox(closeBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(titleLabel, statusLabel, rapportArea, btnBox);

        Scene scene = new Scene(root, 650, 550);
        modal.setScene(scene);
        modal.show();

        new Thread(() -> {
            try {
                List<JournalHumeur> journals = journalService.getByUserId(student.getId());

                if (journals.isEmpty()) {
                    Platform.runLater(() -> {
                        statusLabel.setText("⚠️ Aucune donnée disponible");
                        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #f39c12;");
                        rapportArea.setText("Cet étudiant n'a pas encore d'entrées dans son journal d'humeur.\n\nAucun rapport ne peut être généré pour le moment.");
                    });
                    return;
                }

                String journalSummary = journals.stream()
                        .map(j -> "Date: " + (j.getDate() != null ? j.getDate().format(dateFormatter) : "N/A")
                                + " | Humeur: " + j.getHumeur().getLabel()
                                + " | Score: " + j.getScore() + "/5")
                        .collect(Collectors.joining("\n"));

                journalSummary += "\n\nRésumé: " + journals.size() + " entrées au total.";
                double avgScore = journals.stream().mapToInt(JournalHumeur::getScore).average().orElse(0);
                Humeur avgHumeur = Humeur.fromScore((int) Math.round(avgScore));
                journalSummary += "\nScore moyen: " + String.format("%.1f", avgScore) + "/5 (" + avgHumeur.getLabel() + ")";

                String rapport = groqService.generateStudentRapport(student.getFullName(), journalSummary);

                Platform.runLater(() -> {
                    statusLabel.setText("✅ Rapport généré avec succès");
                    statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #27ae60;");
                    rapportArea.setText(rapport);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusLabel.setText("❌ Erreur lors de la génération");
                    statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #e74c3c;");
                    rapportArea.setText("Erreur: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }
}
