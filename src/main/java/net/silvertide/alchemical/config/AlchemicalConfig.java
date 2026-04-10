package net.silvertide.alchemical.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AlchemicalConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.DoubleValue ESSENCE_STONE_BREAK_CHANCE;
    public static final ModConfigSpec.IntValue ELIXIR_COOLDOWN_SECONDS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("server");
        ESSENCE_STONE_BREAK_CHANCE = builder
                .comment("Chance (0.0 to 1.0) that each Essence Stone is destroyed when clearing an elixir. Default: 0.5 (50% chance to break).")
                .defineInRange("essenceStoneBreakChance", 0.5, 0.0, 1.0);
        ELIXIR_COOLDOWN_SECONDS = builder
                .comment("Base global cooldown in seconds after drinking an elixir. Default: 1800 (30 minutes).")
                .defineInRange("elixirCooldownSeconds", 1800, 0, 86400);
        builder.pop();
        SERVER_SPEC = builder.build();
    }

    private AlchemicalConfig() {}
}
