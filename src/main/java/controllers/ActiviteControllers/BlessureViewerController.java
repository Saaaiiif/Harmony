package controllers.ActiviteControllers;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ═══════════════════════════════════════════════════════════
 *  BlessureViewerController
 *  Visualisation 3D des blessures — Canvas 2D pur (no WebGL)
 *  Compatible JavaFX WebView 17+
 * ═══════════════════════════════════════════════════════════
 */
public class BlessureViewerController {

    // ── Données des blessures (clé JS → [Nom affiché, Emoji, Sous-titre]) ──
    private static final Map<String, String[]> BLESSURES = new LinkedHashMap<>();
    static {
        BLESSURES.put("genou",    new String[]{"Genou",    "🦵", "Ligaments / Entorse"});
        BLESSURES.put("dos",      new String[]{"Dos",      "🔙", "Lombaires / Contracture"});
        BLESSURES.put("epaule",   new String[]{"Épaule",   "💪", "Coiffe des rotateurs"});
        BLESSURES.put("cheville", new String[]{"Cheville", "🦶", "Entorse latérale"});
        BLESSURES.put("coude",    new String[]{"Coude",    "💪", "Épicondylite"});
        BLESSURES.put("hanche",   new String[]{"Hanche",   "🦴", "Fléchisseurs / Piriforme"});
        BLESSURES.put("nuque",    new String[]{"Nuque",    "🧠", "Cervicales / Trapèzes"});
        BLESSURES.put("poignet",  new String[]{"Poignet",  "✋", "Tendons / Canal carpien"});
    }

    private WebEngine webEngine;
    // volatile : modifié depuis le thread WebKit, lu depuis FX thread
    private volatile boolean webLoaded  = false;
    private volatile boolean webFailed  = false;
    private String  selectedKey         = null;
    private Button  lastBtn             = null;

    // ═══════════════════════════════════════════════════════
    //  Ouvre la fenêtre (appelé depuis JournalExercicesController)
    // ═══════════════════════════════════════════════════════
    public static void openBlessureViewer(javafx.stage.Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setTitle("Harmony - Visualisation 3D des Blessures");

        BlessureViewerController ctrl = new BlessureViewerController();
        BorderPane root = ctrl.buildUI(stage);

        Scene scene = new Scene(root, 1100, 700);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // Ouverture en fondu
        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(350), root);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        stage.showAndWait();
    }

    // ═══════════════════════════════════════════════════════
    //  Construction de l'interface
    // ═══════════════════════════════════════════════════════
    private BorderPane buildUI(Stage stage) {
        BorderPane root = new BorderPane();
        root.setStyle(
                "-fx-background-color: #1E0A3C;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-color: #8B5CF6;" +
                        "-fx-border-width: 2;" +
                        "-fx-border-radius: 20;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(139,92,246,0.6), 40, 0, 0, 0);"
        );
        root.setTop(buildHeader(stage));
        root.setLeft(buildLeftPanel());
        root.setCenter(buildWebViewPane());
        return root;
    }

    // ── En-tête ───────────────────────────────────────────
    private HBox buildHeader(Stage stage) {
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setStyle("-fx-background-color: rgba(139,92,246,0.15); -fx-background-radius: 20 20 0 0;");

        Label icon = new Label("🦴");
        icon.setStyle("-fx-font-size: 26px;");

        VBox titleBox = new VBox(2);
        Label title = new Label("Visualisation 3D des Blessures");
        title.setStyle("-fx-font-size: 19px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subtitle = new Label("Modèle interactif 360° • Sélectionnez une zone pour les détails et conseils de récupération");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #a78bfa;");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnReset = new Button("🔄 Vue initiale");
        btnReset.setStyle(
                "-fx-background-color: rgba(139,92,246,0.3);" +
                        "-fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-background-radius: 10; -fx-padding: 8 15; -fx-cursor: hand;" +
                        "-fx-border-color: rgba(139,92,246,0.5); -fx-border-radius: 10; -fx-border-width: 1;"
        );
        btnReset.setOnAction(e -> {
            selectedKey = null;
            if (lastBtn != null) resetBtnStyle(lastBtn);
            callJS("window.resetView()");
        });

        Button btnClose = new Button("✕");
        btnClose.setStyle(
                "-fx-background-color: rgba(239,68,68,0.3);" +
                        "-fx-text-fill: #fca5a5; -fx-font-weight: bold; -fx-font-size: 16px;" +
                        "-fx-background-radius: 50; -fx-min-width: 36; -fx-min-height: 36; -fx-cursor: hand;" +
                        "-fx-border-color: rgba(239,68,68,0.5); -fx-border-radius: 50; -fx-border-width: 1;"
        );
        btnClose.setOnAction(e -> stage.close());

        header.getChildren().addAll(icon, titleBox, spacer, btnReset, btnClose);
        return header;
    }

    // ── Panneau gauche (liste blessures) ──────────────────
    private VBox buildLeftPanel() {
        VBox panel = new VBox(8);
        panel.setPadding(new Insets(20, 15, 20, 20));
        panel.setPrefWidth(205);
        panel.setStyle("-fx-background-color: rgba(0,0,0,0.22);");

        Label panelTitle = new Label("🎯 Zones de Blessures");
        panelTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #a78bfa; -fx-padding: 0 0 8 0;");
        panel.getChildren().add(panelTitle);

        for (Map.Entry<String, String[]> entry : BLESSURES.entrySet()) {
            final String key = entry.getKey();
            String[] info = entry.getValue();

            Button btn = new Button();
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setCursor(javafx.scene.Cursor.HAND);

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            Label emoji = new Label(info[1]);
            emoji.setStyle("-fx-font-size: 20px;");
            VBox textBox = new VBox(2);
            Label nameLbl = new Label(info[0]);
            nameLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white;");
            Label descLbl = new Label(info[2]);
            descLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");
            textBox.getChildren().addAll(nameLbl, descLbl);
            row.getChildren().addAll(emoji, textBox);
            btn.setGraphic(row);
            btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            resetBtnStyle(btn);

            btn.setOnMouseEntered(e -> {
                if (!key.equals(selectedKey))
                    btn.setStyle("-fx-background-color: rgba(139,92,246,0.2);" +
                            "-fx-background-radius: 12; -fx-padding: 12 15;" +
                            "-fx-border-color: rgba(139,92,246,0.4);" +
                            "-fx-border-radius: 12; -fx-border-width: 1;");
            });
            btn.setOnMouseExited(e -> {
                if (!key.equals(selectedKey)) resetBtnStyle(btn);
            });
            btn.setOnAction(e -> {
                if (lastBtn != null && lastBtn != btn) resetBtnStyle(lastBtn);
                selectedKey = key;
                lastBtn = btn;
                btn.setStyle(
                        "-fx-background-color: rgba(239,68,68,0.25);" +
                                "-fx-background-radius: 12; -fx-padding: 12 15;" +
                                "-fx-border-color: #ef4444;" +
                                "-fx-border-radius: 12; -fx-border-width: 2;" +
                                "-fx-effect: dropshadow(three-pass-box, rgba(239,68,68,0.4), 10, 0, 0, 0);"
                );
                // ── Appel JS vers la page HTML ──────────────────────────────
                // On échappe la clé pour éviter toute injection
                final String safeKey = key.replaceAll("[^a-zA-Z0-9_]", "");
                callJS("window.showBodyInjury('" + safeKey + "')");
            });

            panel.getChildren().add(btn);
        }

        // Aide en bas
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox hint = new VBox(5);
        hint.setStyle("-fx-background-color: rgba(139,92,246,0.08);" +
                "-fx-background-radius: 10; -fx-padding: 12;");
        Label hintTitle = new Label("ℹ️ Comment utiliser");
        hintTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #a78bfa;");
        Label hintText = new Label("• Cliquez une blessure\n• Glissez pour tourner\n• Molette pour zoomer");
        hintText.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af; -fx-padding: 3 0 0 5;");
        hint.getChildren().addAll(hintTitle, hintText);
        panel.getChildren().addAll(spacer, hint);
        return panel;
    }

    // ── Zone WebView ──────────────────────────────────────
    private StackPane buildWebViewPane() {
        StackPane pane = new StackPane();

        // Indicateur de chargement
        VBox loadingBox = new VBox(15);
        loadingBox.setAlignment(Pos.CENTER);
        loadingBox.setStyle("-fx-background-color: #1E0A3C;");
        ProgressIndicator prog = new ProgressIndicator();
        prog.setStyle("-fx-accent: #8B5CF6;");
        prog.setPrefSize(55, 55);
        Label loadLbl = new Label("⏳ Chargement du modèle 3D...");
        loadLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #a78bfa;");
        loadingBox.getChildren().addAll(prog, loadLbl);
        pane.getChildren().add(loadingBox);

        // WebView
        WebView webView = new WebView();
        webEngine = webView.getEngine();
        webView.setVisible(false);

        // Désactiver les popups de confirmation JS (évite les freezes)
        webEngine.setConfirmHandler(msg -> true);
        webEngine.setPromptHandler(data -> "");
        webEngine.setOnAlert(evt -> System.out.println("[HTML alert] " + evt.getData()));

        // Charger le fichier HTML
        URL htmlUrl = getClass().getResource("/views/ActiviteViews/html/body3d_viewer.html");
        if (htmlUrl != null) {
            System.out.println("[BlessureViewer] Chargement: " + htmlUrl.toExternalForm());
            webEngine.load(htmlUrl.toExternalForm());
        } else {
            System.err.println("[BlessureViewer] ❌ body3d_viewer.html introuvable dans /html/");
            loadLbl.setText("❌ Fichier introuvable.\nVérifiez: src/main/resources/html/body3d_viewer.html");
            loadLbl.setStyle("-fx-text-fill: #ef4444; -fx-text-alignment: center;");
            prog.setVisible(false);
            return pane;
        }

        // Gérer les transitions de chargement
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            System.out.println("[BlessureViewer] WebView state: " + newState);
            if (newState == Worker.State.SUCCEEDED) {
                webLoaded = true;
                Platform.runLater(() -> {
                    webView.setVisible(true);
                    pane.getChildren().remove(loadingBox);
                    System.out.println("[BlessureViewer] ✅ Page chargée avec succès");
                });
            } else if (newState == Worker.State.FAILED) {
                webFailed = true;
                Throwable ex = webEngine.getLoadWorker().getException();
                System.err.println("[BlessureViewer] ❌ Erreur: " + (ex != null ? ex.getMessage() : "inconnue"));
                Platform.runLater(() ->
                        loadLbl.setText("❌ Erreur de chargement. Vérifiez la console.")
                );
            }
        });

        // Intercepter les erreurs JS pour le débogage
        webEngine.getLoadWorker().exceptionProperty().addListener((obs, oldEx, newEx) -> {
            if (newEx != null) System.err.println("[BlessureViewer] Exception: " + newEx.getMessage());
        });

        pane.getChildren().add(webView);
        return pane;
    }

    // ═══════════════════════════════════════════════════════
    //  Exécution JS sécurisée — attend que la page soit prête
    // ═══════════════════════════════════════════════════════
    private void callJS(final String script) {
        if (webFailed) {
            System.err.println("[BlessureViewer] callJS ignoré (page en erreur)");
            return;
        }
        if (webLoaded) {
            // Page déjà prête : exécuter immédiatement sur le FX thread
            Platform.runLater(() -> executeScriptSafe(script));
        } else {
            // Page pas encore chargée : attendre en arrière-plan
            new Thread(() -> {
                int tries = 0;
                while (!webLoaded && !webFailed && tries < 40) {
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                    tries++;
                }
                if (webLoaded) {
                    Platform.runLater(() -> executeScriptSafe(script));
                } else {
                    System.err.println("[BlessureViewer] Timeout: page non chargée après 4s");
                }
            }, "BlessureViewer-JSWaiter").start();
        }
    }

    private void executeScriptSafe(String script) {
        try {
            Object result = webEngine.executeScript(script);
            System.out.println("[BlessureViewer] JS ok: " + script.substring(0, Math.min(60, script.length())));
        } catch (Exception ex) {
            System.err.println("[BlessureViewer] JS erreur: " + ex.getMessage() + " | script: " + script);
        }
    }

    private void resetBtnStyle(Button btn) {
        btn.setStyle(
                "-fx-background-color: rgba(255,255,255,0.04);" +
                        "-fx-background-radius: 12; -fx-padding: 12 15;" +
                        "-fx-border-color: rgba(255,255,255,0.08);" +
                        "-fx-border-radius: 12; -fx-border-width: 1;"
        );
    }
}