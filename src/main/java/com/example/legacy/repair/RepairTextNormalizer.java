package com.example.legacy.repair;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class RepairTextNormalizer {

    private static final Pattern BULLET_PREFIX =
            Pattern.compile("^\\s*(?:[-*]+|\\d+[.)]|[가-힣][.)])\\s*");
    private static final String ACTION_NAME_PATTERN =
            "1/2\\s*OH|1/3\\s*OH|1/4\\s*OH|오버홀|수리|조정|도장|탈착|교환|판금";
    private static final Pattern TRAILING_PAREN_ACTION =
            Pattern.compile("\\s*\\((" + ACTION_NAME_PATTERN + ")\\)\\s*$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern TRAILING_ACTION =
            Pattern.compile("\\s*(" + ACTION_NAME_PATTERN + ")\\s*$",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private RepairTextNormalizer() {
    }

    public static List<String> splitItems(String input) {
        List<String> items = new ArrayList<String>();
        if (input == null) {
            return items;
        }

        String normalized = input.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\\n+");
        boolean splitInlineDelimiters = lines.length == 1;

        for (String line : lines) {
            collectLineItems(items, line, splitInlineDelimiters);
        }
        return items;
    }

    private static void collectLineItems(List<String> items, String line, boolean splitInlineDelimiters) {
        if (line == null) {
            return;
        }

        String trimmed = line.trim();
        if (trimmed.length() == 0 || "```".equals(trimmed)) {
            return;
        }

        if (splitInlineDelimiters) {
            String[] fragments = trimmed.split("[,;]+");
            for (String fragment : fragments) {
                addCleanItem(items, fragment);
            }
            return;
        }

        addCleanItem(items, trimmed);
    }

    private static void addCleanItem(List<String> items, String value) {
        String cleaned = cleanText(BULLET_PREFIX.matcher(value).replaceFirst(""));
        if (cleaned.length() > 0) {
            items.add(cleaned);
        }
    }

    public static String cleanText(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.trim();
        cleaned = cleaned.replaceAll("\\(\\s*\\)$", "");
        cleaned = cleaned.replaceAll("\\s+", " ");
        return cleaned;
    }

    public static String normalizeKey(String value) {
        String normalized = cleanText(value).toUpperCase(Locale.ROOT);
        normalized = normalized.replaceAll("[\"']", "");
        normalized = normalized.replaceAll("\\s+", "");
        normalized = normalized.replaceAll("[\\|\\-_·ㆍ]", "");
        normalized = normalized.replace('（', '(').replace('）', ')');
        return normalized;
    }

    public static String stripTrailingAction(String value) {
        String stripped = cleanText(value);
        String previous;
        do {
            previous = stripped;
            stripped = TRAILING_PAREN_ACTION.matcher(stripped).replaceFirst("");
            stripped = TRAILING_ACTION.matcher(stripped).replaceFirst("");
            stripped = cleanText(stripped);
        } while (stripped.length() > 0 && !stripped.equals(previous));

        return stripped.length() == 0 ? cleanText(value) : stripped;
    }

    public static String extractTrailingActionName(String value) {
        String cleaned = cleanText(value);
        java.util.regex.Matcher parenMatcher = TRAILING_PAREN_ACTION.matcher(cleaned);
        if (parenMatcher.find()) {
            return parenMatcher.group(1);
        }

        java.util.regex.Matcher plainMatcher = TRAILING_ACTION.matcher(cleaned);
        if (plainMatcher.find()) {
            return plainMatcher.group(1);
        }

        return "";
    }

    public static List<String> lookupKeys(String value) {
        List<String> keys = new ArrayList<String>();
        addKey(keys, normalizeKey(value));
        String stripped = stripTrailingAction(value);
        if (!cleanText(value).equals(stripped)) {
            addKey(keys, normalizeKey(stripped));
        }
        return keys;
    }

    private static void addKey(List<String> keys, String key) {
        if (key.length() > 0 && !keys.contains(key)) {
            keys.add(key);
        }
    }
}
