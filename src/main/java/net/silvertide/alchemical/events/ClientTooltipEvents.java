package net.silvertide.alchemical.events;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.item.IngredientType;

/** Tags any item that a datapack has made an elixir ingredient with a line saying so. */
@Mod.EventBusSubscriber(modid = Alchemical.MODID, value = Dist.CLIENT)
public final class ClientTooltipEvents {
    private ClientTooltipEvents() {}

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        IngredientType type = IngredientType.of(event.getItemStack());
        if (type == IngredientType.NONE) return;
        event.getToolTip().add(Component.translatable("tooltip.alchemical.ingredient." + type.key())
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
