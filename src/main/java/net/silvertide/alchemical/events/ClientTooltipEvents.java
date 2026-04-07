package net.silvertide.alchemical.events;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.data.ClientIngredientData;

@EventBusSubscriber(modid = Alchemical.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class ClientTooltipEvents {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        var item = event.getItemStack().getItem();
        boolean isIngredient = ClientIngredientData.getStone(item).isPresent()
                || ClientIngredientData.getTincture(item).isPresent()
                || ClientIngredientData.getCatalyst(item).isPresent();

        if (isIngredient) {
            event.getToolTip().add(Component.translatable("tooltip.alchemical.usable_in_elixir")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }
    }

    private ClientTooltipEvents() {}
}
