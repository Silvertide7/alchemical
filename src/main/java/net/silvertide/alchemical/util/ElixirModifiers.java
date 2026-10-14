package net.silvertide.alchemical.util;

import net.minecraft.world.item.ItemStack;
import net.silvertide.alchemical.config.AlchemicalConfig;
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.item.ElixirItem;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.records.ModifierDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Every tincture and catalyst bonus in a flask folded together. Apply to a stone to get
 * the numbers the player actually sees.
 */
public record ElixirModifiers(float durationMult, int durationFlat, int levelMod, float cooldownMult, int cooldownFlat) {

    public static ElixirModifiers of(ItemStack flask) {
        List<ModifierDefinition> defs = new ArrayList<>();
        for (ItemStack s : ElixirItem.tinctures(flask)) IngredientManager.getTincture(s.getItem()).ifPresent(defs::add);
        for (ItemStack s : ElixirItem.catalysts(flask)) IngredientManager.getCatalyst(s.getItem()).ifPresent(defs::add);

        float durationMult = 1f, cooldownMult = 1f;
        int durationFlat = 0, levelMod = 0, cooldownFlat = 0;
        for (ModifierDefinition def : defs) {
            durationMult *= def.durationMultiplier();
            durationFlat += def.durationFlat();
            levelMod += def.levelModifier();
            cooldownMult *= def.cooldownMultiplier();
            cooldownFlat += def.cooldownFlat();
        }
        return new ElixirModifiers(durationMult, durationFlat, levelMod, cooldownMult, cooldownFlat);
    }

    /** Effect duration in ticks. */
    public int duration(EssenceStoneDefinition stone) {
        return Math.max(1, (int) ((stone.baseDuration() + durationFlat) * durationMult));
    }

    /** Effect level, 1-based. */
    public int level(EssenceStoneDefinition stone) {
        return Math.max(1, stone.baseLevel() + levelMod);
    }

    /** Cooldown in seconds: config base plus every flat bonus, times every multiplier. */
    public int cooldown(EssenceStoneDefinition stone) {
        int base = AlchemicalConfig.ELIXIR_COOLDOWN_SECONDS.get();
        float mult = cooldownMult * stone.cooldownMultiplier();
        return Math.max(0, (int) ((base + cooldownFlat + stone.cooldownFlat()) * mult));
    }
}
