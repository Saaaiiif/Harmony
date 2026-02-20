package utils;

import models.NiveauActivitePhysique;
import models.user;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Détecteur de faux comptes basé sur un système de scoring
 * Score 0-2 : Peu suspect (VERT)
 * Score 3-5 : Moyennement suspect (ORANGE)
 * Score 6+ : Très suspect (ROUGE)
 */
public class FakeAccountDetector {

    // Listes de noms suspects
    private static final List<String> SUSPECT_NAMES = Arrays.asList(
        "test", "aaa", "bbb", "ccc", "xxx", "zzz",
        "user", "admin", "fake", "temp", "temporary",
        "demo", "sample", "example", "dummy", "null",
        "undefined", "unknown", "anonymous", "guest",
        "azerty", "qwerty", "asdf", "qsdf"
    );

    // Patterns suspects
    private static final Pattern SUSPECT_NAME_PATTERN = Pattern.compile("^(user|test|admin)\\d+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPETITIVE_CHARS = Pattern.compile("(.)\\1{3,}"); // aaaa, bbbb, etc.
    private static final Pattern RANDOM_NUMBERS = Pattern.compile("\\d{3,}"); // 3+ chiffres consécutifs
    private static final Pattern TEMP_EMAIL_PATTERN = Pattern.compile("@(temp|test|fake|yopmail|guerrillamail|10minutemail)", Pattern.CASE_INSENSITIVE);

    /**
     * Calcule le score de suspicion d'un compte
     * @param u L'utilisateur à analyser
     * @param allUsers Tous les utilisateurs (pour détecter les doublons)
     * @return Score de suspicion (0 = clean, 10+ = très suspect)
     */
    public static int calculateSuspicionScore(user u, List<user> allUsers) {
        int score = 0;

        // 1. Vérifier le nom (max +3 points)
        score += checkSuspiciousName(u.getUser_nom());
        
        // 2. Vérifier le prénom (max +3 points)
        score += checkSuspiciousName(u.getUser_prenom());
        
        // 3. Vérifier l'email (max +4 points)
        score += checkSuspiciousEmail(u.getUser_email());
        
        // 4. Vérifier l'heure d'inscription (max +2 points)
        score += checkSuspiciousRegistrationTime(u.getDate_inscription());
        
        // 5. Vérifier les doublons (max +3 points)
        score += checkDuplicates(u, allUsers);
        
        // 6. Vérifier incohérence âge/activité (max +3 points)
        score += checkAgeActivityIncoherence(u);
        
        // 7. Vérifier établissement suspect (max +2 points)
        score += checkSuspiciousEstablishment(u.getUser_etablissement_scolaire());
        
        // 8. Vérifier poids/taille incohérents (max +2 points)
        score += checkPhysicalIncoherence(u);

        return score;
    }

    /**
     * Retourne le niveau de suspicion
     * @param score Score calculé
     * @return "LOW", "MEDIUM", "HIGH"
     */
    public static String getSuspicionLevel(int score) {
        if (score <= 2) return "LOW";
        if (score <= 5) return "MEDIUM";
        return "HIGH";
    }

    /**
     * Retourne la couleur associée au niveau de suspicion
     */
    public static String getSuspicionColor(int score) {
        String level = getSuspicionLevel(score);
        switch (level) {
            case "LOW": return "#10B981"; // Vert
            case "MEDIUM": return "#F59E0B"; // Orange
            case "HIGH": return "#EF4444"; // Rouge
            default: return "#6B7280"; // Gris par défaut
        }
    }

    /**
     * Retourne le label de suspicion
     */
    public static String getSuspicionLabel(int score) {
        String level = getSuspicionLevel(score);
        switch (level) {
            case "LOW": return "✓ Fiable";
            case "MEDIUM": return "⚠ Suspect";
            case "HIGH": return "⛔ Très suspect";
            default: return "";
        }
    }

    // ==================== MÉTHODES DE DÉTECTION ====================

    private static int checkSuspiciousName(String name) {
        if (name == null || name.trim().isEmpty()) return 1;
        
        String nameLower = name.trim().toLowerCase();
        int suspicionPoints = 0;

        // Nom dans la liste suspecte
        if (SUSPECT_NAMES.contains(nameLower)) {
            suspicionPoints += 2;
        }

        // Pattern user123, test456, etc.
        if (SUSPECT_NAME_PATTERN.matcher(nameLower).matches()) {
            suspicionPoints += 2;
        }

        // Caractères répétitifs (aaaa, bbbb)
        if (REPETITIVE_CHARS.matcher(nameLower).find()) {
            suspicionPoints += 1;
        }

        // Nom trop court (1 caractère)
        if (name.trim().length() == 1) {
            suspicionPoints += 1;
        }

        return Math.min(suspicionPoints, 3); // Max 3 points
    }

    private static int checkSuspiciousEmail(String email) {
        if (email == null || email.trim().isEmpty()) return 2;
        
        String emailLower = email.toLowerCase();
        int suspicionPoints = 0;

        // Email temporaire
        if (TEMP_EMAIL_PATTERN.matcher(emailLower).find()) {
            suspicionPoints += 3;
        }

        // Beaucoup de chiffres dans l'email (ex: user12345678@test.com)
        long digitCount = emailLower.chars().filter(Character::isDigit).count();
        if (digitCount >= 6) {
            suspicionPoints += 2;
        }

        // Email avec pattern suspect (randomXXXXX@)
        if (RANDOM_NUMBERS.matcher(emailLower.split("@")[0]).find()) {
            suspicionPoints += 1;
        }

        return Math.min(suspicionPoints, 4); // Max 4 points
    }

    private static int checkSuspiciousRegistrationTime(String dateInscription) {
        try {
            // Note: On ne peut vérifier l'heure que si elle est stockée
            // Si vous stockez uniquement la date, cette vérification ne s'applique pas
            // Pour l'instant, on suppose que les inscriptions suspectes pourraient être détectées autrement
            
            LocalDate inscriptionDate = LocalDate.parse(dateInscription);
            LocalDate now = LocalDate.now();
            
            // Inscription le jour même = peut-être suspect si combiné avec autres signaux
            if (inscriptionDate.equals(now)) {
                return 1;
            }
            
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int checkDuplicates(user current, List<user> allUsers) {
        if (allUsers == null || allUsers.isEmpty()) return 0;
        
        int suspicionPoints = 0;
        
        for (user other : allUsers) {
            // Ne pas se comparer à soi-même
            if (other.getUser_id() == current.getUser_id()) continue;
            
            // Email exactement identique (ne devrait pas arriver avec contrainte UNIQUE)
            if (current.getUser_email().equalsIgnoreCase(other.getUser_email())) {
                suspicionPoints += 3;
            }
            
            // Nom ET prénom identiques
            if (current.getUser_nom().equalsIgnoreCase(other.getUser_nom()) &&
                current.getUser_prenom().equalsIgnoreCase(other.getUser_prenom())) {
                suspicionPoints += 2;
            }
            
            // Email très similaire (même préfixe)
            String currentEmailPrefix = current.getUser_email().split("@")[0];
            String otherEmailPrefix = other.getUser_email().split("@")[0];
            if (currentEmailPrefix.equalsIgnoreCase(otherEmailPrefix)) {
                suspicionPoints += 1;
            }
        }
        
        return Math.min(suspicionPoints, 3); // Max 3 points
    }

    private static int checkAgeActivityIncoherence(user u) {
        if (u.getUser_date_de_naissance() == null || u.getUser_niveau_activite_physique() == null) {
            return 0;
        }
        
        try {
            LocalDate birthDate = LocalDate.parse(u.getUser_date_de_naissance());
            int age = Period.between(birthDate, LocalDate.now()).getYears();
            
            NiveauActivitePhysique niveau = u.getUser_niveau_activite_physique();
            
            // Enfant (< 15 ans) avec activité très intense
            if (age < 15 && (niveau == NiveauActivitePhysique.TRES_INTENSE)) {
                return 2;
            }
            
            // Personne âgée (> 70 ans) avec activité très intense
            if (age > 70 && niveau == NiveauActivitePhysique.TRES_INTENSE) {
                return 2;
            }
            
            // Âge invalide (> 120 ans ou < 13 ans)
            if (age > 120 || age < 13) {
                return 3;
            }
            
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int checkSuspiciousEstablishment(String etablissement) {
        if (etablissement == null || etablissement.trim().isEmpty()) {
            return 0; // Peut être null, pas forcément suspect
        }
        
        String etablissementLower = etablissement.toLowerCase().trim();
        
        // Établissement suspect
        if (SUSPECT_NAMES.contains(etablissementLower)) {
            return 2;
        }
        
        // Établissement trop court
        if (etablissement.trim().length() <= 2) {
            return 1;
        }
        
        return 0;
    }

    private static int checkPhysicalIncoherence(user u) {
        if (u.getUser_poids() == null || u.getUser_taille() == null) {
            return 0;
        }
        
        Double poids = u.getUser_poids();
        Integer taille = u.getUser_taille();
        
        // Valeurs extrêmes
        if (poids < 30 || poids > 200) {
            return 1;
        }
        
        if (taille < 120 || taille > 220) {
            return 1;
        }
        
        // IMC extrême (très maigre ou très obèse peut indiquer des données fake)
        double imc = poids / Math.pow(taille / 100.0, 2);
        if (imc < 15 || imc > 40) {
            return 1;
        }
        
        return 0;
    }
}
