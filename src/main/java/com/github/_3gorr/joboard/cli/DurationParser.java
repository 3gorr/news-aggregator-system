package com.github._3gorr.joboard.cli;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {

    private static final Pattern PATTERN = Pattern.compile("(\\d+)([smhd])", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    public static Duration parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("interval is required");
        }
        String trimmed = text.trim().toLowerCase();
        Matcher m = PATTERN.matcher(trimmed);
        Duration total = Duration.ZERO;
        int pos = 0;
        boolean matched = false;
        while (m.find()) {
            if (m.start() != pos) {
                throw new IllegalArgumentException("Bad interval format: " + text);
            }
            long value = Long.parseLong(m.group(1));
            String unit = m.group(2);
            total = total.plus(switch (unit) {
                case "s" -> Duration.ofSeconds(value);
                case "m" -> Duration.ofMinutes(value);
                case "h" -> Duration.ofHours(value);
                case "d" -> Duration.ofDays(value);
                default -> throw new IllegalArgumentException("Unknown unit: " + unit);
            });
            pos = m.end();
            matched = true;
        }
        if (!matched || pos != trimmed.length()) {
            throw new IllegalArgumentException("Bad interval format: " + text
                    + " (expected e.g. 30s, 5m, 1h, 1h30m)");
        }
        return total;
    }
}
