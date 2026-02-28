package controllers.ForumControllers.BackOffice;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.control.TextArea;
import models.ForumModels.Commentaire;
import services.ForumServices.ServiceCommentaire;

public class EditCommentaireController {

    @FXML private TextArea contenuField;

    private Commentaire commentaire;
    private ServiceCommentaire service = new ServiceCommentaire();

    public void setCommentaire(Commentaire commentaire) {
        this.commentaire = commentaire;
        contenuField.setText(commentaire.getContenu());
    }

    @FXML
    private void handleSave() {
        commentaire.setContenu(contenuField.getText());

        service.update(commentaire);

        Stage stage = (Stage) contenuField.getScene().getWindow();
        stage.close();
    }
}