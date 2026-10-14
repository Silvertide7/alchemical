package net.silvertide.alchemical.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.data.DefinitionLoader;
import net.silvertide.alchemical.data.ElixirCooldown;
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.network.CB_SyncIngredientDefinitionsPacket;
import net.silvertide.alchemical.network.Networking;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.records.ModifierDefinition;

import java.util.List;

@Mod.EventBusSubscriber(modid = Alchemical.MODID)
public final class ElixirEvents {
    private ElixirEvents() {}

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new DefinitionLoader<>("essence_stone", EssenceStoneDefinition.CODEC,
                EssenceStoneDefinition::withId, IngredientManager::setStones));
        event.addListener(new DefinitionLoader<>("tincture", ModifierDefinition.CODEC, IngredientManager::setTinctures));
        event.addListener(new DefinitionLoader<>("catalyst", ModifierDefinition.CODEC, IngredientManager::setCatalysts));
    }

    // Definitions only load on the server; push them to each client on join and after /reload.
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        var packet = new CB_SyncIngredientDefinitionsPacket(
                List.copyOf(IngredientManager.stones()),
                List.copyOf(IngredientManager.tinctures()),
                List.copyOf(IngredientManager.catalysts()));
        List<ServerPlayer> players = event.getPlayer() != null ? List.of(event.getPlayer()) : event.getPlayerList().getPlayers();
        players.forEach(player -> Networking.sendToPlayer(player, packet));
    }

    // The client's cooldown copy is static and outlives the connection, so always overwrite it
    // on login. Otherwise a cooldown from one world shows up as "still recovering" in the next.
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ElixirCooldown.sync(player);
    }
}
