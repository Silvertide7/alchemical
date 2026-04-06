package net.silvertide.alchemical.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.silvertide.alchemical.Alchemical;

public final class TabRegistry {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Alchemical.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ALCHEMICAL_TAB =
            CREATIVE_MODE_TABS.register("alchemical_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ItemRegistry.ELIXIR.get()))
                    .title(Component.translatable("creativetab.alchemical_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ItemRegistry.ELIXIR.get());
                        output.accept(BlockRegistry.ATHANOR_ITEM.get());
                    })
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }

    private TabRegistry() {}
}
