package net.silvertide.alchemical.client;

import net.silvertide.alchemical.data.ClientIngredientData;
import net.silvertide.alchemical.network.CB_SyncElixirCooldownPacket;
import net.silvertide.alchemical.network.CB_SyncIngredientDefinitionsPacket;

/**
 * Client-side packet handler logic. Only loaded on the client via lambda references
 * in Networking — never resolved on a dedicated server.
 */
public final class ClientPacketHandler {

    public static void handleCooldownSync(CB_SyncElixirCooldownPacket packet) {
        ClientElixirCooldownData.set(packet.lastDrankAt(), packet.cooldownSeconds());
    }

    public static void handleIngredientSync(CB_SyncIngredientDefinitionsPacket packet) {
        ClientIngredientData.sync(packet.stones(), packet.tinctures(), packet.catalysts());
    }

    private ClientPacketHandler() {}
}
