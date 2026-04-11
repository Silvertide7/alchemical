package net.silvertide.alchemical.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.records.ModifierDefinition;
import net.silvertide.alchemical.registry.DataComponentRegistry;

import java.util.List;
import java.util.Optional;

/**
 * Shared utility for computing elixir modifier totals from tinctures and catalysts.
 * Works on both client and server by delegating lookups through the dist-appropriate store.
 */
public final class ElixirCalcUtil {
    private ElixirCalcUtil() {}

    /**
     * Accumulated modifier totals from tinctures and catalysts (not including stone modifiers).
     */
    public record ModifierResult(
            float durationMult,
            int durationFlat,
            int levelMod,
            float cooldownMult,
            int cooldownFlat
    ) {
        public static final ModifierResult IDENTITY = new ModifierResult(1.0f, 0, 0, 1.0f, 0);
    }

    /**
     * Aggregates modifier values from all tinctures and catalysts.
     * Uses ClientIngredientData on the client, IngredientManager on the server.
     */
    public static ModifierResult computeSharedModifiers(List<ItemStack> tinctures, List<ItemStack> catalysts) {
        float durMult = 1.0f;
        int durFlat = 0;
        int levelMod = 0;
        float cdMult = 1.0f;
        int cdFlat = 0;

        for (ItemStack stack : tinctures) {
            Optional<? extends ModifierDefinition> opt = IngredientUtil.getTincture(stack.getItem());
            if (opt.isPresent()) {
                ModifierDefinition def = opt.get();
                durMult *= def.effectDurationMultiplier();
                durFlat += def.effectDurationFlat();
                levelMod += def.effectLevelModifier();
                cdMult *= def.elixirCooldownMultiplier();
                cdFlat += def.elixirCooldownFlat();
            }
        }

        for (ItemStack stack : catalysts) {
            Optional<? extends ModifierDefinition> opt = IngredientUtil.getCatalyst(stack.getItem());
            if (opt.isPresent()) {
                ModifierDefinition def = opt.get();
                durMult *= def.effectDurationMultiplier();
                durFlat += def.effectDurationFlat();
                levelMod += def.effectLevelModifier();
                cdMult *= def.elixirCooldownMultiplier();
                cdFlat += def.elixirCooldownFlat();
            }
        }

        return new ModifierResult(durMult, durFlat, levelMod, cdMult, cdFlat);
    }

    /**
     * Computes the effective effect duration in ticks for a given stone after applying modifiers.
     */
    public static int computeEffectiveDuration(EssenceStoneDefinition stone, ModifierResult mods) {
        return Math.max(1, (int) ((stone.baseDuration() + mods.durationFlat()) * mods.durationMult()));
    }

    /**
     * Computes the effective effect level (1-based) for a given stone after applying modifiers.
     */
    public static int computeEffectiveLevel(EssenceStoneDefinition stone, ModifierResult mods) {
        return Math.max(1, stone.baseLevel() + mods.levelMod());
    }

    /**
     * Computes the effective cooldown in seconds for a given stone after applying all modifiers.
     * The stone's own cooldown modifier is combined with the shared tincture/catalyst modifiers.
     */
    public static int computeEffectiveCooldown(int baseCooldownSeconds, EssenceStoneDefinition stone, ModifierResult mods) {
        float totalCdMult = stone.elixirCooldownMultiplier() * mods.cooldownMult();
        int totalCdFlat = stone.elixirCooldownFlat() + mods.cooldownFlat();
        return Math.max(0, (int) ((baseCooldownSeconds + totalCdFlat) * totalCdMult));
    }

    /**
     * Resolves an EssenceStoneDefinition from a stone ItemStack, using the appropriate dist store.
     */
    public static Optional<EssenceStoneDefinition> resolveStone(ItemStack stoneStack) {
        ResourceLocation stoneType = stoneStack.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());
        if (stoneType == null) return Optional.empty();
        return IngredientUtil.getStone(stoneType);
    }
}
