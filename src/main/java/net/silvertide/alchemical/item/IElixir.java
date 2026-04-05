package net.silvertide.alchemical.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public interface IElixir {
    /** Max total number of ingredient slots (tinctures + stones + catalysts combined). */
    int getCapacity();

    /** Derives the effect to apply based on the mixture of ingredients currently loaded in the flask. */
    Optional<MobEffectInstance> deriveEffect(ItemStack stack);

    /** Returns the index of the currently active Essence Stone. */
    int getActiveStoneIndex(ItemStack stack);

    /** Returns how many Essence Stones are loaded into the flask. */
    int getStoneCount(ItemStack stack);

    /** Returns how many Tinctures are loaded into the flask. */
    int getTinctureCount(ItemStack stack);

    /** Returns the total number of ingredients currently loaded (tinctures + stones + catalysts). */
    int getLoadedCount(ItemStack stack);

    /** Returns true if the flask has the minimum required ingredients to be used (>=1 stone, >=1 tincture). */
    boolean isUsable(ItemStack stack);
}
