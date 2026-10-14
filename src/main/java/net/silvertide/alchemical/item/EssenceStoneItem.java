package net.silvertide.alchemical.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.records.EssenceStoneDefinition;

/** One item for every stone; the ESSENCE_STONE_TYPE component says which definition it is. */
public class EssenceStoneItem extends Item {
    public EssenceStoneItem() {
        super(new Item.Properties().stacksTo(64));
    }

    @Override
    public Component getName(ItemStack stack) {
        return IngredientManager.getStone(stack).flatMap(EssenceStoneDefinition::name)
                .<Component>map(Component::literal)
                .orElseGet(() -> super.getName(stack));
    }
}
