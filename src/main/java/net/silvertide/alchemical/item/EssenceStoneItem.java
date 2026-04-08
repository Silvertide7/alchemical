package net.silvertide.alchemical.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
        return lookupName(stoneType)
                .<Component>map(Component::literal)
                .orElseGet(() -> super.getName(stack));
    }

    // IngredientManager is populated server-side by datapack loaders.
    // ClientIngredientData is populated client-side via the sync packet.
    // Neither has @OnlyIn restrictions, so both are classloader-safe on either side.
    private static Optional<String> lookupName(ResourceLocation stoneType) {
        if (FMLEnvironment.dist.isClient()) {
            return ClientIngredientData.getStone(stoneType).flatMap(EssenceStoneDefinition::name);
        }
        return IngredientManager.getStone(stoneType).flatMap(EssenceStoneDefinition::name);
    }
}
