package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Session;
import models.Sexe;
import models.user;
import services.serviceUser;
import utils.PasswordUtils;
import utils.ValidationUtils;

import java.io.File;
import java.time.LocalDate;

/**
 * Controller du popup "Modifier mon profil" pour l'administrateur.
 *
 * Usage dans AdminSidebarController :
 *   ctrl.setup(admin, popupStage, refreshCallback);
 *   popup.showAndWait();
 */
public class AdminProfilePopupController {

    // ── FXML ──────────────────────────────────────────────────────────────────
    @FXML private StackPane  popupAvatarContainer;
    @FXML private ImageView  popupAvatarImage;
    @FXML private Label      popupAvatarInitial;
    @FXML private Label      headerName;

    @FXML private TextField  fieldPrenom;
    @FXML private TextField  fieldNom;
    @FXML private TextField  fieldEmail;
    @FXML private DatePicker fieldDateNaissance;
    @FXML private ComboBox<Sexe> fieldSexe;

    @FXML private PasswordField fieldPassword;
    @FXML private PasswordField fieldPasswordConfirm;

    @FXML private Label errorPrenom;
    @FXML private Label errorNom;
    @FXML private Label errorEmail;
    @FXML private Label errorPassword;
    @FXML private Label globalMessage;

    @FXML private Button btnSauvegarder;
    @FXML private Button btnSupprPhoto;

    // ── État ──────────────────────────────────────────────────────────────────
    private user             admin;
    private Stage            popupStage;
    private Runnable         onSaveCallback;   // rafraîchit la sidebar après sauvegarde
    private String           newImagePath = null;   // null = pas de changement
    private boolean          removeImage  = false;  // true = supprimer l'image existante

    private final serviceUser service = new serviceUser();

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    @FXML
    public void initialize() {
        fieldSexe.setItems(FXCollections.observableArrayList(Sexe.values()));
        hideAllErrors();
    }

    /**
     * Configure le popup avec l'admin courant et le Stage.
     * À appeler AVANT showAndWait().
     */
    public void setup(user admin, Stage stage, Runnable onSave) {
        this.admin          = admin;
        this.popupStage     = stage;
        this.onSaveCallback = onSave;
        populateFields();
    }

    // =========================================================================
    //  REMPLISSAGE DES CHAMPS
    // =========================================================================

    private void populateFields() {
        if (admin == null) return;

        fieldPrenom.setText(nvl(admin.getUser_prenom()));
        fieldNom.setText(nvl(admin.getUser_nom()));
        fieldEmail.setText(nvl(admin.getUser_email()));

        if (admin.getUser_date_de_naissance() != null) {
            try {
                fieldDateNaissance.setValue(LocalDate.parse(admin.getUser_date_de_naissance()));
            } catch (Exception ignored) {}
        }

        if (admin.getUser_sexe() != null) fieldSexe.setValue(admin.getUser_sexe());

        // En-tête
        String nom = nvl(admin.getUser_prenom()) + " " + nvl(admin.getUser_nom());
        headerName.setText(nom.trim().isEmpty() ? "Administrateur" : nom.trim());

        // Avatar
        refreshAvatarPreview(admin.getUser_image_path());
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
        String initial = (!fieldPrenom.getText().isEmpty())
                ? fieldPrenom.getText().substring(0, 1).toUpperCase() : "A";
        popupAvatarInitial.setText(initial);
        popupAvatarInitial.setVisible(true);
        popupAvatarImage.setVisible(false);
    }

    // =========================================================================
    //  ACTIONS PHOTO
    // =========================================================================

    /**
     * Clic sur l'avatar → sélectionner une nouvelle photo.
     */
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
                ? fieldPrenom.getText().substring(0, 1).toUpperCase() : "A";
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
            String prenom     = fieldPrenom.getText().trim();
            String nom        = fieldNom.getText().trim();
            String email      = fieldEmail.getText().trim();
            String dateStr    = fieldDateNaissance.getValue() != null
                    ? fieldDateNaissance.getValue().toString()
                    : admin.getUser_date_de_naissance();
            Sexe sexe         = fieldSexe.getValue();
            String newPass    = fieldPassword.getText();

            // ── Mise à jour en BDD ────────────────────────────────────────────
            service.updateById(
                    admin.getUser_id(),
                    nom, prenom, email,
                    admin.getUser_password(), // mot de passe haché existant (inchangé si vide)
                    dateStr,
                    admin.getDate_inscription(),
                    admin.getType_utilisateur(),
                    sexe,
                    admin.getUser_poids(),
                    admin.getUser_taille(),
                    admin.getUser_niveau_activite_physique(),
                    admin.getUser_niveau_scolaire(),
                    admin.getUser_etablissement_scolaire(),
                    removeImage ? null : newImagePath,
                    removeImage
            );

            // ── Changement de mot de passe (séparé) ──────────────────────────
            if (!newPass.isEmpty()) {
                updatePassword(admin.getUser_id(), newPass);
            }

            // ── Mettre à jour la session en mémoire ───────────────────────────
            user refreshed = service.getOneById(admin.getUser_id());
            if (refreshed != null) {
                Session.getInstance().getUser().setUser_nom(refreshed.getUser_nom());
                Session.getInstance().getUser().setUser_prenom(refreshed.getUser_prenom());
                Session.getInstance().getUser().setUser_email(refreshed.getUser_email());
                Session.getInstance().getUser().setUser_image_path(refreshed.getUser_image_path());
                Session.getInstance().getUser().setUser_sexe(refreshed.getUser_sexe());
                Session.getInstance().getUser().setUser_date_de_naissance(refreshed.getUser_date_de_naissance());
            }

            // ── Callback → rafraîchir l'avatar dans la sidebar ───────────────
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
            btnSauvegarder.setText("✓ Sauvegarder");
        }
    }

    /**
     * Met à jour uniquement le mot de passe (re-hachage BCrypt).
     */
    private void updatePassword(int userId, String plainPassword) {
        String hashed = PasswordUtils.hashPassword(plainPassword);
        String sql = "UPDATE `user` SET `user_password` = ? WHERE `user_id` = ?";
        try {
            java.sql.Connection cnx = utils.MyDataBase.getInstance().getCnx();
            try (java.sql.PreparedStatement pstm = cnx.prepareStatement(sql)) {
                pstm.setString(1, hashed);
                pstm.setInt(2, userId);
                pstm.executeUpdate();
            }
            // Mettre à jour en session
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

    // =========================================================================
    //  UTILITAIRE
    // =========================================================================
    private String nvl(String s) { return s != null ? s : ""; }
}
