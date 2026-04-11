package net.silvertide.alchemical.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.silvertide.alchemical.data.ClientIngredientData;
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.item.IngredientType;
import net.silvertide.alchemical.records.CatalystDefinition;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.records.TinctureDefinition;
import net.silvertide.alchemical.registry.DataComponentRegistry;

import java.util.Optional;

public final class IngredientUtil {
    private IngredientUtil() {}

    // ── Dist-aware ingredient lookups ────────────────────────────────────────

    /** Looks up a stone definition by id, using the correct store for the current dist. */
    public static Optional<EssenceStoneDefinition> getStone(ResourceLocation id) {
        return FMLEnvironment.dist.isClient()
                ? ClientIngredientData.getStone(id)
                : IngredientManager.getStone(id);
    }

    /** Looks up a tincture definition by item, using the correct store for the current dist. */
    public static Optional<TinctureDefinition> getTincture(Item item) {
        return FMLEnvironment.dist.isClient()
                ? ClientIngredientData.getTincture(item)
                : IngredientManager.getTincture(item);
    }

    /** Looks up a catalyst definition by item, using the correct store for the current dist. */
    public static Optional<CatalystDefinition> getCatalyst(Item item) {
        return FMLEnvironment.dist.isClient()
                ? ClientIngredientData.getCatalyst(item)
                : IngredientManager.getCatalyst(item);
    }

    /**
     * Returns how many elixir capacity slots this ingredient consumes.
     * Uses ClientIngredientData on the client, IngredientManager on the server.
     * Defaults to 1 if the definition is not found.
     */
    public static int getPotency(ItemStack stack) {
        IngredientType type = IngredientType.of(stack);
        return switch (type) {
            case ESSENCE_STONE -> {
                var stoneType = stack.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());
                if (stoneType == null) yield 1;
                yield getStone(stoneType).map(EssenceStoneDefinition::potency).orElse(1);
            }
            case TINCTURE -> getTincture(stack.getItem()).map(TinctureDefinition::potency).orElse(1);
            case CATALYST -> getCatalyst(stack.getItem()).map(CatalystDefinition::potency).orElse(1);
            default -> 1;
        };
    }
}
