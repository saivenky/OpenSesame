package saivenky.opensesame;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

/**
 * Created by saivenky on 2/25/18.
 */

public class PasswordGenerator {
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMBERS = "0123456789";
    private static final String SYMBOLS = "!@#$%&*()?=";
    private static final int PASS_LENGTH = 10;

    public String generate(
        boolean hasLowercase,
        boolean hasUppercase,
        boolean hasNumbers,
        boolean hasSymbols,
        String tag,
        String passphrase) {
        int[] seed = generateSeed(hasLowercase, hasUppercase, hasNumbers, hasSymbols, tag, passphrase);
        StringBuilder passwordBuilder = buildPassword(seed, hasLowercase, hasUppercase, hasNumbers, hasSymbols);
        String generatedPassword = correctGeneratedPassword(passwordBuilder, seed, hasLowercase, hasUppercase, hasNumbers, hasSymbols);
        return generatedPassword;
    }

    private int[] generateSeed(
        boolean hasLowercase, boolean hasUppercase, boolean hasNumbers, boolean hasSymbols,
        String tag, String passphrase) {
        String usage = "";
        usage += hasLowercase ? "1" : "0";
        usage += hasUppercase ? "1" : "0";
        usage += hasNumbers ? "1" : "0";
        usage += hasSymbols ? "1" : "0";

        String output = encode(usage) + "+" + encode(tag) + "+" + encode(passphrase);
        return unsigned(toSHA1(output));
    }

    private StringBuilder buildPassword(
        int[] seed, boolean hasLowercase, boolean hasUppercase, boolean hasNumbers, boolean hasSymbols) {
        String fullSet = "";
        fullSet += hasLowercase ? LOWERCASE : "";
        fullSet += hasUppercase ? UPPERCASE : "";
        fullSet += hasNumbers ? NUMBERS : "";
        fullSet += hasSymbols ? SYMBOLS : "";

        StringBuilder passwordBuilder = new StringBuilder();
        for (int i = 0; i < PASS_LENGTH; i++) {
            passwordBuilder.append(fullSet.charAt(seed[i] % fullSet.length()));
        }
        passwordBuilder.append(" ");
        return passwordBuilder;
    }

    private String correctGeneratedPassword(
            StringBuilder passwordBuilder, int[] seed,
            boolean hasLowercase, boolean hasUppercase, boolean hasNumbers, boolean hasSymbols) {
        int setNumber = 0;
        if (hasLowercase) {
            if (!Pattern.matches(".*?[" + LOWERCASE + "].*?", passwordBuilder)) {
                passwordBuilder.setCharAt((setNumber + 2) * 2, LOWERCASE.charAt(seed[PASS_LENGTH + setNumber] % LOWERCASE.length()));
            }
            setNumber += 1;
        }
        if (hasUppercase) {
            if (!Pattern.matches(".*?[" + UPPERCASE + "].*?", passwordBuilder)) {
                passwordBuilder.setCharAt((setNumber + 2) * 2, UPPERCASE.charAt(seed[PASS_LENGTH + setNumber] % UPPERCASE.length()));
            }
            setNumber += 1;
        }
        if (hasNumbers) {
            if (!Pattern.matches(".*?[" + NUMBERS + "].*?", passwordBuilder)) {
                passwordBuilder.setCharAt((setNumber + 2) * 2, NUMBERS.charAt(seed[PASS_LENGTH + setNumber] % NUMBERS.length()));
            }
            setNumber += 1;
        }
        if (hasSymbols) {
            if (!Pattern.matches(".*?[" + SYMBOLS + "].*?", passwordBuilder)) {
                passwordBuilder.setCharAt((setNumber + 2) * 2, SYMBOLS.charAt(seed[PASS_LENGTH + setNumber] % SYMBOLS.length()));
            }
        }

        return passwordBuilder.substring(0, PASS_LENGTH);
    }

    private String encode(String toEncode) {
        byte[] digest = toSHA1(toEncode);
        String digestAsString = Charset.forName("ISO-8859-1").decode(ByteBuffer.wrap(digest)).toString();
        return digestAsString;
    }

    public static byte[] toSHA1(String toHash) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return md.digest(toHash.getBytes());
    }
    public static int[] unsigned(byte[] byteArray) {
        int[] result = new int[byteArray.length];
        for (int i = 0; i < byteArray.length; i++) {
            result[i] = byteArray[i] & 0xff;
        }
        return result;
    }
}
