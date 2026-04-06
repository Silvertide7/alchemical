package net.silvertide.alchemical.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.silvertide.alchemical.client.ClientElixirCooldownData;
import net.silvertide.alchemical.data.ClientIngredientData;

public final class Networking {
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(Networking::registerPackets);
    }

    private static void registerPackets(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(
                CB_SyncElixirCooldownPacket.TYPE,
                CB_SyncElixirCooldownPacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() ->
                        ClientElixirCooldownData.set(packet.lastDrankAt(), packet.cooldownSeconds())));

        registrar.playToClient(
                CB_SyncIngredientDefinitionsPacket.TYPE,
                CB_SyncIngredientDefinitionsPacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() ->
                        ClientIngredientData.sync(packet.stones(), packet.tinctures(), packet.catalysts())));
    }

    private Networking() {}
}
