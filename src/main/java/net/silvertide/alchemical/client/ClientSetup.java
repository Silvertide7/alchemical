package net.silvertide.alchemical.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.client.screen.AthanorScreen;
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.registry.ItemRegistry;
import net.silvertide.alchemical.registry.MenuRegistry;

@Mod.EventBusSubscriber(modid = Alchemical.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(MenuRegistry.ATHANOR.get(), AthanorScreen::new));
    }

    // Essence stones are one grey base texture (layer 0) plus an overlay (layer 1) tinted per definition.
    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            if (tintIndex == 0) return 0xFFFFFFFF;
            return IngredientManager.getStone(stack).map(def -> 0xFF000000 | def.color()).orElse(0xFFAAAAAA);
        }, ItemRegistry.ESSENCE_STONE.get());
    }
}
