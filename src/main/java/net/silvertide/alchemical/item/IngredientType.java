package net.silvertide.alchemical.item;

import net.minecraft.world.item.ItemStack;
import net.silvertide.alchemical.data.IngredientManager;

/**
 * Identifies which category of Elixir ingredient an ItemStack belongs to.
 * Use {@link #of(ItemStack)} as the single authoritative resolution point —
 * never do raw instanceof checks in menu or screen code.
 * Resolution is driven by datapack definitions in IngredientManager.
 */
public enum IngredientType {
    TINCTURE,
    ESSENCE_STONE,
    CATALYST,
    NONE;

    public static IngredientType of(ItemStack stack) {
        if (stack.isEmpty()) return NONE;
        if (IngredientManager.getStone(stack.getItem()).isPresent()) return ESSENCE_STONE;
        if (IngredientManager.getTincture(stack.getItem()).isPresent()) return TINCTURE;
        if (IngredientManager.getCatalyst(stack.getItem()).isPresent()) return CATALYST;
        return NONE;
    }
}
