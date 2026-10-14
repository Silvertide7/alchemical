package net.silvertide.alchemical.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.client.screen.AthanorScreen;
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.registry.ItemRegistry;
import net.silvertide.alchemical.registry.MenuRegistry;

@EventBusSubscriber(modid = Alchemical.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistry.ATHANOR.get(), AthanorScreen::new);
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
