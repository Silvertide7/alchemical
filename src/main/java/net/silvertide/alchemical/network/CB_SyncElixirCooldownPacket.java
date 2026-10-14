package net.silvertide.alchemical.network;

import net.minecraft.network.FriendlyByteBuf;
import net.silvertide.alchemical.data.ElixirCooldown;

public record CB_SyncElixirCooldownPacket(ElixirCooldown cooldown) {
    public void encode(FriendlyByteBuf buf) {
        cooldown.encode(buf);
    }

    public static CB_SyncElixirCooldownPacket decode(FriendlyByteBuf buf) {
        return new CB_SyncElixirCooldownPacket(ElixirCooldown.decode(buf));
    }
}
