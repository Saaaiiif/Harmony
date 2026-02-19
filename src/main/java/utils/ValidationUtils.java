package utils;

import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

public class ValidationUtils {

    // ================ VALIDATION NOM/PRÉNOM ================
    
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return name.trim().length() >= 2 && Pattern.matches("^[a-zA-ZÀ-ÿ\\s'-]+$", name.trim());
    }

    public static String getNameErrorMessage(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Ce champ est obligatoire";
        }
        if (name.trim().length() < 2) {
            return "Minimum 2 caractères requis";
        }
        if (!Pattern.matches("^[a-zA-ZÀ-ÿ\\s'-]+$", name.trim())) {
            return "Lettres uniquement (pas de chiffres)";
        }
        return "";
    }

    // ================ VALIDATION EMAIL ================
    
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.matches(emailRegex, email.trim());
    }

    public static String getEmailErrorMessage(String email) {
        if (email == null || email.trim().isEmpty()) {
            return "L'email est obligatoire";
        }
        if (!isValidEmail(email)) {
            return "Format d'email invalide";
        }
        return "";
    }

    // ================ VALIDATION MOT DE PASSE ================
    
    public static boolean isValidPassword(String password) {
        return getPasswordStrength(password) >= 2;
    }

    public static int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;
        
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (Pattern.compile("[a-z]").matcher(password).find()) score++;
        if (Pattern.compile("[A-Z]").matcher(password).find()) score++;
        if (Pattern.compile("[0-9]").matcher(password).find()) score++;
        if (Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]").matcher(password).find()) score++;

        if (score <= 2) return 0;
        if (score <= 4) return 1;
        return 2;
    }

    public static String getPasswordStrengthLabel(int strength) {
        switch (strength) {
            case 0: return "Faible";
            case 1: return "Moyen";
            case 2: return "Fort";
            default: return "Aucun";
        }
    }

    public static String getPasswordStrengthColor(int strength) {
        switch (strength) {
            case 0: return "#EF4444";
            case 1: return "#F59E0B";
            case 2: return "#10B981";
            default: return "#D1D5DB";
        }
    }

    public static String getPasswordErrorMessage(String password) {
        if (password == null || password.isEmpty()) {
            return "Le mot de passe est obligatoire";
        }
        if (password.length() < 8) {
            return "Minimum 8 caractères requis";
        }
        if (!Pattern.compile("[a-z]").matcher(password).find()) {
            return "Au moins une minuscule requise";
        }
        if (!Pattern.compile("[A-Z]").matcher(password).find()) {
            return "Au moins une majuscule requise";
        }
        if (!Pattern.compile("[0-9]").matcher(password).find()) {
            return "Au moins un chiffre requis";
        }
        if (getPasswordStrength(password) < 2) {
            return "Mot de passe trop faible (ajoutez des caractères spéciaux)";
        }
        return "";
    }

    // ================ VALIDATION DATE DE NAISSANCE ================
    
    public static boolean isValidBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            return false;
        }
        
        LocalDate now = LocalDate.now();
        
        if (birthDate.isAfter(now)) {
            return false;
        }
        
        int age = Period.between(birthDate, now).getYears();
        return age >= 13;
    }

    public static String getBirthDateErrorMessage(LocalDate birthDate) {
        if (birthDate == null) {
            return "La date de naissance est obligatoire";
        }
        
        LocalDate now = LocalDate.now();
        
        if (birthDate.isAfter(now)) {
            return "La date ne peut pas être dans le futur";
        }
        
        int age = Period.between(birthDate, now).getYears();
        if (age < 13) {
            return "Vous devez avoir au moins 13 ans";
        }
        
        if (age > 120) {
            return "Veuillez vérifier la date saisie";
        }
        
        return "";
    }

    // ================ VALIDATION POIDS (NOUVEAU) ================
    
    public static boolean isValidPoids(Double poids) {
        if (poids == null) {
            return false; // Obligatoire
        }
        return poids >= 20.0 && poids <= 300.0;
    }

    public static String getPoidsErrorMessage(Double poids) {
        if (poids == null) {
            return "Le poids est obligatoire";
        }
        if (poids < 20.0) {
            return "Le poids minimum est 20 kg";
        }
        if (poids > 300.0) {
            return "Le poids maximum est 300 kg";
        }
        return "";
    }

    // ================ VALIDATION TAILLE (NOUVEAU) ================
    
    public static boolean isValidTaille(Integer taille) {
        if (taille == null) {
            return false; // Obligatoire
        }
        return taille >= 50 && taille <= 250;
    }

    public static String getTailleErrorMessage(Integer taille) {
        if (taille == null) {
            return "La taille est obligatoire";
        }
        if (taille < 50) {
            return "La taille minimum est 50 cm";
        }
        if (taille > 250) {
            return "La taille maximum est 250 cm";
        }
        return "";
    }

    // ================ VALIDATION ÉTABLISSEMENT (NOUVEAU) ================
    
    public static boolean isValidEtablissement(String etablissement) {
        if (etablissement == null || etablissement.trim().isEmpty()) {
            return false; // Obligatoire
        }
        
        String trimmed = etablissement.trim();
        
        // Minimum 2 caractères, maximum 255
        if (trimmed.length() < 2 || trimmed.length() > 255) {
            return false;
        }
        
        // Lettres, chiffres, espaces, tirets, apostrophes autorisés
        return Pattern.matches("^[a-zA-Z0-9À-ÿ\\s'\\-]+$", trimmed);
    }

    public static String getEtablissementErrorMessage(String etablissement) {
        if (etablissement == null || etablissement.trim().isEmpty()) {
            return "L'établissement est obligatoire";
        }
        
        String trimmed = etablissement.trim();
        
        if (trimmed.length() < 2) {
            return "Minimum 2 caractères requis";
        }
        if (trimmed.length() > 255) {
            return "Maximum 255 caractères";
        }
        if (!Pattern.matches("^[a-zA-Z0-9À-ÿ\\s'\\-]+$", trimmed)) {
            return "Caractères invalides détectés";
        }
        
        return "";
    }

    // ================ VALIDATION GLOBALE ================
    
    public static boolean isValidRegistrationForm(String nom, String prenom, String email, 
                                                    String password, LocalDate birthDate) {
        return isValidName(nom) && 
               isValidName(prenom) && 
               isValidEmail(email) && 
               isValidPassword(password) && 
               isValidBirthDate(birthDate);
    }
    
    // Validation complète (étape 1 + étape 2)
    public static boolean isValidCompleteRegistration(String nom, String prenom, String email, 
                                                       String password, LocalDate birthDate,
                                                       Double poids, Integer taille, String etablissement) {
        return isValidRegistrationForm(nom, prenom, email, password, birthDate) &&
               isValidPoids(poids) &&
               isValidTaille(taille) &&
               isValidEtablissement(etablissement);
    }
}
