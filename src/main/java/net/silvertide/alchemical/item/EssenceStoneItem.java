package net.silvertide.alchemical.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.registry.DataComponentRegistry;
import net.silvertide.alchemical.util.IngredientUtil;

import java.util.Optional;

public class EssenceStoneItem extends Item {
    public EssenceStoneItem() {
        super(new Item.Properties().stacksTo(64));
    }

    @Override
    public Component getName(ItemStack stack) {
        var stoneType = stack.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());
        if (stoneType == null) return super.getName(stack);
        return lookupName(stoneType)
                .<Component>map(Component::literal)
                .orElseGet(() -> super.getName(stack));
    }

    private static Optional<String> lookupName(ResourceLocation stoneType) {
        return IngredientUtil.getStone(stoneType).flatMap(EssenceStoneDefinition::name);
    }
}
