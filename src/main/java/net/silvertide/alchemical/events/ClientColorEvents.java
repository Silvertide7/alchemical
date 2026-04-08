package net.silvertide.alchemical.events;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.client.EssenceStoneColorHandler;
import net.silvertide.alchemical.registry.ItemRegistry;

@EventBusSubscriber(modid = Alchemical.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientColorEvents {

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(new EssenceStoneColorHandler(), ItemRegistry.ESSENCE_STONE.get());
    }

    private ClientColorEvents() {}
}
