package controllers.ForumControllers.BackOffice;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import models.ForumModels.Post;
import services.ForumServices.ServicePost;

public class EditPostController {

    @FXML private TextField titreField;
    @FXML private TextArea contenuField;

    private Post post;
    private ServicePost service = new ServicePost();

    public void setPost(Post post) {
        this.post = post;
        titreField.setText(post.getTitre());
        contenuField.setText(post.getContenu());
    }

    @FXML
    private void handleSave() {
        post.setTitre(titreField.getText());
        post.setContenu(contenuField.getText());

        service.update(post);

        Stage stage = (Stage) titreField.getScene().getWindow();
        stage.close();
    }
}