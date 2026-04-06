package net.silvertide.alchemical.events;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
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
        ItemStack stack = event.getItemStack();

        if (ClientIngredientData.getStone(stack.getItem()).isPresent()
                || ClientIngredientData.getTincture(stack.getItem()).isPresent()
                || ClientIngredientData.getCatalyst(stack.getItem()).isPresent()) {
            event.getToolTip().add(Component.translatable("tooltip.alchemical.usable_in_elixir")
                    .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        }
    }

    private ClientTooltipEvents() {}
}
