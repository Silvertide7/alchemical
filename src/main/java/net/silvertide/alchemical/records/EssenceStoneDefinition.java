package net.silvertide.alchemical.records;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public record EssenceStoneDefinition(
        ResourceLocation id,           // set by loader from file key — NOT in JSON codec
        Optional<String> name,
        int color,
        ResourceLocation effect,
        int baseDuration,
        int baseLevel,
        float elixirCooldownMultiplier,
        int elixirCooldownFlat,
        int potency                    // capacity slots consumed; default 2 for stones
) {
    private static final ResourceLocation PLACEHOLDER_ID = ResourceLocation.fromNamespaceAndPath("alchemical", "unknown");

    private static final Codec<Integer> HEX_COLOR_CODEC = Codec.STRING.xmap(
            s -> (int) Long.parseLong(s.startsWith("#") ? s.substring(1) : s, 16),
            i -> String.format("#%06X", i & 0xFFFFFF)
    );

    // JSON codec — 8 fields, no id (id is stamped by the loader from the file key)
    public static final Codec<EssenceStoneDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.optionalFieldOf("name").forGetter(EssenceStoneDefinition::name),
            HEX_COLOR_CODEC.optionalFieldOf("color", 0xAA88FF).forGetter(EssenceStoneDefinition::color),
            ResourceLocation.CODEC.fieldOf("effect").forGetter(EssenceStoneDefinition::effect),
            Codec.INT.optionalFieldOf("base_duration", 200).forGetter(EssenceStoneDefinition::baseDuration),
            Codec.INT.optionalFieldOf("base_level", 1).forGetter(EssenceStoneDefinition::baseLevel),
            Codec.FLOAT.optionalFieldOf("elixir_cooldown_multiplier", 1.0f).forGetter(EssenceStoneDefinition::elixirCooldownMultiplier),
            Codec.INT.optionalFieldOf("elixir_cooldown_flat", 0).forGetter(EssenceStoneDefinition::elixirCooldownFlat),
            Codec.INT.optionalFieldOf("potency", 2).forGetter(EssenceStoneDefinition::potency)
    ).apply(inst, (name, color, effect, dur, lvl, cMult, cFlat, potency) ->
            new EssenceStoneDefinition(PLACEHOLDER_ID, name, color, effect, dur, lvl, cMult, cFlat, potency)));

    // Network codec — includes id as first field
    public static final StreamCodec<RegistryFriendlyByteBuf, EssenceStoneDefinition> STREAM_CODEC = StreamCodec.of(
            (buf, def) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, def.id());
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buf, def.name());
                ByteBufCodecs.INT.encode(buf, def.color());
                ResourceLocation.STREAM_CODEC.encode(buf, def.effect());
                ByteBufCodecs.INT.encode(buf, def.baseDuration());
                ByteBufCodecs.INT.encode(buf, def.baseLevel());
                ByteBufCodecs.FLOAT.encode(buf, def.elixirCooldownMultiplier());
                ByteBufCodecs.INT.encode(buf, def.elixirCooldownFlat());
                ByteBufCodecs.INT.encode(buf, def.potency());
            },
            buf -> new EssenceStoneDefinition(
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf)
            )
    );

    public EssenceStoneDefinition withId(ResourceLocation id) {
        return new EssenceStoneDefinition(id, name, color, effect, baseDuration, baseLevel,
                elixirCooldownMultiplier, elixirCooldownFlat, potency);
    }
}
