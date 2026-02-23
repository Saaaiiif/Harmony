package utils;

import java.util.regex.Pattern;

public class ValidationUtils {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"
    );
    
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$"
    );
    
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    public static boolean isValidEmail(String email) {
        if (isNullOrEmpty(email)) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    public static boolean isValidPassword(String password) {
        if (isNullOrEmpty(password)) return false;
        return PASSWORD_PATTERN.matcher(password).matches();
    }
    
    public static boolean isValidUrl(String url) {
        if (isNullOrEmpty(url)) return true;
        return URL_PATTERN.matcher(url.trim()).matches();
    }
    
    public static boolean isValidLength(String str, int min, int max) {
        if (str == null) return min == 0;
        int length = str.trim().length();
        return length >= min && length <= max;
    }
    
    public static boolean isPositiveNumber(int num) {
        return num > 0;
    }
    
    public static boolean isInRange(int num, int min, int max) {
        return num >= min && num <= max;
    }
    
    public static String getEmailError(String email) {
        if (isNullOrEmpty(email)) return "L'email est obligatoire";
        if (!isValidEmail(email)) return "Format d'email invalide";
        return null;
    }
    
    public static String getPasswordError(String password) {
        if (isNullOrEmpty(password)) return "Le mot de passe est obligatoire";
        if (password.length() < 8) return "Le mot de passe doit contenir au moins 8 caractères";
        if (!password.matches(".*[A-Z].*")) return "Le mot de passe doit contenir au moins une majuscule";
        if (!password.matches(".*[a-z].*")) return "Le mot de passe doit contenir au moins une minuscule";
        if (!password.matches(".*\\d.*")) return "Le mot de passe doit contenir au moins un chiffre";
        return null;
    }
    
    public static String getRequiredFieldError(String value, String fieldName) {
        if (isNullOrEmpty(value)) return fieldName + " est obligatoire";
        return null;
    }
    
    public static String getLengthError(String value, String fieldName, int min, int max) {
        if (value == null) value = "";
        int length = value.trim().length();
        if (length < min) return fieldName + " doit contenir au moins " + min + " caractères";
        if (length > max) return fieldName + " ne doit pas dépasser " + max + " caractères";
        return null;
    }
    
    public static String getPositiveNumberError(String value, String fieldName) {
        if (isNullOrEmpty(value)) return fieldName + " est obligatoire";
        try {
            int num = Integer.parseInt(value.trim());
            if (num <= 0) return fieldName + " doit être un nombre positif";
        } catch (NumberFormatException e) {
            return fieldName + " doit être un nombre valide";
        }
        return null;
    }
    
    public static String getUrlError(String url) {
        if (isNullOrEmpty(url)) return "L'URL est obligatoire";
        if (!isValidUrl(url)) return "Format d'URL invalide";
        return null;
    }
}
