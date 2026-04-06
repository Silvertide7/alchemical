package net.silvertide.alchemical.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.records.CatalystDefinition;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.records.TinctureDefinition;

import java.util.List;

public record CB_SyncIngredientDefinitionsPacket(
        List<EssenceStoneDefinition> stones,
        List<TinctureDefinition> tinctures,
        List<CatalystDefinition> catalysts
) implements CustomPacketPayload {
    public static final Type<CB_SyncIngredientDefinitionsPacket> TYPE =
            new Type<>(Alchemical.id("sync_ingredient_definitions"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CB_SyncIngredientDefinitionsPacket> STREAM_CODEC =
            StreamCodec.composite(
                    EssenceStoneDefinition.STREAM_CODEC.apply(ByteBufCodecs.list()), CB_SyncIngredientDefinitionsPacket::stones,
                    TinctureDefinition.STREAM_CODEC.apply(ByteBufCodecs.list()), CB_SyncIngredientDefinitionsPacket::tinctures,
                    CatalystDefinition.STREAM_CODEC.apply(ByteBufCodecs.list()), CB_SyncIngredientDefinitionsPacket::catalysts,
                    CB_SyncIngredientDefinitionsPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
