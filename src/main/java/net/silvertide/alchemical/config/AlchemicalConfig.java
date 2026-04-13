package net.silvertide.alchemical.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AlchemicalConfig {
    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.DoubleValue ESSENCE_STONE_BREAK_CHANCE;
    public static final ModConfigSpec.IntValue ELIXIR_COOLDOWN_SECONDS;
    public static final ModConfigSpec.IntValue ELIXIR_CAPACITY;
    public static final ModConfigSpec.IntValue MAX_ESSENCE_STONES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("server");
        ESSENCE_STONE_BREAK_CHANCE = builder
                .comment("Chance (0.0 to 1.0) that each Essence Stone is destroyed when clearing an elixir. Default: 0.5 (50% chance to break).")
                .defineInRange("essenceStoneBreakChance", 0.5, 0.0, 1.0);
        ELIXIR_COOLDOWN_SECONDS = builder
                .comment("Base global cooldown in seconds after drinking an elixir. Default: 1800 (30 minutes).")
                .defineInRange("elixirCooldownSeconds", 1800, 0, 86400);
        ELIXIR_CAPACITY = builder
                .comment("Maximum potency capacity of an elixir flask. Default: 9. Max 45.")
                .defineInRange("elixirCapacity", 9, 1, 45);
        MAX_ESSENCE_STONES = builder
                .comment("Maximum number of essence stones that can be loaded into a single elixir. Default: 3.")
                .comment("The Athanor UI only shows the first 3 stones, so if you go higher you will have to check")
                .comment("using the tooltip.")
                .defineInRange("maxEssenceStones", 3, 1, 10);
        builder.pop();
        SERVER_SPEC = builder.build();
    }

    private AlchemicalConfig() {}
}
