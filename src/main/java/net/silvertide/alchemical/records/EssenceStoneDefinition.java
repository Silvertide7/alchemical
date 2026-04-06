package net.silvertide.alchemical.records;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Optional;

public record EssenceStoneDefinition(
        Item item,
        Optional<String> name,
        ResourceLocation effect,
        int baseDuration,
        int baseLevel,
        float elixirCooldownMultiplier,
        int elixirCooldownFlat
) {
    public static final Codec<EssenceStoneDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(EssenceStoneDefinition::item),
            Codec.STRING.optionalFieldOf("name").forGetter(EssenceStoneDefinition::name),
            ResourceLocation.CODEC.fieldOf("effect").forGetter(EssenceStoneDefinition::effect),
            Codec.INT.optionalFieldOf("base_duration", 200).forGetter(EssenceStoneDefinition::baseDuration),
            Codec.INT.optionalFieldOf("base_level", 1).forGetter(EssenceStoneDefinition::baseLevel),
            Codec.FLOAT.optionalFieldOf("elixir_cooldown_multiplier", 1.0f).forGetter(EssenceStoneDefinition::elixirCooldownMultiplier),
            Codec.INT.optionalFieldOf("elixir_cooldown_flat", 0).forGetter(EssenceStoneDefinition::elixirCooldownFlat)
    ).apply(inst, EssenceStoneDefinition::new));

    // StreamCodec.composite supports up to 6 fields; write manually for 7
    public static final StreamCodec<RegistryFriendlyByteBuf, EssenceStoneDefinition> STREAM_CODEC = StreamCodec.of(
            (buf, def) -> {
                ByteBufCodecs.registry(Registries.ITEM).encode(buf, def.item());
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buf, def.name());
                ResourceLocation.STREAM_CODEC.encode(buf, def.effect());
                ByteBufCodecs.INT.encode(buf, def.baseDuration());
                ByteBufCodecs.INT.encode(buf, def.baseLevel());
                ByteBufCodecs.FLOAT.encode(buf, def.elixirCooldownMultiplier());
                ByteBufCodecs.INT.encode(buf, def.elixirCooldownFlat());
            },
            buf -> new EssenceStoneDefinition(
                    ByteBufCodecs.registry(Registries.ITEM).decode(buf),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buf),
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.INT.decode(buf)
            )
    );
}
