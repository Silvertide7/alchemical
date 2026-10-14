package net.silvertide.alchemical.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.silvertide.alchemical.client.ClientElixirCooldownData;
import net.silvertide.alchemical.data.IngredientManager;

public final class Networking {
    private Networking() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(Networking::registerPackets);
    }

    // Handlers run on the client main thread by default, so no enqueueWork needed.
    private static void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(CB_SyncElixirCooldownPacket.TYPE, CB_SyncElixirCooldownPacket.STREAM_CODEC,
                (packet, ctx) -> ClientElixirCooldownData.set(packet.cooldown()));

        registrar.playToClient(CB_SyncIngredientDefinitionsPacket.TYPE, CB_SyncIngredientDefinitionsPacket.STREAM_CODEC,
                (packet, ctx) -> {
                    IngredientManager.setStones(packet.stones());
                    IngredientManager.setTinctures(packet.tinctures());
                    IngredientManager.setCatalysts(packet.catalysts());
                });
    }
}
