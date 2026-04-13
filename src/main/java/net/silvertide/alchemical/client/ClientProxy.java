package net.silvertide.alchemical.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.silvertide.alchemical.data.ClientIngredientData;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.registry.DataComponentRegistry;

import java.util.Optional;

/**
 * Client-side proxy for calls that reference client-only classes.
 * Methods here are ONLY invoked when FMLEnvironment.dist.isClient() or
 * level.isClientSide() is true, so this class is never loaded on a dedicated server.
 */
public final class ClientProxy {

    public static boolean isShiftDown() {
        return Screen.hasShiftDown();
    }

    public static boolean isOnCooldown(long gameTime) {
        return ClientElixirCooldownData.isOnCooldown(gameTime);
    }

    public static boolean tryMarkMessageSent(long gameTime) {
        return ClientElixirCooldownData.tryMarkMessageSent(gameTime);
    }

    public static int getRemainingSeconds(long gameTime) {
        return ClientElixirCooldownData.getRemainingSeconds(gameTime);
    }

    /**
     * Resolves a display name for an ingredient using client-side ingredient data.
     */
    public static Optional<String> getIngredientName(ItemStack stack, String type) {
        return switch (type) {
            case "TINCTURE" -> ClientIngredientData.getTincture(stack.getItem()).flatMap(d -> d.name());
            case "ESSENCE_STONE" -> {
                ResourceLocation stoneType = stack.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());
                yield stoneType != null
                        ? ClientIngredientData.getStone(stoneType).flatMap(EssenceStoneDefinition::name)
                        : Optional.empty();
            }
            case "CATALYST" -> ClientIngredientData.getCatalyst(stack.getItem()).flatMap(d -> d.name());
            default -> Optional.empty();
        };
    }

    private ClientProxy() {}
}
