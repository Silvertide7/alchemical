package net.silvertide.alchemical.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.alchemical.Alchemical;

import java.util.List;
import java.util.function.Supplier;

public final class DataComponentRegistry {
    private static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Alchemical.MODID);

    // The base solutions loaded into the flask — contribute to effect duration and delivery.
    public static final Supplier<DataComponentType<List<ItemStack>>> TINCTURES =
            DATA_COMPONENTS.register("tinctures", () ->
                    DataComponentType.<List<ItemStack>>builder()
                            .persistent(ItemStack.CODEC.listOf())
                            .networkSynchronized(ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()))
                            .build());

    // The Essence Stones loaded into the flask — each stone contributes an effect type.
    // The combination of stones determines the final effect.
    public static final Supplier<DataComponentType<List<ItemStack>>> ESSENCE_STONES =
            DATA_COMPONENTS.register("essence_stones", () ->
                    DataComponentType.<List<ItemStack>>builder()
                            .persistent(ItemStack.CODEC.listOf())
                            .networkSynchronized(ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()))
                            .build());

    // The Catalysts loaded into the flask — modify potency, duration, or add secondary effects.
    public static final Supplier<DataComponentType<List<ItemStack>>> CATALYSTS =
            DATA_COMPONENTS.register("catalysts", () ->
                    DataComponentType.<List<ItemStack>>builder()
                            .persistent(ItemStack.CODEC.listOf())
                            .networkSynchronized(ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list()))
                            .build());

    // Which Essence Stone is currently active (index into ESSENCE_STONES list).
    // Only relevant when multiple stones are loaded. Cycled by shift+use.
    public static final Supplier<DataComponentType<Integer>> ACTIVE_STONE_INDEX =
            DATA_COMPONENTS.register("active_stone_index", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.INT)
                            .build());

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }

    private DataComponentRegistry() {}
}
