package net.silvertide.alchemical.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;
import net.silvertide.alchemical.data.ClientIngredientData;
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.registry.DataComponentRegistry;

import java.util.Optional;

public class EssenceStoneItem extends Item {
    public EssenceStoneItem() {
        super(new Item.Properties().stacksTo(64));
    }

    @Override
    public Component getName(ItemStack stack) {
        var stoneType = stack.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());
        if (stoneType == null) return super.getName(stack);

        Optional<String> customName = FMLEnvironment.dist.isClient()
                ? ClientIngredientData.getStone(stoneType).flatMap(EssenceStoneDefinition::name)
                : IngredientManager.getStone(stoneType).flatMap(EssenceStoneDefinition::name);

        return customName.<Component>map(Component::literal).orElseGet(() -> super.getName(stack));
    }
}
