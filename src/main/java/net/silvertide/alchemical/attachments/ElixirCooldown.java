package net.silvertide.alchemical.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.silvertide.alchemical.network.CB_SyncElixirCooldownPacket;
import net.silvertide.alchemical.registry.AttachmentRegistry;

/**
 * When a player last drank an elixir and how long they have to wait. Stored on the player
 * as an attachment; the client keeps a copy (ClientElixirCooldownData) for instant feedback.
 */
public record ElixirCooldown(long lastDrankAt, int cooldownSeconds) {
    public static final ElixirCooldown NONE = new ElixirCooldown(0L, 0);

    public static final Codec<ElixirCooldown> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.LONG.fieldOf("lastDrankAt").forGetter(ElixirCooldown::lastDrankAt),
            Codec.INT.fieldOf("cooldownSeconds").forGetter(ElixirCooldown::cooldownSeconds)
    ).apply(inst, ElixirCooldown::new));

    public static final StreamCodec<ByteBuf, ElixirCooldown> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, ElixirCooldown::lastDrankAt,
            ByteBufCodecs.VAR_INT, ElixirCooldown::cooldownSeconds,
            ElixirCooldown::new);

    public boolean isActive(long gameTime) {
        return remainingSeconds(gameTime) > 0;
    }

    public int remainingSeconds(long gameTime) {
        return (int) Math.max(0, cooldownSeconds - (gameTime - lastDrankAt) / 20);
    }

    // --- Player attachment ---

    public static ElixirCooldown of(Player player) {
        return player.getData(AttachmentRegistry.ELIXIR_COOLDOWN);
    }

    public static boolean isOnCooldown(Player player) {
        return of(player).isActive(player.level().getGameTime());
    }

    /** Stores the cooldown on the player and pushes it to their client. */
    public static void set(ServerPlayer player, ElixirCooldown cooldown) {
        player.setData(AttachmentRegistry.ELIXIR_COOLDOWN, cooldown);
        sync(player);
    }

    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new CB_SyncElixirCooldownPacket(of(player)));
    }
}
