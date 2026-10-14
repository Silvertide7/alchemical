package net.silvertide.alchemical.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.silvertide.alchemical.network.CB_SyncElixirCooldownPacket;
import net.silvertide.alchemical.network.Networking;

/**
 * When a player last drank an elixir and how long they have to wait. Stored in the player's
 * persistent NBT under PlayerPersisted, which Forge carries across death and dimension
 * changes on its own. The client keeps a copy (ClientElixirCooldownData) for instant feedback.
 */
public record ElixirCooldown(long lastDrankAt, int cooldownSeconds) {
    public static final ElixirCooldown NONE = new ElixirCooldown(0L, 0);

    private static final String KEY = "alchemical:elixir_cooldown";

    public boolean isActive(long gameTime) {
        return remainingSeconds(gameTime) > 0;
    }

    public int remainingSeconds(long gameTime) {
        return (int) Math.max(0, cooldownSeconds - (gameTime - lastDrankAt) / 20);
    }

    // --- Player storage ---

    public static ElixirCooldown of(Player player) {
        CompoundTag tag = persisted(player).getCompound(KEY);
        return tag.isEmpty() ? NONE : new ElixirCooldown(tag.getLong("lastDrankAt"), tag.getInt("cooldownSeconds"));
    }

    public static boolean isOnCooldown(Player player) {
        return of(player).isActive(player.level().getGameTime());
    }

    /** Stores the cooldown on the player and pushes it to their client. */
    public static void set(ServerPlayer player, ElixirCooldown cooldown) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("lastDrankAt", cooldown.lastDrankAt);
        tag.putInt("cooldownSeconds", cooldown.cooldownSeconds);
        persisted(player).put(KEY, tag);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        Networking.sendToPlayer(player, new CB_SyncElixirCooldownPacket(of(player)));
    }

    private static CompoundTag persisted(Player player) {
        CompoundTag root = player.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    // --- Network ---

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarLong(lastDrankAt);
        buf.writeVarInt(cooldownSeconds);
    }

    public static ElixirCooldown decode(FriendlyByteBuf buf) {
        return new ElixirCooldown(buf.readVarLong(), buf.readVarInt());
    }
}
