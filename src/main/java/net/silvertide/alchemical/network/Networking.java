package net.silvertide.alchemical.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.client.ClientElixirCooldownData;
import net.silvertide.alchemical.data.IngredientManager;

public final class Networking {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Alchemical.id("main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private Networking() {}

    // Both packets are client-bound, and their handlers only touch common classes, so plain
    // lambdas are safe to register on a dedicated server. consumerMainThread already defers
    // to the main thread and marks the packet handled.
    public static void register() {
        int id = 0;

        CHANNEL.messageBuilder(CB_SyncElixirCooldownPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CB_SyncElixirCooldownPacket::encode)
                .decoder(CB_SyncElixirCooldownPacket::decode)
                .consumerMainThread((packet, ctx) -> ClientElixirCooldownData.set(packet.cooldown()))
                .add();

        CHANNEL.messageBuilder(CB_SyncIngredientDefinitionsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CB_SyncIngredientDefinitionsPacket::encode)
                .decoder(CB_SyncIngredientDefinitionsPacket::decode)
                .consumerMainThread((packet, ctx) -> {
                    IngredientManager.setStones(packet.stones());
                    IngredientManager.setTinctures(packet.tinctures());
                    IngredientManager.setCatalysts(packet.catalysts());
                })
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
