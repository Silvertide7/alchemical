package net.silvertide.alchemical.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.data.CatalystLoader;
import net.silvertide.alchemical.data.EssenceStoneLoader;
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.data.TinctureLoader;
import net.silvertide.alchemical.network.CB_SyncElixirCooldownPacket;
import net.silvertide.alchemical.network.CB_SyncIngredientDefinitionsPacket;
import net.silvertide.alchemical.util.ElixirAttachmentUtil;

import java.util.ArrayList;
import java.util.List;

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

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(EssenceStoneLoader.INSTANCE);
        event.addListener(TinctureLoader.INSTANCE);
        event.addListener(CatalystLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        CB_SyncIngredientDefinitionsPacket packet = new CB_SyncIngredientDefinitionsPacket(
                new ArrayList<>(IngredientManager.getAllStones()),
                new ArrayList<>(IngredientManager.getAllTinctures()),
                new ArrayList<>(IngredientManager.getAllCatalysts())
        );
        if (event.getPlayer() != null) {
            PacketDistributor.sendToPlayer(event.getPlayer(), packet);
        } else {
            PacketDistributor.sendToAllPlayers(packet);
        }
    }

    private ElixirEvents() {}
}
