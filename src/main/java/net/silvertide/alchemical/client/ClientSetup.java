package net.silvertide.alchemical.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.client.screen.AthanorScreen;
import net.silvertide.alchemical.registry.MenuRegistry;

@EventBusSubscriber(modid = Alchemical.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistry.ATHANOR.get(), AthanorScreen::new);
    }

    private ClientSetup() {}
}
