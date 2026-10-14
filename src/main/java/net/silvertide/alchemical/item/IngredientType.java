package net.silvertide.alchemical.item;

import net.minecraft.world.item.ItemStack;
import net.silvertide.alchemical.data.IngredientManager;

import java.util.Locale;

/** Which kind of ingredient a stack is, as decided by the loaded datapack definitions. */
public enum IngredientType {
    ESSENCE_STONE("essence_stones"),
    TINCTURE("tinctures"),
    CATALYST("catalysts"),
    NONE("");

    /** The NBT list on the elixir this kind of ingredient is stored in. */
    public final String nbtKey;

    IngredientType(String nbtKey) {
        this.nbtKey = nbtKey;
    }

    public static IngredientType of(ItemStack stack) {
        if (stack.isEmpty()) return NONE;
        if (EssenceStoneItem.typeOf(stack) != null) return ESSENCE_STONE;
        if (IngredientManager.getTincture(stack.getItem()).isPresent()) return TINCTURE;
        if (IngredientManager.getCatalyst(stack.getItem()).isPresent()) return CATALYST;
        return NONE;
    }

    /** Lower-case name for lang keys: "essence_stone", "tincture", "catalyst". */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
