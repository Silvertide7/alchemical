package net.silvertide.alchemical.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.silvertide.alchemical.item.EssenceStoneItem;
import net.silvertide.alchemical.item.IngredientType;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.records.ModifierDefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The loaded ingredient definitions. One store for both sides: the server fills it from
 * datapacks (DefinitionLoader) and the client from the sync packet. On an integrated
 * server both happen and agree. Maps are swapped wholesale so a reload never exposes a
 * half-built set to whoever is reading.
 */
public final class IngredientManager {
    private static volatile Map<ResourceLocation, EssenceStoneDefinition> stones = Map.of();
    private static volatile Map<Item, ModifierDefinition> tinctures = Map.of();
    private static volatile Map<Item, ModifierDefinition> catalysts = Map.of();

    private IngredientManager() {}

    // --- Loading ---

    public static void setStones(Collection<EssenceStoneDefinition> defs) {
        Map<ResourceLocation, EssenceStoneDefinition> map = new TreeMap<>(); // sorted so the creative tab is stable
        defs.forEach(d -> map.put(d.id(), d));
        stones = Collections.unmodifiableMap(map);
    }

    public static void setTinctures(Collection<ModifierDefinition> defs) {
        tinctures = byItem(defs);
    }

    public static void setCatalysts(Collection<ModifierDefinition> defs) {
        catalysts = byItem(defs);
    }

    private static Map<Item, ModifierDefinition> byItem(Collection<ModifierDefinition> defs) {
        Map<Item, ModifierDefinition> map = new HashMap<>();
        defs.forEach(d -> map.put(d.item(), d));
        return Collections.unmodifiableMap(map);
    }

    // --- Lookups ---

    public static Collection<EssenceStoneDefinition> stones() { return stones.values(); }
    public static Collection<ModifierDefinition> tinctures() { return tinctures.values(); }
    public static Collection<ModifierDefinition> catalysts() { return catalysts.values(); }

    public static Optional<EssenceStoneDefinition> getStone(ResourceLocation id) {
        return Optional.ofNullable(stones.get(id));
    }

    /** Resolves an essence stone item through its type tag. */
    public static Optional<EssenceStoneDefinition> getStone(ItemStack stack) {
        ResourceLocation type = EssenceStoneItem.typeOf(stack);
        return type == null ? Optional.empty() : getStone(type);
    }

    public static Optional<ModifierDefinition> getTincture(Item item) {
        return Optional.ofNullable(tinctures.get(item));
    }

    public static Optional<ModifierDefinition> getCatalyst(Item item) {
        return Optional.ofNullable(catalysts.get(item));
    }

    /** The tincture or catalyst definition for a stack, whichever it is. */
    public static Optional<ModifierDefinition> getModifier(ItemStack stack) {
        return switch (IngredientType.of(stack)) {
            case TINCTURE -> getTincture(stack.getItem());
            case CATALYST -> getCatalyst(stack.getItem());
            default -> Optional.empty();
        };
    }

    // --- Per-stack helpers ---

    /** Flask capacity this ingredient uses. Anything we don't recognise counts as 1. */
    public static int potency(ItemStack stack) {
        return switch (IngredientType.of(stack)) {
            case ESSENCE_STONE -> getStone(stack).map(EssenceStoneDefinition::potency).orElse(1);
            case TINCTURE -> getTincture(stack.getItem()).map(ModifierDefinition::potency).orElse(1);
            case CATALYST -> getCatalyst(stack.getItem()).map(ModifierDefinition::potency).orElse(1);
            case NONE -> 1;
        };
    }

    /**
     * The name to show for an ingredient. Tinctures and catalysts can override the vanilla
     * item name in their JSON; stones already resolve theirs in EssenceStoneItem.getName.
     */
    public static Component displayName(ItemStack stack) {
        return getModifier(stack).flatMap(ModifierDefinition::name)
                .<Component>map(Component::literal)
                .orElseGet(stack::getHoverName);
    }
}
