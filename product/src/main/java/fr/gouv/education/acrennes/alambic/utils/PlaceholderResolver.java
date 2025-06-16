package fr.gouv.education.acrennes.alambic.utils;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderResolver {

    private PlaceholderResolver() {
        // hide public constructor, avoid instantiation
    }

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^:{}]+)(?::([^}]*))?}");

    /**
     * Resolves all property values using env vars and optional default values.
     * Property values can be either fixed or placeholders for environment variables like ${MY_VAR} or ${MY_VAR:value}.
     * Unresolved placeholders are left as-is.
     */
    public static Properties resolvePlaceholders(Properties input) {
        Properties resolved = new Properties();

        for (String key : input.stringPropertyNames()) {
            String value = input.getProperty(key);
            if (value != null) {
                String newValue = resolveString(value);
                resolved.setProperty(key, newValue);
            }
        }

        return resolved;
    }

    /**
     * Resolves all placeholders in a string using environment variables and optional default values.
     * Unresolved placeholders are left intact (like Spring does).
     */
    private static String resolveString(String value) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String defaultValue = matcher.group(2);

            String replacement = System.getenv(varName);
            if (replacement == null) {
                replacement = defaultValue != null ? defaultValue : matcher.group(0); // Keep ${VAR} as-is
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }
}
