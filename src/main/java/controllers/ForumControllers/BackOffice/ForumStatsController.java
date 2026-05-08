package controllers.ForumControllers.BackOffice;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import services.ForumServices.ForumStatsService;
import java.util.Map;

public class ForumStatsController {

    // ── KPI labels ────────────────────────────────────────────────────────────
    @FXML private Label labelTotalPosts;
    @FXML private Label labelTotalCommentaires;
    @FXML private Label labelTotalCategories;
    @FXML private Label labelAuteursActifs;

    // ── Graphiques ────────────────────────────────────────────────────────────
    @FXML private PieChart pieCategories;
    @FXML private BarChart<String, Number> barTopAuteurs;
    @FXML private LineChart<String, Number> linePostsParJour;
    @FXML private BarChart<String, Number> barCommentairesParPost;

    private final ForumStatsService stats = new ForumStatsService();

    @FXML
    public void initialize() {
        loadKPIs();
        loadPie();
        loadLine();
        loadBarAuteurs();
        loadBarCommentaires();
    }

    @FXML
    private void handleRefresh() { initialize(); }

    // ── KPIs ─────────────────────────────────────────────────────────────────
    private void loadKPIs() {
        labelTotalPosts.setText(String.valueOf(stats.getTotalPosts()));
        labelTotalCommentaires.setText(String.valueOf(stats.getTotalCommentaires()));
        labelTotalCategories.setText(String.valueOf(stats.getTotalCategories()));
        labelAuteursActifs.setText(String.valueOf(stats.getAuteursActifs()));
    }

    // ── PieChart — posts par catégorie ────────────────────────────────────────
    private void loadPie() {
        pieCategories.getData().clear();
        Map<String, Integer> data = stats.getPostsParCategorie();
        if (data.isEmpty()) {
            pieCategories.getData().add(new PieChart.Data("Aucune donnée", 1));
            return;
        }
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            if (e.getValue() > 0)
                pieCategories.getData().add(
                        new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue())
                );
        }
        pieCategories.setLabelsVisible(true);
        pieCategories.setAnimated(true);
    }

    // ── LineChart — activité 7 derniers jours ─────────────────────────────────
    private void loadLine() {
        linePostsParJour.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Posts");
        Map<String, Integer> data = stats.getPostsParJour();
        if (data.isEmpty()) {
            series.getData().add(new XYChart.Data<>("Auj.", 0));
        } else {
            data.forEach((jour, nb) ->
                    series.getData().add(new XYChart.Data<>(jour, nb)));
        }
        linePostsParJour.getData().add(series);
        linePostsParJour.setAnimated(true);
        linePostsParJour.setCreateSymbols(true);
    }

    // ── BarChart — top 5 auteurs ──────────────────────────────────────────────
    private void loadBarAuteurs() {
        barTopAuteurs.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Posts publiés");
        Map<String, Integer> data = stats.getTopAuteurs();
        if (data.isEmpty()) {
            series.getData().add(new XYChart.Data<>("Aucun", 0));
        } else {
            data.forEach((auteur, nb) ->
                    series.getData().add(new XYChart.Data<>(auteur, nb)));
        }
        barTopAuteurs.getData().add(series);
        barTopAuteurs.setLegendVisible(false);
        barTopAuteurs.setAnimated(true);
    }

    // ── BarChart — commentaires par post ──────────────────────────────────────
    private void loadBarCommentaires() {
        barCommentairesParPost.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Commentaires");
        Map<String, Integer> data = stats.getCommentairesParPost();
        if (data.isEmpty()) {
            series.getData().add(new XYChart.Data<>("Aucun", 0));
        } else {
            data.forEach((titre, nb) ->
                    series.getData().add(new XYChart.Data<>(titre, nb)));
        }
        barCommentairesParPost.getData().add(series);
        barCommentairesParPost.setLegendVisible(false);
        barCommentairesParPost.setAnimated(true);
    }
}
