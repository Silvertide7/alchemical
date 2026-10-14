package net.silvertide.alchemical.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.records.ModifierDefinition;

import java.util.List;

/** Everything the server loaded from datapacks, so the client can render names, colours and stats. */
public record CB_SyncIngredientDefinitionsPacket(
        List<EssenceStoneDefinition> stones,
        List<ModifierDefinition> tinctures,
        List<ModifierDefinition> catalysts
) implements CustomPacketPayload {
    public static final Type<CB_SyncIngredientDefinitionsPacket> TYPE = new Type<>(Alchemical.id("sync_ingredient_definitions"));

    public static final StreamCodec<ByteBuf, CB_SyncIngredientDefinitionsPacket> STREAM_CODEC = StreamCodec.composite(
            EssenceStoneDefinition.STREAM_CODEC.apply(ByteBufCodecs.list()), CB_SyncIngredientDefinitionsPacket::stones,
            ModifierDefinition.STREAM_CODEC.apply(ByteBufCodecs.list()), CB_SyncIngredientDefinitionsPacket::tinctures,
            ModifierDefinition.STREAM_CODEC.apply(ByteBufCodecs.list()), CB_SyncIngredientDefinitionsPacket::catalysts,
            CB_SyncIngredientDefinitionsPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
