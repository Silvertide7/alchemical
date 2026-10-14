package net.silvertide.alchemical.client;

import net.silvertide.alchemical.data.ElixirCooldown;

/** The client's copy of its own cooldown, fed by CB_SyncElixirCooldownPacket. */
public final class ClientElixirCooldownData {
    // Right-click repeats every few ticks while held; don't flood chat with the same warning.
    private static final int MESSAGE_GATE_TICKS = 40;

    private static ElixirCooldown cooldown = ElixirCooldown.NONE;
    private static long lastMessageAt = -MESSAGE_GATE_TICKS;

    private ClientElixirCooldownData() {}

    public static ElixirCooldown get() {
        return cooldown;
    }

    public static void set(ElixirCooldown value) {
        cooldown = value;
    }

    /** True at most once per gate window; the caller sends its message only if so. */
    public static boolean tryMarkMessageSent(long gameTime) {
        if (gameTime - lastMessageAt < MESSAGE_GATE_TICKS) return false;
        lastMessageAt = gameTime;
        return true;
    }
}
