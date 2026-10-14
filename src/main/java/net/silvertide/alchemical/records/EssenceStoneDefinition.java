package net.silvertide.alchemical.records;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.silvertide.alchemical.Alchemical;

import java.util.Optional;

/** An essence stone: the ingredient that decides which effect an elixir gives. */
public record EssenceStoneDefinition(
        ResourceLocation id,            // the JSON file name; stamped on by the loader, not read from JSON
        Optional<String> name,
        int color,                      // 0xRRGGBB tint for the item overlay
        Holder<MobEffect> effect,
        int baseDuration,               // ticks
        int baseLevel,                  // 1-based
        float cooldownMultiplier,
        int cooldownFlat,               // seconds
        int potency                     // capacity used in the flask
) {
    private static final ResourceLocation UNSET_ID = Alchemical.id("unset");

    private static final Codec<Integer> HEX_COLOR = Codec.STRING.xmap(
            s -> (int) Long.parseLong(s.startsWith("#") ? s.substring(1) : s, 16),
            i -> String.format("#%06X", i & 0xFFFFFF));

    public static final Codec<EssenceStoneDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.STRING.optionalFieldOf("name").forGetter(EssenceStoneDefinition::name),
            HEX_COLOR.optionalFieldOf("color", 0xAA88FF).forGetter(EssenceStoneDefinition::color),
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(EssenceStoneDefinition::effect),
            Codec.INT.optionalFieldOf("base_duration", 200).forGetter(EssenceStoneDefinition::baseDuration),
            Codec.INT.optionalFieldOf("base_level", 1).forGetter(EssenceStoneDefinition::baseLevel),
            Codec.FLOAT.optionalFieldOf("elixir_cooldown_multiplier", 1.0f).forGetter(EssenceStoneDefinition::cooldownMultiplier),
            Codec.INT.optionalFieldOf("elixir_cooldown_flat", 0).forGetter(EssenceStoneDefinition::cooldownFlat),
            Codec.INT.optionalFieldOf("potency", 2).forGetter(EssenceStoneDefinition::potency)
    ).apply(inst, (name, color, effect, duration, level, cdMult, cdFlat, potency) ->
            new EssenceStoneDefinition(UNSET_ID, name, color, effect, duration, level, cdMult, cdFlat, potency)));

    // The id isn't part of the JSON codec, so send it alongside.
    public static final StreamCodec<ByteBuf, EssenceStoneDefinition> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, EssenceStoneDefinition::id,
            ByteBufCodecs.fromCodec(CODEC), def -> def,
            (id, def) -> def.withId(id));

    public EssenceStoneDefinition withId(ResourceLocation id) {
        return new EssenceStoneDefinition(id, name, color, effect, baseDuration, baseLevel,
                cooldownMultiplier, cooldownFlat, potency);
    }
}
