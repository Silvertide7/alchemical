package net.silvertide.alchemical.client;

public final class ClientElixirCooldownData {
    // 40 ticks = 2 seconds at 20 TPS — prevents spam when repeatedly right-clicking while on cooldown
    private static final int MESSAGE_GATE_TICKS = 40;

    private static long lastDrankAt = 0L;
    private static int cooldownSeconds = 0;
    private static long lastMessageSentAt = -60L;

    public static void set(long lastDrankAt, int cooldownSeconds) {
        ClientElixirCooldownData.lastDrankAt = lastDrankAt;
        ClientElixirCooldownData.cooldownSeconds = cooldownSeconds;
    }

    public static boolean isOnCooldown(long currentGameTime) {
        return (currentGameTime - lastDrankAt) / 20 < cooldownSeconds;
    }

    public static int getRemainingSeconds(long currentGameTime) {
        return Math.max(0, cooldownSeconds - (int) ((currentGameTime - lastDrankAt) / 20));
    }

    public static boolean tryMarkMessageSent(long currentGameTime) {
        if (currentGameTime - lastMessageSentAt >= MESSAGE_GATE_TICKS) {
            lastMessageSentAt = currentGameTime;
            return true;
        }
        return false;
    }

    private ClientElixirCooldownData() {}
}
