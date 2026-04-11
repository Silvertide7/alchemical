package net.silvertide.alchemical.util;

/**
 * Shared formatting helpers used across both item tooltips and GUI screens.
 */
public final class FormattingUtil {
    private FormattingUtil() {}

    /** Converts ticks to a human-friendly time string like "8m 0s" or "1m 30s". */
    public static String ticksToTime(int ticks) {
        return secondsToTime(ticks / 20);
    }

    /** Converts seconds to a human-friendly time string like "30m 0s" or "1h 5m 0s". */
    public static String secondsToTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (hours > 0 || minutes > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString();
    }

    /** Converts a number (1-10) to a Roman numeral string. Falls back to decimal for >10. */
    public static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }
}
