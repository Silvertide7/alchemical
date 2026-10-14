package net.silvertide.alchemical.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.silvertide.alchemical.attachments.ElixirCooldown;
import net.silvertide.alchemical.client.ClientElixirCooldownData;
import net.silvertide.alchemical.config.AlchemicalConfig;
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.registry.DataComponentRegistry;
import net.silvertide.alchemical.util.ElixirModifiers;
import net.silvertide.alchemical.util.FormattingUtil;

import java.util.List;
import java.util.Optional;

/**
 * A reusable flask. Stones, tinctures and catalysts are loaded in at the Athanor and live
 * on the stack as data components. Drinking applies the active stone's effect and starts
 * a global cooldown; shift-use cycles which stone is active.
 */
public class ElixirItem extends Item {
    private static final int DRINK_TICKS = 32;

    public ElixirItem() {
        super(new Item.Properties().stacksTo(1));
    }

    // --- What's in the flask ---

    public static List<ItemStack> stones(ItemStack flask) {
        return flask.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());
    }

    public static List<ItemStack> tinctures(ItemStack flask) {
        return flask.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of());
    }

    public static List<ItemStack> catalysts(ItemStack flask) {
        return flask.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.of());
    }

    public static int capacity() {
        return AlchemicalConfig.ELIXIR_CAPACITY.get();
    }

    /** Total potency of everything loaded. */
    public static int loadedCount(ItemStack flask) {
        int total = 0;
        for (ItemStack s : stones(flask)) total += IngredientManager.potency(s);
        for (ItemStack s : tinctures(flask)) total += IngredientManager.potency(s);
        for (ItemStack s : catalysts(flask)) total += IngredientManager.potency(s);
        return total;
    }

    public static boolean isFull(ItemStack flask) {
        return loadedCount(flask) >= capacity();
    }

    /** Needs at least one stone and one tincture before it does anything. */
    public static boolean isUsable(ItemStack flask) {
        return !stones(flask).isEmpty() && !tinctures(flask).isEmpty();
    }

    public static int activeStoneIndex(ItemStack flask) {
        int count = stones(flask).size();
        int index = flask.getOrDefault(DataComponentRegistry.ACTIVE_STONE_INDEX.get(), 0);
        return count == 0 ? 0 : Math.min(index, count - 1);
    }

    public static Optional<EssenceStoneDefinition> activeStone(ItemStack flask) {
        List<ItemStack> stones = stones(flask);
        return stones.isEmpty() ? Optional.empty() : IngredientManager.getStone(stones.get(activeStoneIndex(flask)));
    }

    // --- Drinking ---

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack flask = player.getItemInHand(hand);
        long now = level.getGameTime();

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide() && stones(flask).size() > 1) cycleActiveStone(player, flask);
            return InteractionResultHolder.success(flask);
        }

        if (!isUsable(flask)) {
            if (level.isClientSide()) tellOnce(player, now, Component.translatable("message.alchemical.flask_not_ready"));
            return InteractionResultHolder.fail(flask);
        }

        // The client answers from its synced copy so the message is instant; the server has the real say.
        if (level.isClientSide()) {
            ElixirCooldown cooldown = ClientElixirCooldownData.get();
            if (cooldown.isActive(now)) {
                tellOnce(player, now, Component.translatable("message.alchemical.on_cooldown",
                        FormattingUtil.secondsToTime(cooldown.remainingSeconds(now))));
                return InteractionResultHolder.fail(flask);
            }
        } else if (ElixirCooldown.isOnCooldown(player)) {
            return InteractionResultHolder.fail(flask);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(flask);
    }

    private static void tellOnce(Player player, long now, Component message) {
        if (ClientElixirCooldownData.tryMarkMessageSent(now)) player.sendSystemMessage(message);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack flask, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            activeStone(flask).ifPresent(stone -> {
                ElixirModifiers mods = ElixirModifiers.of(flask);
                player.addEffect(new MobEffectInstance(stone.effect(), mods.duration(stone), mods.level(stone) - 1, false, false, true));
                ElixirCooldown.set(player, new ElixirCooldown(level.getGameTime(), mods.cooldown(stone)));
            });
        }
        return flask;
    }

    private static void cycleActiveStone(Player player, ItemStack flask) {
        List<ItemStack> stones = stones(flask);
        int next = (activeStoneIndex(flask) + 1) % stones.size();
        flask.set(DataComponentRegistry.ACTIVE_STONE_INDEX.get(), next);
        player.sendSystemMessage(Component.translatable("tooltip.alchemical.stone_switched", stones.get(next).getHoverName()));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    // --- Tooltip ---

    @Override
    public void appendHoverText(ItemStack flask, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        List<ItemStack> stones = stones(flask);
        int active = activeStoneIndex(flask);

        // Screen is client-only, but so is this method.
        if (!Screen.hasShiftDown()) {
            lines.add(stones.isEmpty()
                    ? Component.translatable("tooltip.alchemical.empty_flask")
                    : Component.translatable("tooltip.alchemical.active_stone", stones.get(active).getHoverName()));
            lines.add(Component.translatable("tooltip.alchemical.elixir_hint"));
            return;
        }

        if (stones.isEmpty()) {
            lines.add(Component.translatable("tooltip.alchemical.empty_flask"));
            return;
        }

        activeStone(flask).ifPresent(stone -> {
            ElixirModifiers mods = ElixirModifiers.of(flask);
            lines.add(Component.translatable("tooltip.alchemical.effect",
                    FormattingUtil.effectName(stone.effect(), mods.level(stone))).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.alchemical.duration",
                    FormattingUtil.ticksToTime(mods.duration(stone))).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.alchemical.cooldown",
                    FormattingUtil.secondsToTime(mods.cooldown(stone))).withStyle(ChatFormatting.GRAY));
        });

        if (stones.size() > 1) lines.add(Component.empty());
        for (int i = 0; i < stones.size(); i++) {
            lines.add(stones.get(i).getHoverName().copy()
                    .withStyle(i == active ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        }
        if (stones.size() > 1) {
            lines.add(Component.translatable("tooltip.alchemical.shift_to_switch")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
