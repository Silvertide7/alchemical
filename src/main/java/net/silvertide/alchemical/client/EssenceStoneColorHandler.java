package net.silvertide.alchemical.client;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;
import net.silvertide.alchemical.data.ClientIngredientData;
import net.silvertide.alchemical.registry.DataComponentRegistry;

public class EssenceStoneColorHandler implements ItemColor {
    @Override
    public int getColor(ItemStack stack, int tintIndex) {
        if (tintIndex == 0) return 0xFFFFFFFF; // layer0 is the uncolored base — render as-is
        // layer1 is the overlay — tint it with the stone's color
        var stoneType = stack.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());
        if (stoneType == null) return 0xFFAAAAAA;
        return ClientIngredientData.getStone(stoneType)
                .map(def -> 0xFF000000 | def.color())
                .orElse(0xFFAAAAAA);
    }
}
