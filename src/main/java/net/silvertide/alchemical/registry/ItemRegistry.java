package net.silvertide.alchemical.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.item.ElixirItem;
import net.silvertide.alchemical.item.EssenceStoneItem;

public final class ItemRegistry {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Alchemical.MODID);

    public static final RegistryObject<ElixirItem> ELIXIR = ITEMS.register("elixir", ElixirItem::new);
    public static final RegistryObject<EssenceStoneItem> ESSENCE_STONE = ITEMS.register("essence_stone", EssenceStoneItem::new);
    public static final RegistryObject<BlockItem> ATHANOR = ITEMS.register("athanor",
            () -> new BlockItem(BlockRegistry.ATHANOR.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private ItemRegistry() {}
}
