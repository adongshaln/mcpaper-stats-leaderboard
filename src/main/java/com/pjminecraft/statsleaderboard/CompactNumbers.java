package com.pjminecraft.statsleaderboard;

import java.util.Locale;

final class CompactNumbers {
    private CompactNumbers() {
    }

    static String format(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        double abs = Math.abs(value);
        if (abs < 1000.0D) {
            if (Math.abs(value - Math.rint(value)) < 0.05D) {
                return Long.toString(Math.round(value));
            }
            return String.format(Locale.ROOT, "%.1f", value);
        }
        double scaled;
        String suffix;
        if (abs < 1_000_000.0D) {
            scaled = value / 1_000.0D;
            suffix = "k";
        } else if (abs < 1_000_000_000.0D) {
            scaled = value / 1_000_000.0D;
            suffix = "m";
        } else {
            scaled = value / 1_000_000_000.0D;
            suffix = "b";
        }
        double magnitude = Math.abs(scaled);
        if (magnitude >= 10.0D || Math.abs(scaled - Math.rint(scaled)) < 0.05D) {
            return Math.round(scaled) + suffix;
        }
        return String.format(Locale.ROOT, "%.1f%s", scaled, suffix);
    }

    static int width(String text) {
        int width = 0;
        for (int index = 0; index < text.length(); index++) {
            width += advance(text.charAt(index));
        }
        return width;
    }

    static String padRight(String text, int pixels) {
        StringBuilder builder = new StringBuilder(text);
        while (width(builder.toString()) + 4 <= pixels) {
            builder.append(' ');
        }
        return builder.toString();
    }

    static String padLeft(String text, int pixels) {
        StringBuilder builder = new StringBuilder();
        int filled = width(text);
        while (filled + 4 <= pixels) {
            builder.append(' ');
            filled += 4;
        }
        builder.append(text);
        return builder.toString();
    }

    static String fit(String text, int pixels) {
        if (width(text) <= pixels) {
            return text;
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (width(builder.toString()) + advance(character) + advance('…') > pixels) {
                break;
            }
            builder.append(character);
        }
        builder.append('…');
        return builder.toString();
    }

    private static int advance(char character) {
        return switch (character) {
            case 'i', 'I', '!', '\'', ',', '.', ':', ';', '|', '’' -> 2;
            case 'l', '`' -> 3;
            case ' ', 't', '[', ']' -> 4;
            case '"', '(', ')', '*', '<', '>', '{', '}', 'f', 'k' -> 5;
            default -> character > 127 ? 10 : 6;
        };
    }
}
