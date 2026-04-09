package net.silvertide.alchemical.util;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.silvertide.alchemical.data.ClientIngredientData;
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.item.IngredientType;
import net.silvertide.alchemical.records.CatalystDefinition;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.records.TinctureDefinition;
import net.silvertide.alchemical.registry.DataComponentRegistry;

public final class IngredientUtil {
    private IngredientUtil() {}

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
                var opt = FMLEnvironment.dist.isClient()
                        ? ClientIngredientData.getStone(stoneType)
                        : IngredientManager.getStone(stoneType);
                yield opt.map(EssenceStoneDefinition::potency).orElse(1);
            }
            case TINCTURE -> {
                var opt = FMLEnvironment.dist.isClient()
                        ? ClientIngredientData.getTincture(stack.getItem())
                        : IngredientManager.getTincture(stack.getItem());
                yield opt.map(TinctureDefinition::potency).orElse(1);
            }
            case CATALYST -> {
                var opt = FMLEnvironment.dist.isClient()
                        ? ClientIngredientData.getCatalyst(stack.getItem())
                        : IngredientManager.getCatalyst(stack.getItem());
                yield opt.map(CatalystDefinition::potency).orElse(1);
            }
            default -> 1;
        };
    }
}
