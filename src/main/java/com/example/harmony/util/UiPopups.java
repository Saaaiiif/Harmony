package com.example.harmony.util;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.Objects;
import java.util.Optional;

public final class UiPopups {

    private UiPopups() {}

    public static final String STYLESHEET = "/com/example/harmony/styles.css";
    private static final double CORNER_RADIUS = 14;

    private static void centerOnOwner(Window owner, Window child) {
        if (owner != null) {
            child.setX(owner.getX() + (owner.getWidth() - child.getWidth()) / 2.0);
            child.setY(owner.getY() + (owner.getHeight() - child.getHeight()) / 2.0);
            return;
        }
        if (child instanceof Stage s) s.centerOnScreen();
    }

    public static Stage buildModalNoTitleBar(
            Stage owner,
            Parent content,
            double width,
            double height,
            boolean resizable,
            boolean darkMode,
            Class<?> resourceOwner
    ) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(resourceOwner, "resourceOwner");

        boolean canTransparent = Platform.isSupported(ConditionalFeature.TRANSPARENT_WINDOW);

        Stage popup = new Stage();
        if (owner != null) popup.initOwner(owner);
        popup.initModality(Modality.WINDOW_MODAL);
        popup.initStyle(canTransparent ? StageStyle.TRANSPARENT : StageStyle.UNDECORATED);
        popup.setResizable(resizable);

        StackPane wrapper = new StackPane(content);
        wrapper.getStyleClass().add("popup-root");
        if (!darkMode) wrapper.getStyleClass().add("light-mode");

        Scene scene = new Scene(wrapper, width, height);
        scene.setFill(Color.TRANSPARENT);

        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(CORNER_RADIUS * 2);
        clip.setArcHeight(CORNER_RADIUS * 2);
        wrapper.setClip(clip);
        wrapper.layoutBoundsProperty().addListener((obs, o, b) -> {
            clip.setWidth(b.getWidth());
            clip.setHeight(b.getHeight());
        });


        scene.getStylesheets().add(Objects.requireNonNull(
                resourceOwner.getResource(STYLESHEET),
                "Missing stylesheet: " + STYLESHEET
        ).toExternalForm());

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) popup.close();
        });

        popup.setScene(scene);

        popup.setOnShown(ev -> Platform.runLater(() -> centerOnOwner(owner, popup)));

        return popup;
    }

    public static Stage showModalNoTitleBar(
            Stage owner,
            Parent content,
            double width,
            double height,
            boolean resizable,
            boolean darkMode,
            Class<?> resourceOwner
    ) {
        Stage s = buildModalNoTitleBar(owner, content, width, height, resizable, darkMode, resourceOwner);
        s.showAndWait();
        return s;
    }

    public static void styleDialog(Dialog<?> dialog, boolean darkMode, Class<?> resourceOwner) {
        Objects.requireNonNull(dialog, "dialog");
        Objects.requireNonNull(resourceOwner, "resourceOwner");

        boolean canTransparent = Platform.isSupported(ConditionalFeature.TRANSPARENT_WINDOW);

        dialog.initStyle(canTransparent ? StageStyle.TRANSPARENT : StageStyle.UNDECORATED);

        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(Objects.requireNonNull(
                resourceOwner.getResource(STYLESHEET),
                "Missing stylesheet: " + STYLESHEET
        ).toExternalForm());

        pane.getStyleClass().add("popup-root");
        if (!darkMode) pane.getStyleClass().add("light-mode");

        for (ButtonType bt : pane.getButtonTypes()) {
            Node b = pane.lookupButton(bt);
            if (b != null) b.getStyleClass().add("action-button");
        }

        pane.sceneProperty().addListener((obs, old, scene) -> {
            if (scene == null) return;

            if (canTransparent) scene.setFill(Color.TRANSPARENT);

            if (canTransparent) {
                Rectangle clip = new Rectangle();
                clip.setArcWidth(CORNER_RADIUS * 2);
                clip.setArcHeight(CORNER_RADIUS * 2);
                pane.setClip(clip);
                pane.layoutBoundsProperty().addListener((o, ov, b) -> {
                    clip.setWidth(b.getWidth());
                    clip.setHeight(b.getHeight());
                });
            }
        });

        dialog.setOnShown(ev -> Platform.runLater(() -> {
            Scene sc = pane.getScene();
            if (sc == null) return;
            Window w = sc.getWindow();
            if (w == null) return;
            centerOnOwner(dialog.getOwner(), w);
        }));

    }

    public static void showInfo(Stage owner, String message, boolean darkMode, Class<?> resourceOwner) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        if (owner != null) a.initOwner(owner);
        a.setHeaderText(null);
        styleDialog(a, darkMode, resourceOwner);
        a.showAndWait();
    }

    public static void showWarning(Stage owner, String message, boolean darkMode, Class<?> resourceOwner) {
        Alert a = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        if (owner != null) a.initOwner(owner);
        a.setHeaderText(null);
        styleDialog(a, darkMode, resourceOwner);
        a.showAndWait();
    }

    public static void showError(Stage owner, String message, boolean darkMode, Class<?> resourceOwner) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        if (owner != null) a.initOwner(owner);
        a.setHeaderText(null);
        styleDialog(a, darkMode, resourceOwner);
        a.showAndWait();
    }

    public static boolean confirm(Stage owner, String message, boolean darkMode, Class<?> resourceOwner) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        if (owner != null) a.initOwner(owner);
        a.setHeaderText(null);
        styleDialog(a, darkMode, resourceOwner);
        Optional<ButtonType> res = a.showAndWait();
        return res.isPresent() && res.get() == ButtonType.OK;
    }

    public static Optional<String> prompt(
            Stage owner,
            String title,
            String contentText,
            String initialValue,
            boolean darkMode,
            Class<?> resourceOwner
    ) {
        TextInputDialog d = new TextInputDialog(initialValue);
        if (owner != null) d.initOwner(owner);
        d.setTitle(title);
        d.setHeaderText(null);
        d.setContentText(contentText);
        styleDialog(d, darkMode, resourceOwner);
        return d.showAndWait();
    }
}
