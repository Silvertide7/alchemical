package net.silvertide.alchemical.item;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.silvertide.alchemical.client.ClientElixirCooldownData;
import net.silvertide.alchemical.registry.DataComponentRegistry;
import net.silvertide.alchemical.util.ElixirAttachmentUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class ElixirItem extends Item implements IElixir {
    private static final int DRINK_DURATION_TICKS = 32;

    private final int capacity;
    private final int cooldownSeconds;

    public ElixirItem(int capacity, int cooldownSeconds) {
        super(new Item.Properties().stacksTo(1));
        this.capacity = capacity;
        this.cooldownSeconds = cooldownSeconds;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide() && getStoneCount(stack) > 1) {
                cycleActiveStone(player, stack);
            }
            return InteractionResultHolder.success(stack);
        }

        if (!isUsable(stack)) {
            if (level.isClientSide() && ClientElixirCooldownData.tryMarkMessageSent(level.getGameTime())) {
                player.sendSystemMessage(Component.translatable("message.alchemical.flask_not_ready"));
            }
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide()) {
            if (ClientElixirCooldownData.isOnCooldown(level.getGameTime())) {
                if (ClientElixirCooldownData.tryMarkMessageSent(level.getGameTime())) {
                    player.sendSystemMessage(Component.translatable("message.alchemical.on_cooldown",
                            ClientElixirCooldownData.getRemainingSeconds(level.getGameTime())));
                }
                return InteractionResultHolder.fail(stack);
            }
        } else {
            if (ElixirAttachmentUtil.isOnCooldown(player)) {
                return InteractionResultHolder.fail(stack);
            }
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            // TODO: Replace with deriveEffect(stack) once the formula system is implemented
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            ElixirAttachmentUtil.applyNewCooldown(player, cooldownSeconds);
        }
        // Return the same stack unmodified — elixir is never consumed
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK_DURATION_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public Optional<MobEffectInstance> deriveEffect(ItemStack stack) {
        List<ItemStack> tinctures = stack.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of());
        List<ItemStack> stones = stack.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());
        List<ItemStack> catalysts = stack.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.of());

        if (stones.isEmpty() || tinctures.isEmpty()) return Optional.empty();

        ItemStack activeStone = stones.get(getActiveStoneIndex(stack));

        // TODO: Map activeStone item -> MobEffect
        // TODO: Derive duration from tincture combination
        // TODO: Derive amplifier/secondary effects from catalyst combination
        return Optional.empty();
    }

    @Override
    public int getActiveStoneIndex(ItemStack stack) {
        int index = stack.getOrDefault(DataComponentRegistry.ACTIVE_STONE_INDEX.get(), 0);
        int count = getStoneCount(stack);
        // Clamp in case a stone was removed and the saved index is now out of bounds
        return count > 0 ? Math.min(index, count - 1) : 0;
    }

    @Override
    public int getStoneCount(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of()).size();
    }

    @Override
    public int getTinctureCount(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of()).size();
    }

    @Override
    public int getLoadedCount(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of()).size()
                + stack.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of()).size()
                + stack.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.of()).size();
    }

    @Override
    public boolean isUsable(ItemStack stack) {
        return getStoneCount(stack) >= 1 && getTinctureCount(stack) >= 1;
    }

    private void cycleActiveStone(Player player, ItemStack stack) {
        int next = (getActiveStoneIndex(stack) + 1) % getStoneCount(stack);
        stack.set(DataComponentRegistry.ACTIVE_STONE_INDEX.get(), next);
        player.getInventory().setChanged();

        List<ItemStack> stones = stack.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());
        player.sendSystemMessage(Component.translatable("tooltip.alchemical.stone_switched",
                stones.get(next).getHoverName()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            List<ItemStack> tinctures = stack.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of());
            List<ItemStack> stones = stack.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());
            List<ItemStack> catalysts = stack.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.of());

            tooltipComponents.add(Component.translatable("tooltip.alchemical.capacity",
                    getLoadedCount(stack), capacity));

            if (tinctures.isEmpty() && stones.isEmpty() && catalysts.isEmpty()) {
                tooltipComponents.add(Component.translatable("tooltip.alchemical.empty_flask"));
            } else {
                tinctures.forEach(t -> tooltipComponents.add(
                        Component.translatable("tooltip.alchemical.tincture", t.getHoverName())));

                int activeIndex = getActiveStoneIndex(stack);
                for (int i = 0; i < stones.size(); i++) {
                    Component name = stones.get(i).getHoverName();
                    if (i == activeIndex) {
                        tooltipComponents.add(Component.translatable("tooltip.alchemical.essence_stone_active", name));
                    } else {
                        tooltipComponents.add(Component.translatable("tooltip.alchemical.essence_stone", name));
                    }
                }
                if (stones.size() > 1) {
                    tooltipComponents.add(Component.translatable("tooltip.alchemical.shift_to_switch"));
                }

                catalysts.forEach(c -> tooltipComponents.add(
                        Component.translatable("tooltip.alchemical.catalyst", c.getHoverName())));
            }
        } else {
            tooltipComponents.add(Component.translatable("tooltip.alchemical.elixir_hint"));
        }
        super.appendHoverText(stack, context, tooltipComponents, flag);
    }
}
