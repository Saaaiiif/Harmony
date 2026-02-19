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
        // Regex email standard
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
        return getPasswordStrength(password) >= 2; // Au moins "Moyen"
    }


    public static int getPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }

        int score = 0;
        
        // Critère 1 : Longueur >= 8
        if (password.length() >= 8) score++;
        
        // Critère 2 : Longueur >= 12
        if (password.length() >= 12) score++;
        
        // Critère 3 : Contient au moins une minuscule
        if (Pattern.compile("[a-z]").matcher(password).find()) score++;
        
        // Critère 4 : Contient au moins une majuscule
        if (Pattern.compile("[A-Z]").matcher(password).find()) score++;
        
        // Critère 5 : Contient au moins un chiffre
        if (Pattern.compile("[0-9]").matcher(password).find()) score++;
        
        // Critère 6 : Contient au moins un caractère spécial
        if (Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]").matcher(password).find()) score++;

        // Classification :
        // 0-2 points = Faible (0)
        // 3-4 points = Moyen (1)
        // 5-6 points = Fort (2)
        if (score <= 2) return 0; // Faible
        if (score <= 4) return 1; // Moyen
        return 2; // Fort
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
            case 0: return "#EF4444"; // Rouge
            case 1: return "#F59E0B"; // Orange
            case 2: return "#10B981"; // Vert
            default: return "#D1D5DB"; // Gris
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

    public static String getPasswordRequirements() {
        return "• Min 8 caractères\n" +
               "• Au moins 1 majuscule\n" +
               "• Au moins 1 minuscule\n" +
               "• Au moins 1 chiffre\n" +
               "• Caractères spéciaux recommandés";
    }

    // ================ VALIDATION DATE DE NAISSANCE ================
    
    public static boolean isValidBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            return false;
        }
        
        LocalDate now = LocalDate.now();
        
        // La date ne doit pas être dans le futur
        if (birthDate.isAfter(now)) {
            return false;
        }
        
        // L'utilisateur doit avoir au moins 13 ans
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

    // ================ VALIDATION GLOBALE ================
    
    public static boolean isValidRegistrationForm(String nom, String prenom, String email, 
                                                    String password, LocalDate birthDate) {
        return isValidName(nom) && 
               isValidName(prenom) && 
               isValidEmail(email) && 
               isValidPassword(password) && 
               isValidBirthDate(birthDate);
    }
}
