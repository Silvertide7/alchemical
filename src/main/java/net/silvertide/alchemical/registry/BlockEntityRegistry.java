package net.silvertide.alchemical.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.block.entity.AthanorBlockEntity;

public final class BlockEntityRegistry {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Alchemical.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AthanorBlockEntity>> ATHANOR =
            BLOCK_ENTITY_TYPES.register("athanor", () ->
                    BlockEntityType.Builder.of(AthanorBlockEntity::new, BlockRegistry.ATHANOR.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }

    private BlockEntityRegistry() {}
}
