package net.silvertide.alchemical.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.registry.ItemRegistry;
import org.jetbrains.annotations.Nullable;

/** One item for every stone; the essence_stone_type NBT tag says which definition it is. */
public class EssenceStoneItem extends Item {
    public static final String TYPE_KEY = "essence_stone_type";

    public EssenceStoneItem() {
        super(new Item.Properties().stacksTo(64));
    }

    /** The definition id stored on a stone, or null if the stack isn't a stone. */
    public static @Nullable ResourceLocation typeOf(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().contains(TYPE_KEY)) return null;
        return ResourceLocation.tryParse(stack.getTag().getString(TYPE_KEY));
    }

    public static ItemStack withType(ResourceLocation type) {
        ItemStack stone = new ItemStack(ItemRegistry.ESSENCE_STONE.get());
        stone.getOrCreateTag().putString(TYPE_KEY, type.toString());
        return stone;
    }

    @Override
    public Component getName(ItemStack stack) {
        return IngredientManager.getStone(stack).flatMap(EssenceStoneDefinition::name)
                .<Component>map(Component::literal)
                .orElseGet(() -> super.getName(stack));
    }
}
