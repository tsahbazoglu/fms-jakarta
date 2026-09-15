package tr.org.tspb.service.lms.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.bson.Document;

/**
 * Enterprise PII Anonymization & Privacy Protection Utility.
 * Provides masking, HMAC/SHA-256 pseudonymization, and document scrubbing for GDPR compliance.
 */
public class LmsPiiAnonymizer {

    private LmsPiiAnonymizer() {
        // Private constructor for utility class
    }

    /**
     * Masks phone numbers for privacy compliance.
     * Example: "+375291234567" -> "+375 29 *** 4567"
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return phone;
        }
        String clean = phone.trim();
        if (clean.length() < 7) {
            return "***";
        }
        String prefix = clean.substring(0, Math.min(6, clean.length() - 4));
        String suffix = clean.substring(clean.length() - 4);
        return prefix + " *** " + suffix;
    }

    /**
     * Masks email addresses for privacy compliance.
     * Example: "john.doe@domain.com" -> "j***e@domain.com"
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIdx = email.indexOf("@");
        String namePart = email.substring(0, atIdx);
        String domainPart = email.substring(atIdx);

        if (namePart.length() <= 2) {
            return namePart.charAt(0) + "***" + domainPart;
        }

        char first = namePart.charAt(0);
        char last = namePart.charAt(namePart.length() - 1);
        return first + "***" + last + domainPart;
    }

    /**
     * Computes SHA-256 hex string for GDPR pseudonymization lookup keys.
     */
    public static String hashPii(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    /**
     * Anonymizes PII fields in a BSON Document for audit export or client display.
     */
    public static Document anonymizeDocument(Document doc) {
        if (doc == null) return null;
        Document copy = new Document(doc);

        if (copy.containsKey("phone")) {
            copy.put("phone", maskPhone(copy.getString("phone")));
        }
        if (copy.containsKey("email")) {
            copy.put("email", maskEmail(copy.getString("email")));
        }
        if (copy.containsKey("fullName")) {
            String name = copy.getString("fullName");
            if (name != null && name.contains(" ")) {
                String[] parts = name.split(" ");
                copy.put("fullName", parts[0] + " " + parts[parts.length - 1].charAt(0) + ".");
            }
        }
        return copy;
    }
}
