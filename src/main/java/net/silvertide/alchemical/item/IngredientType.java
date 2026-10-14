package net.silvertide.alchemical.item;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.registry.DataComponentRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Which kind of ingredient a stack is, as decided by the loaded datapack definitions. */
public enum IngredientType {
    ESSENCE_STONE(DataComponentRegistry.ESSENCE_STONES),
    TINCTURE(DataComponentRegistry.TINCTURES),
    CATALYST(DataComponentRegistry.CATALYSTS),
    NONE(null);

    /** The list on the elixir this kind of ingredient is stored in. */
    public final @Nullable Supplier<DataComponentType<List<ItemStack>>> component;

    IngredientType(@Nullable Supplier<DataComponentType<List<ItemStack>>> component) {
        this.component = component;
    }

    public static IngredientType of(ItemStack stack) {
        if (stack.isEmpty()) return NONE;
        if (stack.has(DataComponentRegistry.ESSENCE_STONE_TYPE.get())) return ESSENCE_STONE;
        if (IngredientManager.getTincture(stack.getItem()).isPresent()) return TINCTURE;
        if (IngredientManager.getCatalyst(stack.getItem()).isPresent()) return CATALYST;
        return NONE;
    }

    /** Lower-case name for lang keys: "essence_stone", "tincture", "catalyst". */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
