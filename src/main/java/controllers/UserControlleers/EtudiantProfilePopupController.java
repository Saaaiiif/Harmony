package controllers.UserControlleers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.UserModels.*;
import services.UserServices.serviceUser;
import utils.PasswordUtils;
import utils.ValidationUtils;

import java.io.File;
import java.time.LocalDate;

/**
 * Controller du popup "Modifier mon profil" pour l'étudiant (ETUDIANT).
 *
 * Même pattern que AdminProfilePopupController, avec les champs
 * supplémentaires propres aux étudiants :
 *   - Poids, Taille, NiveauActivitePhysique
 *   - NiveauScolaire, Etablissement
 *
 * Usage dans AccueilController :
 *   ctrl.setup(etudiant, popupStage, refreshCallback);
 *   popup.showAndWait();
 */
public class EtudiantProfilePopupController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private StackPane  popupAvatarContainer;
    @FXML private ImageView  popupAvatarImage;
    @FXML private Label      popupAvatarInitial;
    @FXML private Label      headerName;

    // Champs communs
    @FXML private TextField  fieldPrenom;
    @FXML private TextField  fieldNom;
    @FXML private TextField  fieldEmail;
    @FXML private DatePicker fieldDateNaissance;
    @FXML private ComboBox<Sexe> fieldSexe;

    // Champs spécifiques étudiant
    @FXML private TextField  fieldPoids;
    @FXML private TextField  fieldTaille;
    @FXML private ComboBox<NiveauActivitePhysique> fieldActivite;
    @FXML private ComboBox<NiveauScolaire>         fieldNiveauScolaire;
    @FXML private TextField  fieldEtablissement;

    // Mot de passe
    @FXML private PasswordField fieldPassword;
    @FXML private PasswordField fieldPasswordConfirm;

    // Labels d'erreur
    @FXML private Label errorPrenom;
    @FXML private Label errorNom;
    @FXML private Label errorEmail;
    @FXML private Label errorPassword;
    @FXML private Label globalMessage;

    @FXML private Button btnSauvegarder;
    @FXML private Button btnSupprPhoto;

    // ── État ──────────────────────────────────────────────────────────────────
    private user etudiant;
    private Stage   popupStage;
    private Runnable onSaveCallback;   // rafraîchit l'avatar dans la navbar
    private String  newImagePath = null;
    private boolean removeImage  = false;

    private final serviceUser service = new serviceUser();

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @FXML
    public void initialize() {
        fieldSexe.setItems(FXCollections.observableArrayList(Sexe.values()));
        fieldActivite.setItems(FXCollections.observableArrayList(NiveauActivitePhysique.values()));
        fieldNiveauScolaire.setItems(FXCollections.observableArrayList(NiveauScolaire.values()));
        hideAllErrors();
    }

    /**
     * Configure le popup avec l'étudiant courant et le Stage.
     * À appeler AVANT showAndWait().
     */
    public void setup(user etudiant, Stage stage, Runnable onSave) {
        this.etudiant        = etudiant;
        this.popupStage      = stage;
        this.onSaveCallback  = onSave;
        populateFields();
    }

    // =========================================================================
    //  REMPLISSAGE DES CHAMPS
    // =========================================================================

    private void populateFields() {
        if (etudiant == null) return;

        fieldPrenom.setText(nvl(etudiant.getUser_prenom()));
        fieldNom.setText(nvl(etudiant.getUser_nom()));
        fieldEmail.setText(nvl(etudiant.getUser_email()));

        if (etudiant.getUser_date_de_naissance() != null) {
            try { fieldDateNaissance.setValue(LocalDate.parse(etudiant.getUser_date_de_naissance())); }
            catch (Exception ignored) {}
        }

        if (etudiant.getUser_sexe() != null) fieldSexe.setValue(etudiant.getUser_sexe());

        // Champs étudiant
        if (etudiant.getUser_poids() != null)
            fieldPoids.setText(String.valueOf(etudiant.getUser_poids()));
        if (etudiant.getUser_taille() != null)
            fieldTaille.setText(String.valueOf(etudiant.getUser_taille()));
        if (etudiant.getUser_niveau_activite_physique() != null)
            fieldActivite.setValue(etudiant.getUser_niveau_activite_physique());
        if (etudiant.getUser_niveau_scolaire() != null)
            fieldNiveauScolaire.setValue(etudiant.getUser_niveau_scolaire());
        fieldEtablissement.setText(nvl(etudiant.getUser_etablissement_scolaire()));

        // En-tête nom
        String nom = nvl(etudiant.getUser_prenom()) + " " + nvl(etudiant.getUser_nom());
        headerName.setText(nom.trim().isEmpty() ? "Étudiant" : nom.trim());

        // Avatar
        refreshAvatarPreview(etudiant.getUser_image_path());
    }

    private void refreshAvatarPreview(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            File imgFile = new File(imagePath);
            if (imgFile.exists()) {
                try {
                    Image img = new Image(imgFile.toURI().toString(), 80, 80, false, true);
                    popupAvatarImage.setImage(img);
                    popupAvatarImage.setVisible(true);
                    popupAvatarInitial.setVisible(false);
                    return;
                } catch (Exception ignored) {}
            }
        }
        // Fallback initiale
        String initial = !fieldPrenom.getText().isEmpty()
                ? fieldPrenom.getText().substring(0, 1).toUpperCase() : "E";
        popupAvatarInitial.setText(initial);
        popupAvatarInitial.setVisible(true);
        popupAvatarImage.setVisible(false);
    }

    // =========================================================================
    //  ACTIONS PHOTO
    // =========================================================================

    @FXML
    void handleChangeImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo de profil");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File file = fc.showOpenDialog(popupStage);
        if (file != null) {
            newImagePath = file.getAbsolutePath();
            removeImage  = false;
            refreshAvatarPreview(newImagePath);
        }
    }

    @FXML
    void handleSupprimerPhoto() {
        removeImage  = true;
        newImagePath = null;
        popupAvatarImage.setVisible(false);
        popupAvatarInitial.setVisible(true);
        String initial = !fieldPrenom.getText().isEmpty()
                ? fieldPrenom.getText().substring(0, 1).toUpperCase() : "E";
        popupAvatarInitial.setText(initial);
    }

    // =========================================================================
    //  SAUVEGARDE
    // =========================================================================

    @FXML
    void handleSauvegarder() {
        if (!validateAll()) return;

        btnSauvegarder.setDisable(true);
        btnSauvegarder.setText("Sauvegarde...");

        try {
            String prenom       = fieldPrenom.getText().trim();
            String nom          = fieldNom.getText().trim();
            String email        = fieldEmail.getText().trim();
            String dateStr      = fieldDateNaissance.getValue() != null
                    ? fieldDateNaissance.getValue().toString()
                    : etudiant.getUser_date_de_naissance();
            Sexe sexe           = fieldSexe.getValue();
            String newPass      = fieldPassword.getText();

            // Champs étudiant
            Double poids = null;
            try { poids = Double.parseDouble(fieldPoids.getText().trim()); } catch (Exception ignored) {}

            Integer taille = null;
            try { taille = Integer.parseInt(fieldTaille.getText().trim()); } catch (Exception ignored) {}

            NiveauActivitePhysique activite = fieldActivite.getValue();
            NiveauScolaire niveauScolaire   = fieldNiveauScolaire.getValue();
            String etablissement            = fieldEtablissement.getText().trim();

            // ── Mise à jour en BDD ────────────────────────────────────────────
            service.updateById(
                    etudiant.getUser_id(),
                    nom, prenom, email,
                    etudiant.getUser_password(), // haché existant (inchangé si vide)
                    dateStr,
                    etudiant.getDate_inscription(),
                    etudiant.getType_utilisateur(),
                    sexe,
                    poids,
                    taille,
                    activite,
                    niveauScolaire,
                    etablissement,
                    removeImage ? null : newImagePath,
                    removeImage
            );

            // ── Changement de mot de passe (optionnel) ────────────────────────
            if (!newPass.isEmpty()) {
                updatePassword(etudiant.getUser_id(), newPass);
            }

            // ── Mettre à jour la session en mémoire ───────────────────────────
            user refreshed = service.getOneById(etudiant.getUser_id());
            if (refreshed != null) {
                Session s = Session.getInstance();
                s.getUser().setUser_nom(refreshed.getUser_nom());
                s.getUser().setUser_prenom(refreshed.getUser_prenom());
                s.getUser().setUser_email(refreshed.getUser_email());
                s.getUser().setUser_image_path(refreshed.getUser_image_path());
                s.getUser().setUser_sexe(refreshed.getUser_sexe());
                s.getUser().setUser_date_de_naissance(refreshed.getUser_date_de_naissance());
                s.getUser().setUser_poids(refreshed.getUser_poids());
                s.getUser().setUser_taille(refreshed.getUser_taille());
                s.getUser().setUser_niveau_activite_physique(refreshed.getUser_niveau_activite_physique());
                s.getUser().setUser_niveau_scolaire(refreshed.getUser_niveau_scolaire());
                s.getUser().setUser_etablissement_scolaire(refreshed.getUser_etablissement_scolaire());
            }

            // ── Callback → rafraîchir l'avatar dans la navbar ─────────────────
            if (onSaveCallback != null) onSaveCallback.run();

            showSuccess("✅ Profil mis à jour avec succès !");

            // Fermer après 1.5 secondes
            javafx.animation.PauseTransition pause =
                    new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.5));
            pause.setOnFinished(e -> popupStage.close());
            pause.play();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de la sauvegarde : " + e.getMessage());
            btnSauvegarder.setDisable(false);
            btnSauvegarder.setText("✓  Sauvegarder");
        }
    }

    /**
     * Met à jour uniquement le mot de passe (re-hachage BCrypt).
     * Même logique que dans AdminProfilePopupController.
     */
    private void updatePassword(int userId, String plainPassword) {
        String hashed = PasswordUtils.hashPassword(plainPassword);
        String sql    = "UPDATE `user` SET `user_password` = ? WHERE `user_id` = ?";
        try {
            java.sql.Connection cnx = utils.MyDataBase.getInstance().getCnx();
            try (java.sql.PreparedStatement pstm = cnx.prepareStatement(sql)) {
                pstm.setString(1, hashed);
                pstm.setInt(2, userId);
                pstm.executeUpdate();
            }
            Session.getInstance().getUser().setUser_password(hashed);
        } catch (java.sql.SQLException e) {
            System.err.println("Erreur update password : " + e.getMessage());
        }
    }

    @FXML
    void handleAnnuler() {
        popupStage.close();
    }

    // =========================================================================
    //  VALIDATION
    // =========================================================================

    private boolean validateAll() {
        hideAllErrors();
        boolean ok = true;

        String prenom = fieldPrenom.getText().trim();
        if (!ValidationUtils.isValidName(prenom)) {
            showFieldError(errorPrenom, ValidationUtils.getNameErrorMessage(prenom));
            ok = false;
        }

        String nom = fieldNom.getText().trim();
        if (!ValidationUtils.isValidName(nom)) {
            showFieldError(errorNom, ValidationUtils.getNameErrorMessage(nom));
            ok = false;
        }

        String email = fieldEmail.getText().trim();
        if (!ValidationUtils.isValidEmail(email)) {
            showFieldError(errorEmail, ValidationUtils.getEmailErrorMessage(email));
            ok = false;
        }

        String pass    = fieldPassword.getText();
        String confirm = fieldPasswordConfirm.getText();
        if (!pass.isEmpty()) {
            if (!ValidationUtils.isValidPassword(pass)) {
                showFieldError(errorPassword, ValidationUtils.getPasswordErrorMessage(pass));
                ok = false;
            } else if (!pass.equals(confirm)) {
                showFieldError(errorPassword, "Les mots de passe ne correspondent pas.");
                ok = false;
            }
        }

        return ok;
    }

    private void hideAllErrors() {
        errorPrenom.setVisible(false);
        errorNom.setVisible(false);
        errorEmail.setVisible(false);
        errorPassword.setVisible(false);
        globalMessage.setVisible(false);
    }

    private void showFieldError(Label lbl, String msg) {
        lbl.setText("⚠ " + msg);
        lbl.setVisible(true);
    }

    private void showSuccess(String msg) {
        globalMessage.setText(msg);
        globalMessage.setStyle("-fx-text-fill: #059669; -fx-font-size: 12; -fx-font-weight: bold;");
        globalMessage.setVisible(true);
    }

    private void showError(String msg) {
        globalMessage.setText(msg);
        globalMessage.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12; -fx-font-weight: bold;");
        globalMessage.setVisible(true);
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
