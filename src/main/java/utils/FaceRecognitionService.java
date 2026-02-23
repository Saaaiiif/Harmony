package utils;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * Service de reconnaissance faciale natif JavaCV/OpenCV.
 *
 * Fonctionnalités :
 *  - Streaming webcam vers JavaFX ImageView (~15 fps)
 *  - Détection de visage par Haar Cascade (haarcascade_frontalface_default.xml)
 *  - Capture + sauvegarde du visage (inscription Step 3)
 *  - Comparaison biométrique par histogramme + corrélation pixel (login)
 *
 * Usage :
 *   FaceRecognitionService svc = new FaceRecognitionService();
 *   svc.startCamera(callback);   // démarre le stream
 *   svc.captureCurrentFace();    // capture le dernier visage détecté
 *   svc.stopCamera();            // libère la caméra
 */
public class FaceRecognitionService {

    // ─── Constantes ──────────────────────────────────────────────────────────
    public  static final String  FACE_DIR           = "face_data/";
    private static final Size    COMPARE_SIZE       = new Size(100, 100);
    private static final double  HIST_THRESHOLD     = 0.38;  // corrélation min (0-1)
    private static final double  PIXEL_THRESHOLD    = 0.30;  // diff max (0-1)
    private static final int     CAMERA_FPS_DELAY   = 66;    // ~15 fps
    private static final int     MIN_FACE_SIZE_PX   = 80;    // visage min détectable

    // ─── Ressources OpenCV ───────────────────────────────────────────────────
    private OpenCVFrameGrabber              grabber;
    private final OpenCVFrameConverter.ToMat frameConverter = new OpenCVFrameConverter.ToMat();
    private CascadeClassifier               faceDetector;
    private ScheduledExecutorService        cameraExecutor;
    private volatile boolean                running = false;

    // Dernier visage (ROI gris 100×100) détecté par le thread caméra
    private final AtomicReference<Mat> lastFaceMat = new AtomicReference<>(null);

    // ─── Callback vers le controller ─────────────────────────────────────────
    public interface FrameCallback {
        /**
         * @param frame       Image RGB prête pour JavaFX ImageView
         * @param faceFound   true si un visage est détecté dans ce frame
         */
        void onFrame(Image frame, boolean faceFound);
    }

    // =========================================================================
    //  INITIALISATION
    // =========================================================================

    public FaceRecognitionService() {
        new File(FACE_DIR).mkdirs();
        loadCascade();
    }

    /**
     * Charge le classificateur Haar depuis les ressources.
     * Le fichier haarcascade_frontalface_default.xml doit être dans
     * src/main/resources/haarcascade_frontalface_default.xml
     */
    private void loadCascade() {
        try {
            InputStream is = getClass().getResourceAsStream("/haarcascade_frontalface_default.xml");
            if (is == null) {
                System.err.println("⚠  haarcascade_frontalface_default.xml introuvable dans resources !");
                return;
            }
            File tempFile = File.createTempFile("haarcascade_frontalface", ".xml");
            tempFile.deleteOnExit();
            Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            faceDetector = new CascadeClassifier(tempFile.getAbsolutePath());
            if (faceDetector.empty()) {
                System.err.println("⚠  Cascade vide — vérifiez le fichier XML.");
                faceDetector = null;
            } else {
                System.out.println("✅ Haar cascade chargée.");
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement cascade : " + e.getMessage());
        }
    }

    // =========================================================================
    //  CAMÉRA — démarrage / arrêt
    // =========================================================================

    /**
     * Démarre le stream caméra et appelle le callback à chaque frame.
     *
     * @param callback Reçoit chaque Image + flag faceFound sur le thread JavaFX
     * @throws Exception si la caméra ne peut pas démarrer
     */
    public void startCamera(FrameCallback callback) throws Exception {
        grabber = new OpenCVFrameGrabber(0);
        grabber.setImageWidth(640);
        grabber.setImageHeight(480);
        grabber.start();
        running = true;

        cameraExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "FaceCapture-Thread");
            t.setDaemon(true);
            return t;
        });

        cameraExecutor.scheduleAtFixedRate(() -> {
            if (!running) return;
            try {
                Frame frame = grabber.grab();
                if (frame == null || frame.image == null) return;

                Mat mat = frameConverter.convert(frame);
                if (mat == null || mat.empty()) return;

                // Effet miroir (plus naturel pour l'utilisateur)
                flip(mat, mat, 1);

                // Conversion en niveaux de gris pour la détection
                Mat gray = new Mat();
                cvtColor(mat, gray, COLOR_BGR2GRAY);
                equalizeHist(gray, gray);

                // ── Détection de visage ──────────────────────────────────────
                RectVector faces = new RectVector();
                boolean faceFound = false;

                if (faceDetector != null && !faceDetector.empty()) {
                    faceDetector.detectMultiScale(
                            gray, faces,
                            1.1,                          // scaleFactor
                            4,                            // minNeighbors
                            0,                            // flags
                            new Size(MIN_FACE_SIZE_PX, MIN_FACE_SIZE_PX),
                            new Size()
                    );
                    faceFound = faces.size() > 0;
                }

                // ── Si visage détecté ────────────────────────────────────────
                if (faceFound) {
                    Rect bestFace = findLargestFace(faces);

                    // Stocker le ROI gris redimensionné pour la capture
                    Mat faceROI  = new Mat(gray, bestFace);
                    Mat faceResized = new Mat();
                    resize(faceROI, faceResized, COMPARE_SIZE);
                    lastFaceMat.set(faceResized.clone());
                    faceROI.release();

                    // Dessiner le rectangle de détection (violet du thème)
                    rectangle(mat,
                            new Point(bestFace.x(), bestFace.y()),
                            new Point(bestFace.x() + bestFace.width(), bestFace.y() + bestFace.height()),
                            new Scalar(246, 92, 139, 255), // BGR : violet #8B5CF6
                            3, LINE_8, 0);
                } else {
                    lastFaceMat.set(null);
                }

                gray.release();

                // ── Conversion Mat → JavaFX Image ────────────────────────────
                Image fxImage = matToFXImage(mat);
                mat.release();

                // ── Mise à jour UI sur le thread JavaFX ──────────────────────
                boolean finalFaceFound = faceFound;
                Platform.runLater(() -> callback.onFrame(fxImage, finalFaceFound));

            } catch (Exception e) {
                // Ignorer les erreurs de frame individuelles (courantes)
            }
        }, 0, CAMERA_FPS_DELAY, TimeUnit.MILLISECONDS);
    }

    /**
     * Stoppe le stream et libère toutes les ressources.
     */
    public void stopCamera() {
        running = false;
        if (cameraExecutor != null) {
            cameraExecutor.shutdownNow();
            cameraExecutor = null;
        }
        if (grabber != null) {
            try { grabber.stop(); grabber.release(); } catch (Exception ignored) {}
            grabber = null;
        }
        lastFaceMat.set(null);
        System.out.println("📷 Caméra arrêtée.");
    }

    // =========================================================================
    //  CAPTURE
    // =========================================================================

    /**
     * Retourne le dernier visage détecté (Mat 100×100 gris) ou null si absent.
     * Thread-safe : peut être appelé depuis le thread JavaFX.
     */
    public Mat captureCurrentFace() {
        Mat face = lastFaceMat.get();
        return face != null ? face.clone() : null;
    }

    /**
     * Sauvegarde un Mat de visage sur disque et retourne le chemin.
     *
     * @param faceMat   Mat retourné par captureCurrentFace()
     * @param userEmail Email de l'utilisateur (utilisé pour nommer le fichier)
     * @return Chemin absolu du fichier sauvegardé
     */
    public String saveFace(Mat faceMat, String userEmail) {
        if (faceMat == null || faceMat.empty()) return null;
        String safeName = userEmail.replaceAll("[^a-zA-Z0-9]", "_");
        String filePath = FACE_DIR + safeName + "_face.png";
        imwrite(filePath, faceMat);
        System.out.println("💾 Visage sauvegardé : " + filePath);
        return filePath;
    }

    // =========================================================================
    //  COMPARAISON BIOMÉTRIQUE
    // =========================================================================

    /**
     * Compare un visage capturé avec le visage enregistré (chemin en BDD).
     *
     * Algorithme : combinaison de
     *   1) Corrélation d'histogramme (distribution des niveaux de gris)
     *   2) Différence absolue normalisée pixel à pixel
     *
     * @param storedFacePath Chemin vers le PNG sauvegardé à l'inscription
     * @param capturedFace   Mat capturé en temps réel (100×100 gris)
     * @return true si les deux visages sont considérés comme identiques
     */
    public boolean compareFaces(String storedFacePath, Mat capturedFace) {
        if (storedFacePath == null || capturedFace == null || capturedFace.empty()) return false;

        Mat storedFace = imread(storedFacePath, IMREAD_GRAYSCALE);
        if (storedFace == null || storedFace.empty()) {
            System.err.println("⚠  Impossible de lire : " + storedFacePath);
            return false;
        }

        // Redimensionner les deux images à la même taille
        Mat ref  = new Mat();
        Mat live = new Mat();
        resize(storedFace, ref,  COMPARE_SIZE);
        resize(capturedFace, live, COMPARE_SIZE);

        // 1. Corrélation d'histogramme ─────────────────────────────────────────
        Mat histRef  = calcGrayHistogram(ref);
        Mat histLive = calcGrayHistogram(live);
        double histScore = compareHist(histRef, histLive, HISTCMP_CORREL);
        // HISTCMP_CORREL : -1 (opposé) à +1 (identique)

        // 2. Différence absolue normalisée ─────────────────────────────────────
        Mat diff = new Mat();
        absdiff(ref, live, diff);
        Scalar meanDiff = mean(diff);
        // ✅ JavaCV : Scalar étend DoublePointer → utiliser .get(index)
        double pixelDiff = meanDiff.get(0) / 255.0;
        // 0.0 = identiques, 1.0 = complètement différents

        // Libérer les ressources
        storedFace.release(); ref.release(); live.release();
        histRef.release(); histLive.release(); diff.release();

        System.out.printf("🔍 Résultat biométrique — histScore=%.3f (seuil %.2f) | pixelDiff=%.3f (seuil %.2f)%n",
                histScore, HIST_THRESHOLD, pixelDiff, PIXEL_THRESHOLD);

        // Décision : les DEUX critères doivent être satisfaits
        return histScore >= HIST_THRESHOLD && pixelDiff <= PIXEL_THRESHOLD;
    }

    // =========================================================================
    //  UTILITAIRES PRIVÉS
    // =========================================================================

    /**
     * Calcule un histogramme normalisé sur 256 bins pour une image en niveaux de gris.
     */
    private Mat calcGrayHistogram(Mat grayMat) {
        MatVector mv = new MatVector(grayMat);
        Mat hist = new Mat();
        int[]   channels    = {0};
        int[]   histSize    = {256};
        float[] ranges      = {0f, 256f};
        calcHist(mv, new IntPointer(channels), new Mat(), hist, new IntPointer(histSize), new FloatPointer(ranges), false);
        normalize(hist, hist, 0, 1, NORM_MINMAX, -1, new Mat());
        return hist;
    }

    /**
     * Parmi les visages détectés, retourne le plus grand (le plus proche de la caméra).
     */
    private Rect findLargestFace(RectVector faces) {
        Rect largest = faces.get(0);
        for (int i = 1; i < faces.size(); i++) {
            Rect r = faces.get(i);
            if (r.width() * r.height() > largest.width() * largest.height()) {
                largest = r;
            }
        }
        return largest;
    }

    /**
     * Convertit un Mat OpenCV BGR en JavaFX WritableImage RGB.
     * Méthode rapide : transfert direct des octets sans passage par PNG/JPEG.
     */
    private Image matToFXImage(Mat mat) {
        try {
            int width    = mat.cols();
            int height   = mat.rows();
            int channels = mat.channels();

            Mat rgbMat = new Mat();
            if (channels == 3) {
                cvtColor(mat, rgbMat, COLOR_BGR2RGB);
            } else if (channels == 1) {
                cvtColor(mat, rgbMat, COLOR_GRAY2RGB);
            } else {
                rgbMat = mat.clone();
            }

            byte[] buffer = new byte[width * height * 3];
            rgbMat.data().get(buffer);
            rgbMat.release();

            WritableImage fxImage = new WritableImage(width, height);
            fxImage.getPixelWriter().setPixels(
                    0, 0, width, height,
                    PixelFormat.getByteRgbInstance(),
                    buffer, 0, width * 3
            );
            return fxImage;
        } catch (Exception e) {
            return null;
        }
    }

    // Classe interne pour IntPointer/FloatPointer (wrappers JavaCV)
    // Note: ces classes sont déjà dans org.bytedeco.javacpp
    private static class IntPointer extends org.bytedeco.javacpp.IntPointer {
        IntPointer(int... values) { super(values); }
    }
    private static class FloatPointer extends org.bytedeco.javacpp.FloatPointer {
        FloatPointer(float... values) { super(values); }
    }
}
