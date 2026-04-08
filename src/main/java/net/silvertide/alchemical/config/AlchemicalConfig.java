package net.silvertide.alchemical.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AlchemicalConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.DoubleValue ESSENCE_STONE_BREAK_CHANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("server");
        ESSENCE_STONE_BREAK_CHANCE = builder
                .comment("Chance (0.0 to 1.0) that each Essence Stone is destroyed when clearing an elixir. Default: 0.5 (50% chance to break).")
                .defineInRange("essenceStoneBreakChance", 0.5, 0.0, 1.0);
        builder.pop();
        SERVER_SPEC = builder.build();
    }

    private AlchemicalConfig() {}
}
