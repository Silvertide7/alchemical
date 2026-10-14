package net.silvertide.alchemical.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.attachments.ElixirCooldown;

public record CB_SyncElixirCooldownPacket(ElixirCooldown cooldown) implements CustomPacketPayload {
    public static final Type<CB_SyncElixirCooldownPacket> TYPE = new Type<>(Alchemical.id("sync_elixir_cooldown"));

    public static final StreamCodec<ByteBuf, CB_SyncElixirCooldownPacket> STREAM_CODEC =
            ElixirCooldown.STREAM_CODEC.map(CB_SyncElixirCooldownPacket::new, CB_SyncElixirCooldownPacket::cooldown);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
