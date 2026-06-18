package tr.org.tspb.util.tools;

import org.bson.Document;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MongoPlaceholderUtil {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("^\\{\\$(.+?):\\{\\{(.+?)\\}\\}\\}$");

    private MongoPlaceholderUtil() {
    }

    public static void resolve(Object target, Document sourceDoc) {
        if (sourceDoc == null || target == null) return;

        if (target instanceof Document) {
            Document doc = (Document) target;
            for (String key : doc.keySet()) {
                Object value = doc.get(key);
                if (value instanceof String) {
                    doc.put(key, resolveValue((String) value, sourceDoc));
                } else if (value instanceof Document || value instanceof List) {
                    resolve(value, sourceDoc);
                }
            }
        } else if (target instanceof List) {
            List<Object> list = (List<Object>) target;
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item instanceof String) {
                    list.set(i, resolveValue((String) item, sourceDoc));
                } else if (item instanceof Document || item instanceof List) {
                    resolve(item, sourceDoc);
                }
            }
        }
    }

    /**
     * Determines if we should return a raw Object (ObjectId, Long, etc.)
     * or a concatenated String.
     */
    private static Object resolveValue(String text, Document sourceDoc) {
        if (text == null) return null;
        // --- NEW: Check for type casting functions like {$toInt:{{key}}} ---
        Matcher funcMatcher = FUNCTION_PATTERN.matcher(text);
        if (funcMatcher.matches()) {
            String function = funcMatcher.group(1); // e.g., "toInt"
            String key = funcMatcher.group(2);      // e.g., "moderator_password"
            Object rawValue = sourceDoc.get(key);

            if (rawValue == null) return null;

            // Apply type conversion based on the function name
            switch (function) {
                case "toInt":
                    if (rawValue instanceof Number) return ((Number) rawValue).intValue();
                    return Integer.parseInt(rawValue.toString().trim());

                case "toLong":
                    if (rawValue instanceof Number) return ((Number) rawValue).longValue();
                    return Long.parseLong(rawValue.toString().trim());

                case "toObjectId":
                    return new org.bson.types.ObjectId(rawValue.toString().trim());

                default:
                    return rawValue; // Fallback if function is unknown
            }
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);

        // 1. Check if the string is EXACTLY one placeholder: "{{key}}"
        if (text.startsWith("{{") && text.endsWith("}}") && text.indexOf("}}") == text.length() - 2) {
            matcher.find();
            String key = matcher.group(1);
            Object rawValue = sourceDoc.get(key);
            return (rawValue != null) ? rawValue : text;
        }

        // 2. Otherwise, treat it as a String interpolation (concatenation)
        StringBuilder sb = new StringBuilder();
        boolean found = false;
        matcher.reset();

        while (matcher.find()) {
            found = true;
            String key = matcher.group(1);
            Object replacementObj = sourceDoc.get(key);

            if (replacementObj != null) {
                // Here, we have no choice but to use String for concatenation
                matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(replacementObj)));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }

        if (!found) return text;
        matcher.appendTail(sb);
        return sb.toString();
    }
}