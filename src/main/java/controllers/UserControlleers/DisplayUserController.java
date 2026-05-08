package controllers.UserControlleers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import models.UserModels.user;

import java.io.File;
import java.time.LocalDate;
import java.time.Period;

public class DisplayUserController {

    @FXML private StackPane avatarContainer;
    @FXML private Label avatarLabel;
    @FXML private ImageView profileImageView;
    @FXML private Label fullNameLabel, emailLabel, ageLabel, dateNaissanceLabel, dateInscriptionLabel, roleLabel;
    @FXML private Label sexeLabel, poidsLabel, tailleLabel, niveauActiviteLabel;
    @FXML private Label niveauScolaireLabel, etablissementLabel;

    public void setUser(user u) {
        // En-tête
        fullNameLabel.setText(u.getUser_prenom() + " " + u.getUser_nom());
        emailLabel.setText(u.getUser_email());

        // Avatar : image ou initiale
        String imagePath = u.getUser_image_path();
        if (imagePath != null && !imagePath.isEmpty()) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                try {
                    Image img = new Image(imgFile.toURI().toString(), 70, 70, true, true);
                    profileImageView.setImage(img);
                    profileImageView.setFitWidth(70);
                    profileImageView.setFitHeight(70);
                    profileImageView.setPreserveRatio(false);
                    // Clip circulaire
                    Circle clip = new Circle(35, 35, 35);
                    profileImageView.setClip(clip);
                    profileImageView.setVisible(true);
                    avatarLabel.setVisible(false);
                } catch (Exception e) {
                    showInitial(u);
                }
            } else {
                showInitial(u);
            }
        } else {
            showInitial(u);
        }

        // Informations générales (Chips)
        int age = calculateAge(u.getUser_date_de_naissance());
        styleChip(ageLabel, age + " ans", "#F3E8FF", "#6B21A8", "🎂");
        styleChip(dateNaissanceLabel, u.getUser_date_de_naissance(), "#F3F4F6", "#374151", "📅");
        styleChip(roleLabel, u.getType_utilisateur().name(), "#FEF3C7", "#92400E", "⭐");
        styleChip(dateInscriptionLabel, u.getDate_inscription(), "#F3F4F6", "#374151", "🕒");

        // Santé
        String sexeStr = u.getUser_sexe() != null ? u.getUser_sexe().getDisplayName() : "N/A";
        styleChip(sexeLabel, sexeStr, "#E0F2FE", "#0369A1", "🚻");
        String poidsStr = u.getUser_poids() != null ? u.getUser_poids() + " kg" : "N/A";
        styleChip(poidsLabel, poidsStr, "#D1FAE5", "#065F46", "⚖️");
        String tailleStr = u.getUser_taille() != null ? u.getUser_taille() + " cm" : "N/A";
        styleChip(tailleLabel, tailleStr, "#D1FAE5", "#065F46", "📏");
        String activiteStr = u.getUser_niveau_activite_physique() != null ? u.getUser_niveau_activite_physique().getDisplayName() : "N/A";
        styleChip(niveauActiviteLabel, activiteStr, "#FFEDD5", "#C2410C", "🏃");

        // Scolaire
        String niveauStr = u.getUser_niveau_scolaire() != null ? u.getUser_niveau_scolaire().getDisplayName() : "N/A";
        styleChip(niveauScolaireLabel, niveauStr, "#FCE7F3", "#9D174D", "📚");
        String etablissementStr = u.getUser_etablissement_scolaire() != null ? u.getUser_etablissement_scolaire() : "N/A";
        styleChip(etablissementLabel, etablissementStr, "#F3E8FF", "#6B21A8", "🏫");
    }

    private void showInitial(user u) {
        String initial = (u.getUser_prenom() != null && !u.getUser_prenom().isEmpty())
                ? u.getUser_prenom().substring(0, 1).toUpperCase() : "U";
        avatarLabel.setText(initial);
        avatarLabel.setVisible(true);
        profileImageView.setVisible(false);
    }

    private void styleChip(Label label, String text, String bgColor, String textColor, String icon) {
        label.setText(icon + " " + text);
        label.setStyle(
                "-fx-background-color: " + bgColor + "; " +
                        "-fx-text-fill: " + textColor + "; " +
                        "-fx-padding: 8 16 8 16; " +
                        "-fx-background-radius: 20; " +
                        "-fx-font-size: 13; " +
                        "-fx-font-weight: bold;"
        );
    }

    private int calculateAge(String birthDateStr) {
        try {
            LocalDate birthDate = LocalDate.parse(birthDateStr);
            return Period.between(birthDate, LocalDate.now()).getYears();
        } catch (Exception e) { return 0; }
    }
}
