package net.silvertide.alchemical.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.silvertide.alchemical.attachments.ElixirCooldown;
import net.silvertide.alchemical.network.CB_SyncElixirCooldownPacket;
import net.silvertide.alchemical.registry.AttachmentRegistry;

import java.util.Optional;

public final class ElixirAttachmentUtil {
    private ElixirAttachmentUtil() {}

    public static Optional<ElixirCooldown> getCooldown(Player player) {
        if (player.hasData(AttachmentRegistry.ELIXIR_COOLDOWN)) {
            return Optional.of(player.getData(AttachmentRegistry.ELIXIR_COOLDOWN));
        }
        return Optional.empty();
    }

    public static void setCooldown(Player player, ElixirCooldown cooldown) {
        if (cooldown == null) {
            player.removeData(AttachmentRegistry.ELIXIR_COOLDOWN);
        } else {
            player.setData(AttachmentRegistry.ELIXIR_COOLDOWN, cooldown);
        }
    }

    public static boolean isOnCooldown(Player player) {
        return getCooldown(player)
                .map(cooldown -> cooldown.isOnCooldown(player.level().getGameTime()))
                .orElse(false);
    }

    public static void applyNewCooldown(ServerPlayer player, int cooldownSeconds) {
        ElixirCooldown cooldown = new ElixirCooldown(player.level().getGameTime(), cooldownSeconds);
        setCooldown(player, cooldown);
        PacketDistributor.sendToPlayer(player,
                new CB_SyncElixirCooldownPacket(cooldown.lastDrankAt(), cooldown.cooldownSeconds()));
    }
}
