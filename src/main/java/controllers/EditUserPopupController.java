package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.*;
import services.serviceUser;
import utils.ValidationUtils;

import java.io.File;
import java.time.LocalDate;

public class EditUserPopupController {

    @FXML private TextField nomField, prenomField, emailField, poidsField, tailleField, etablissementField;
    @FXML private DatePicker dateNaissanceField;
    @FXML private Label roleLabel;
    @FXML private Label dateInscriptionLabel;
    @FXML private ComboBox<Sexe> sexeCombo;
    @FXML private ComboBox<NiveauActivitePhysique> niveauActiviteCombo;
    @FXML private ComboBox<NiveauScolaire> niveauScolaireCombo;

    @FXML private Label errorNom, errorPrenom, errorEmail, errorDate, errorPoids, errorTaille, errorEtablissement;

    // Image de profil
    @FXML private StackPane avatarStackPane;
    @FXML private Label avatarInitialLabel;
    @FXML private ImageView profileImageView;
    @FXML private Label imageStatusLabel;

    private user currentUser;
    private final serviceUser service = new serviceUser();

    // Gestion de l'image : null = pas de changement, "" = supprimer, autrement = nouveau chemin source
    private String newImageSourcePath = null;
    private boolean removeImage = false;

    @FXML
    public void initialize() {
        hideAllErrors();
        populateComboBoxes();
        setupRealtimeValidation();
    }

    private void populateComboBoxes() {
        sexeCombo.setItems(FXCollections.observableArrayList(Sexe.values()));
        niveauActiviteCombo.setItems(FXCollections.observableArrayList(NiveauActivitePhysique.values()));
        niveauScolaireCombo.setItems(FXCollections.observableArrayList(NiveauScolaire.values()));
    }

    private void hideAllErrors() {
        errorNom.setVisible(false);
        errorPrenom.setVisible(false);
        errorEmail.setVisible(false);
        errorDate.setVisible(false);
        errorPoids.setVisible(false);
        errorTaille.setVisible(false);
        errorEtablissement.setVisible(false);
    }

    private void setupRealtimeValidation() {
        nomField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validateNom(); });
        prenomField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validatePrenom(); });
        emailField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validateEmail(); });
        dateNaissanceField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validateDate(); });
        poidsField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validatePoids(); });
        tailleField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validateTaille(); });
        etablissementField.focusedProperty().addListener((obs, oldVal, newVal) -> { if (!newVal) validateEtablissement(); });
    }

    public void setUser(user u) {
        this.currentUser = u;
        nomField.setText(u.getUser_nom());
        prenomField.setText(u.getUser_prenom());
        emailField.setText(u.getUser_email());
        dateNaissanceField.setValue(LocalDate.parse(u.getUser_date_de_naissance()));

        if (u.getUser_sexe() != null) sexeCombo.setValue(u.getUser_sexe());
        if (u.getUser_poids() != null) poidsField.setText(u.getUser_poids().toString());
        if (u.getUser_taille() != null) tailleField.setText(u.getUser_taille().toString());
        if (u.getUser_niveau_activite_physique() != null) niveauActiviteCombo.setValue(u.getUser_niveau_activite_physique());
        if (u.getUser_niveau_scolaire() != null) niveauScolaireCombo.setValue(u.getUser_niveau_scolaire());
        etablissementField.setText(u.getUser_etablissement_scolaire());

        roleLabel.setText("⭐ Rôle : " + u.getType_utilisateur().name());
        dateInscriptionLabel.setText("🕒 Inscrit le : " + u.getDate_inscription());

        // Afficher l'image existante ou l'initiale
        loadExistingImage(u);
    }

    private void loadExistingImage(user u) {
        String imagePath = u.getUser_image_path();
        if (imagePath != null && !imagePath.isEmpty()) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                try {
                    Image img = new Image(imgFile.toURI().toString(), 90, 90, true, true);
                    profileImageView.setImage(img);
                    profileImageView.setFitWidth(90);
                    profileImageView.setFitHeight(90);
                    profileImageView.setPreserveRatio(false);
                    Circle clip = new Circle(45, 45, 45);
                    profileImageView.setClip(clip);
                    profileImageView.setVisible(true);
                    avatarInitialLabel.setVisible(false);
                    imageStatusLabel.setText("✅ Image actuelle");
                    imageStatusLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11; -fx-font-weight: bold;");
                    return;
                } catch (Exception e) {}
            }
        }
        // Fallback initiale
        showInitial();
        imageStatusLabel.setText("Aucune image");
        imageStatusLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11;");
    }

    private void showInitial() {
        String initial = (currentUser != null && currentUser.getUser_prenom() != null && !currentUser.getUser_prenom().isEmpty())
                ? currentUser.getUser_prenom().substring(0, 1).toUpperCase() : "U";
        avatarInitialLabel.setText(initial);
        avatarInitialLabel.setVisible(true);
        profileImageView.setVisible(false);
    }

    // ====================== GESTION IMAGE ======================

    @FXML
    void handleChooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        Stage stage = (Stage) nomField.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            newImageSourcePath = selectedFile.getAbsolutePath();
            removeImage = false;
            try {
                Image img = new Image(selectedFile.toURI().toString(), 90, 90, true, true);
                profileImageView.setImage(img);
                profileImageView.setFitWidth(90);
                profileImageView.setFitHeight(90);
                profileImageView.setPreserveRatio(false);
                Circle clip = new Circle(45, 45, 45);
                profileImageView.setClip(clip);
                profileImageView.setVisible(true);
                avatarInitialLabel.setVisible(false);
                imageStatusLabel.setText("✅ " + selectedFile.getName());
                imageStatusLabel.setStyle("-fx-text-fill: #10B981; -fx-font-size: 11; -fx-font-weight: bold;");
            } catch (Exception e) {
                System.err.println("Erreur chargement image: " + e.getMessage());
            }
        }
    }

    @FXML
    void handleRemoveImage() {
        newImageSourcePath = null;
        removeImage = true;
        profileImageView.setImage(null);
        profileImageView.setVisible(false);
        showInitial();
        imageStatusLabel.setText("Image supprimée");
        imageStatusLabel.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11; -fx-font-weight: bold;");
    }

    // ====================== VALIDATION ======================

    private boolean validateNom() {
        String errorMsg = ValidationUtils.getNameErrorMessage(nomField.getText());
        if (!errorMsg.isEmpty()) { showError(errorNom, errorMsg); return false; }
        hideError(errorNom); return true;
    }

    private boolean validatePrenom() {
        String errorMsg = ValidationUtils.getNameErrorMessage(prenomField.getText());
        if (!errorMsg.isEmpty()) { showError(errorPrenom, errorMsg); return false; }
        hideError(errorPrenom); return true;
    }

    private boolean validateEmail() {
        String errorMsg = ValidationUtils.getEmailErrorMessage(emailField.getText());
        if (!errorMsg.isEmpty()) { showError(errorEmail, errorMsg); return false; }
        hideError(errorEmail); return true;
    }

    private boolean validateDate() {
        if (dateNaissanceField.getValue() == null) { showError(errorDate, "La date est obligatoire"); return false; }
        if (!ValidationUtils.isValidBirthDate(dateNaissanceField.getValue())) {
            showError(errorDate, ValidationUtils.getBirthDateErrorMessage(dateNaissanceField.getValue())); return false;
        }
        hideError(errorDate); return true;
    }

    private boolean validatePoids() {
        String poidsStr = poidsField.getText().trim();
        if (poidsStr.isEmpty()) return true;
        try {
            double poids = Double.parseDouble(poidsStr);
            if (!ValidationUtils.isValidPoids(poids)) { showError(errorPoids, ValidationUtils.getPoidsErrorMessage(poids)); return false; }
            hideError(errorPoids); return true;
        } catch (NumberFormatException e) { showError(errorPoids, "Nombre valide requis"); return false; }
    }

    private boolean validateTaille() {
        String tailleStr = tailleField.getText().trim();
        if (tailleStr.isEmpty()) return true;
        try {
            int taille = Integer.parseInt(tailleStr);
            if (!ValidationUtils.isValidTaille(taille)) { showError(errorTaille, ValidationUtils.getTailleErrorMessage(taille)); return false; }
            hideError(errorTaille); return true;
        } catch (NumberFormatException e) { showError(errorTaille, "Nombre entier valide requis"); return false; }
    }

    private boolean validateEtablissement() {
        String etablissement = etablissementField.getText();
        String errorMsg = ValidationUtils.getEtablissementErrorMessage(etablissement);
        if (!errorMsg.isEmpty()) { showError(errorEtablissement, errorMsg); return false; }
        hideError(errorEtablissement); return true;
    }

    private void showError(Label errorLabel, String message) {
        errorLabel.setText("⚠ " + message);
        errorLabel.setVisible(true);
    }

    private void hideError(Label errorLabel) { errorLabel.setVisible(false); }

    // ====================== SAUVEGARDE ======================

    @FXML
    void saveChanges() {
        boolean nomValid = validateNom();
        boolean prenomValid = validatePrenom();
        boolean emailValid = validateEmail();
        boolean dateValid = validateDate();
        boolean poidsValid = validatePoids();
        boolean tailleValid = validateTaille();
        boolean etablissementValid = validateEtablissement();

        if (!nomValid || !prenomValid || !emailValid || !dateValid || !poidsValid || !tailleValid || !etablissementValid) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de validation");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez corriger les erreurs avant de sauvegarder.");
            alert.showAndWait();
            return;
        }

        Sexe sexe = sexeCombo.getValue();
        Double poids = poidsField.getText().trim().isEmpty() ? null : Double.parseDouble(poidsField.getText().trim());
        Integer taille = tailleField.getText().trim().isEmpty() ? null : Integer.parseInt(tailleField.getText().trim());
        NiveauActivitePhysique niveauActivite = niveauActiviteCombo.getValue();
        NiveauScolaire niveauScolaire = niveauScolaireCombo.getValue();
        String etablissement = etablissementField.getText().trim();

        service.updateById(
                currentUser.getUser_id(),
                nomField.getText().trim(),
                prenomField.getText().trim(),
                emailField.getText().trim(),
                currentUser.getUser_password(),
                dateNaissanceField.getValue().toString(),
                currentUser.getDate_inscription(),
                currentUser.getType_utilisateur(),
                sexe, poids, taille, niveauActivite, niveauScolaire, etablissement,
                newImageSourcePath,
                removeImage
        );

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText("✅ Modifications enregistrées avec succès !");
        alert.showAndWait();

        closeWindow();
    }

    @FXML
    void closeWindow() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        stage.close();
    }
}
