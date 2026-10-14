package net.silvertide.alchemical.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;

public final class FormattingUtil {
    private FormattingUtil() {}

    /** "8m 0s", "1h 5m 0s" */
    public static String ticksToTime(int ticks) {
        return secondsToTime(ticks / 20);
    }

    public static String secondsToTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (hours > 0 || minutes > 0) sb.append(minutes).append("m ");
        return sb.append(seconds).append("s").toString();
    }

    /** 1-10 as Roman numerals, anything else as digits. */
    public static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }

    /** "Regeneration II" */
    public static Component effectName(MobEffect effect, int level) {
        return Component.translatable("gui.alchemical.athanor.stat.effect_name", effect.getDisplayName(), toRoman(level));
    }
}
