package net.silvertide.alchemical.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.block.AthanorBlock;

public final class BlockRegistry {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, Alchemical.MODID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, Alchemical.MODID);

    public static final DeferredHolder<Block, AthanorBlock> ATHANOR = BLOCKS.register("athanor",
            () -> new AthanorBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()));

    public static final DeferredHolder<Item, BlockItem> ATHANOR_ITEM = ITEMS.register("athanor",
            () -> new BlockItem(ATHANOR.get(), new Item.Properties()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
    }

    private BlockRegistry() {}
}
