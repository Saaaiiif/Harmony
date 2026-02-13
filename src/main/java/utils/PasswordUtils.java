package utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {

    // Hacher un mot de passe
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12)); // 12 = force de hachage
    }

    // Vérifier un mot de passe par rapport à son hash
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}