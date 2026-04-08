package net.silvertide.alchemical.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.item.ElixirItem;
import net.silvertide.alchemical.item.EssenceStoneItem;

public final class ItemRegistry {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Alchemical.MODID);

    public static final DeferredHolder<Item, ElixirItem> ELIXIR = ITEMS.register("elixir",
            () -> new ElixirItem(3, 10));

    public static final DeferredHolder<Item, EssenceStoneItem> ESSENCE_STONE = ITEMS.register("essence_stone",
            EssenceStoneItem::new);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

    private ItemRegistry() {}
}
