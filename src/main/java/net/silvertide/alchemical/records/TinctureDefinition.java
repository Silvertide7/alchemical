package net.silvertide.alchemical.records;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

import java.util.Optional;

public record TinctureDefinition(
        Item item,
        Optional<String> name,
        float elixirCooldownMultiplier,
        int elixirCooldownFlat,
        float effectDurationMultiplier,
        int effectDurationFlat,
        int effectLevelModifier,
        int potency                    // capacity slots consumed; default 1
) {
    public static final Codec<TinctureDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(TinctureDefinition::item),
            Codec.STRING.optionalFieldOf("name").forGetter(TinctureDefinition::name),
            Codec.FLOAT.optionalFieldOf("elixir_cooldown_multiplier", 1.0f).forGetter(TinctureDefinition::elixirCooldownMultiplier),
            Codec.INT.optionalFieldOf("elixir_cooldown_flat", 0).forGetter(TinctureDefinition::elixirCooldownFlat),
            Codec.FLOAT.optionalFieldOf("effect_duration_multiplier", 1.0f).forGetter(TinctureDefinition::effectDurationMultiplier),
            Codec.INT.optionalFieldOf("effect_duration_flat", 0).forGetter(TinctureDefinition::effectDurationFlat),
            Codec.INT.optionalFieldOf("effect_level_modifier", 0).forGetter(TinctureDefinition::effectLevelModifier),
            Codec.INT.optionalFieldOf("potency", 1).forGetter(TinctureDefinition::potency)
    ).apply(inst, TinctureDefinition::new));

    // StreamCodec.composite supports up to 6 fields; write manually for 8
    public static final StreamCodec<RegistryFriendlyByteBuf, TinctureDefinition> STREAM_CODEC = StreamCodec.of(
            (buf, def) -> {
                ByteBufCodecs.registry(Registries.ITEM).encode(buf, def.item());
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buf, def.name());
                ByteBufCodecs.FLOAT.encode(buf, def.elixirCooldownMultiplier());
                ByteBufCodecs.INT.encode(buf, def.elixirCooldownFlat());
                ByteBufCodecs.FLOAT.encode(buf, def.effectDurationMultiplier());
                ByteBufCodecs.INT.encode(buf, def.effectDurationFlat());
                ByteBufCodecs.INT.encode(buf, def.effectLevelModifier());
                ByteBufCodecs.INT.encode(buf, def.potency());
            },
            buf -> new TinctureDefinition(
                    ByteBufCodecs.registry(Registries.ITEM).decode(buf),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf)
            )
    );
}
