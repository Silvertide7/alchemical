package net.silvertide.alchemical.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.network.CB_SyncElixirCooldownPacket;
import net.silvertide.alchemical.util.ElixirAttachmentUtil;

@EventBusSubscriber(modid = Alchemical.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class ElixirEvents {

    // On login, restore the client-side cooldown state from the attachment.
    // Without this, logging out mid-cooldown would clear the client's knowledge
    // of the cooldown even though the attachment (and server check) still holds it.
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ElixirAttachmentUtil.getCooldown(player).ifPresent(cooldown -> {
                if (cooldown.isOnCooldown(player.level().getGameTime())) {
                    PacketDistributor.sendToPlayer(player,
                            new CB_SyncElixirCooldownPacket(cooldown.lastDrankAt(), cooldown.cooldownSeconds()));
                }
            });
        }
    }

    private ElixirEvents() {}
}
