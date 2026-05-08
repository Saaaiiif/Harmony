import main.MainFX;

public class Launcher {
    public static void main(String[] args) {
        // Contournement bug JavaFX/Glass sur macOS (NSTrackingRectTag crash)
        System.setProperty("glass.disableAsyncResize", "true");
        MainFX.main(args);
    }
}
