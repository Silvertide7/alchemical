package net.silvertide.alchemical.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.records.ModifierDefinition;

import java.util.List;

/**
 * Everything the server loaded from datapacks, so the client can render names, colours and
 * stats. Sent once per login, so the definitions just go through their JSON codecs.
 */
public record CB_SyncIngredientDefinitionsPacket(
        List<EssenceStoneDefinition> stones,
        List<ModifierDefinition> tinctures,
        List<ModifierDefinition> catalysts
) {
    public void encode(FriendlyByteBuf buf) {
        // The id isn't part of the stone's JSON codec, so it travels alongside.
        buf.writeCollection(stones, (b, def) -> {
            b.writeResourceLocation(def.id());
            b.writeJsonWithCodec(EssenceStoneDefinition.CODEC, def);
        });
        buf.writeCollection(tinctures, (b, def) -> b.writeJsonWithCodec(ModifierDefinition.CODEC, def));
        buf.writeCollection(catalysts, (b, def) -> b.writeJsonWithCodec(ModifierDefinition.CODEC, def));
    }

    public static CB_SyncIngredientDefinitionsPacket decode(FriendlyByteBuf buf) {
        List<EssenceStoneDefinition> stones = buf.readList(b -> {
            ResourceLocation id = b.readResourceLocation();
            return b.readJsonWithCodec(EssenceStoneDefinition.CODEC).withId(id);
        });
        List<ModifierDefinition> tinctures = buf.readList(b -> b.readJsonWithCodec(ModifierDefinition.CODEC));
        List<ModifierDefinition> catalysts = buf.readList(b -> b.readJsonWithCodec(ModifierDefinition.CODEC));
        return new CB_SyncIngredientDefinitionsPacket(stones, tinctures, catalysts);
    }
}
