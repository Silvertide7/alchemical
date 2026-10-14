package net.silvertide.alchemical.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.item.EssenceStoneItem;

public final class TabRegistry {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Alchemical.MODID);

    public static final RegistryObject<CreativeModeTab> ALCHEMICAL_TAB =
            CREATIVE_MODE_TABS.register("alchemical_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ItemRegistry.ELIXIR.get()))
                    .title(Component.translatable("creativetab.alchemical_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ItemRegistry.ELIXIR.get());
                        // One stone item per loaded definition
                        for (var def : IngredientManager.stones()) {
                            output.accept(EssenceStoneItem.withType(def.id()));
                        }
                        output.accept(ItemRegistry.ATHANOR.get());
                    })
                    .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }

    private TabRegistry() {}
}
