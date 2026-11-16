
package backend;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing using PBKDF2.
 * Stored format: PBKDF2$iterations$salt$hash
 */
public class PasswordManager {

    private static final int ITERATIONS = 65536;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;

    public static String hashPassword(String password) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);

        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS);

        return "PBKDF2$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verifyPassword(String password, String stored) {
        try {
            String[] parts = stored.split("\\$");

            if (parts.length != 4 || !parts[0].equals("PBKDF2")) {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);

            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations);

            return MessageDigest.isEqual(expected, actual);

        } catch (RuntimeException e) {
            return false;
        }
    }

    public static boolean isStrongEnough(String password) {
        return password != null
                && password.length() >= 8
                && password.matches(".*[A-Za-z].*")
                && password.matches(".*\\d.*");
    }

    private static byte[] pbkdf2(
            char[] password, byte[] salt, int iterations) {

        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password, salt, iterations, KEY_BITS);

            return SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Password hashing failed", e);
        }
    }
}
