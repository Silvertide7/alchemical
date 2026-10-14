package net.silvertide.alchemical.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.block.AthanorBlock;

public final class BlockRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Alchemical.MODID);

    public static final RegistryObject<AthanorBlock> ATHANOR = BLOCKS.register("athanor",
            () -> new AthanorBlock(BlockBehaviour.Properties.of()
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .lightLevel(state -> 13)));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }

    private BlockRegistry() {}
}
