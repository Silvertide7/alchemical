package net.silvertide.alchemical.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.alchemical.Alchemical;

import java.util.List;
import java.util.function.Supplier;

public final class DataComponentRegistry {
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Alchemical.MODID);

    // What's loaded into a flask, one list per ingredient kind.
    public static final Supplier<DataComponentType<List<ItemStack>>> ESSENCE_STONES = stackList("essence_stones");
    public static final Supplier<DataComponentType<List<ItemStack>>> TINCTURES = stackList("tinctures");
    public static final Supplier<DataComponentType<List<ItemStack>>> CATALYSTS = stackList("catalysts");

    // On an essence stone item: the id of its datapack definition (the JSON file name).
    public static final Supplier<DataComponentType<ResourceLocation>> ESSENCE_STONE_TYPE =
            DATA_COMPONENTS.register("essence_stone_type", () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    // On a flask: index into ESSENCE_STONES of the stone that gets drunk. Cycled with shift-use.
    public static final Supplier<DataComponentType<Integer>> ACTIVE_STONE_INDEX =
            DATA_COMPONENTS.register("active_stone_index", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    private static Supplier<DataComponentType<List<ItemStack>>> stackList(String name) {
        return DATA_COMPONENTS.register(name, () -> DataComponentType.<List<ItemStack>>builder()
                .persistent(ItemStack.CODEC.listOf())
                .networkSynchronized(ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()))
                .build());
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }

    private DataComponentRegistry() {}
}
