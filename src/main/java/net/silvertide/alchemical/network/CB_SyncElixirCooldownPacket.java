package net.silvertide.alchemical.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.silvertide.alchemical.Alchemical;

public record CB_SyncElixirCooldownPacket(long lastDrankAt, int cooldownSeconds) implements CustomPacketPayload {
    public static final Type<CB_SyncElixirCooldownPacket> TYPE =
            new Type<>(Alchemical.id("sync_elixir_cooldown"));

    public static final StreamCodec<FriendlyByteBuf, CB_SyncElixirCooldownPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG, CB_SyncElixirCooldownPacket::lastDrankAt,
                    ByteBufCodecs.INT, CB_SyncElixirCooldownPacket::cooldownSeconds,
                    CB_SyncElixirCooldownPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
