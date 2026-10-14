package net.silvertide.alchemical.records;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

import java.util.Optional;

/**
 * A tincture or a catalyst: a vanilla item that nudges the elixir's cooldown, effect
 * duration, or effect level. Both kinds have the same shape; the datapack folder the
 * JSON sits in (tincture/ or catalyst/) decides which one it is.
 */
public record ModifierDefinition(
        Item item,
        Optional<String> name,          // display name override, e.g. "Honey Tincture"
        float cooldownMultiplier,
        int cooldownFlat,               // seconds
        float durationMultiplier,
        int durationFlat,               // ticks
        int levelModifier,
        int potency                     // capacity used in the flask
) {
    public static final Codec<ModifierDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(ModifierDefinition::item),
            Codec.STRING.optionalFieldOf("name").forGetter(ModifierDefinition::name),
            Codec.FLOAT.optionalFieldOf("elixir_cooldown_multiplier", 1.0f).forGetter(ModifierDefinition::cooldownMultiplier),
            Codec.INT.optionalFieldOf("elixir_cooldown_flat", 0).forGetter(ModifierDefinition::cooldownFlat),
            Codec.FLOAT.optionalFieldOf("effect_duration_multiplier", 1.0f).forGetter(ModifierDefinition::durationMultiplier),
            Codec.INT.optionalFieldOf("effect_duration_flat", 0).forGetter(ModifierDefinition::durationFlat),
            Codec.INT.optionalFieldOf("effect_level_modifier", 0).forGetter(ModifierDefinition::levelModifier),
            Codec.INT.optionalFieldOf("potency", 1).forGetter(ModifierDefinition::potency)
    ).apply(inst, ModifierDefinition::new));

    // Only sent once per login, so going through NBT is fine and saves a hand-written encoder.
    public static final StreamCodec<ByteBuf, ModifierDefinition> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);
}
