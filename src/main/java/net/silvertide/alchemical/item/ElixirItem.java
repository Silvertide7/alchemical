package net.silvertide.alchemical.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.silvertide.alchemical.config.AlchemicalConfig;
import net.silvertide.alchemical.data.ClientIngredientData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.silvertide.alchemical.client.ClientElixirCooldownData;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.registry.DataComponentRegistry;
import net.silvertide.alchemical.util.ElixirAttachmentUtil;
import net.silvertide.alchemical.util.ElixirCalcUtil;
import net.silvertide.alchemical.util.FormattingUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ElixirItem extends Item implements IElixir {
    private static final int DRINK_DURATION_TICKS = 32;

    public ElixirItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public int getCapacity() {
        return AlchemicalConfig.ELIXIR_CAPACITY.get();
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
                            formatTime(ClientElixirCooldownData.getRemainingSeconds(level.getGameTime()))));
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
            List<MobEffectInstance> effects = deriveEffect(stack);
            effects.forEach(player::addEffect);

            int effectiveCooldown = computeCooldown(stack);
            ElixirAttachmentUtil.applyNewCooldown(player, effectiveCooldown);
        }
        return stack;
    }

    /**
     * Computes the effective cooldown in seconds for this elixir, combining the active stone's
     * cooldown modifier with all tincture/catalyst modifiers.
     */
    private int computeCooldown(ItemStack stack) {
        List<ItemStack> stoneStacks = stack.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());
        List<ItemStack> tinctureStacks = stack.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of());
        List<ItemStack> catalystStacks = stack.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.of());

        ElixirCalcUtil.ModifierResult mods = ElixirCalcUtil.computeSharedModifiers(tinctureStacks, catalystStacks);

        if (!stoneStacks.isEmpty()) {
            ItemStack activeStoneStack = stoneStacks.get(getActiveStoneIndex(stack));
            Optional<EssenceStoneDefinition> stoneDef = ElixirCalcUtil.resolveStone(activeStoneStack);
            if (stoneDef.isPresent()) {
                return ElixirCalcUtil.computeEffectiveCooldown(getCooldownSeconds(), stoneDef.get(), mods);
            }
        }

        // Fallback: apply only tincture/catalyst cooldown modifiers (no stone modifier)
        return Math.max(0, (int) ((getCooldownSeconds() + mods.cooldownFlat()) * mods.cooldownMult()));
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
    public List<MobEffectInstance> deriveEffect(ItemStack stack) {
        List<ItemStack> tinctures = stack.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of());
        List<ItemStack> stones = stack.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());
        List<ItemStack> catalysts = stack.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.of());

        if (stones.isEmpty() || tinctures.isEmpty()) return List.of();

        ItemStack activeStoneStack = stones.get(getActiveStoneIndex(stack));
        Optional<EssenceStoneDefinition> stoneDef = ElixirCalcUtil.resolveStone(activeStoneStack);
        if (stoneDef.isEmpty()) return List.of();

        EssenceStoneDefinition stone = stoneDef.get();
        ElixirCalcUtil.ModifierResult mods = ElixirCalcUtil.computeSharedModifiers(tinctures, catalysts);

        int finalDuration = ElixirCalcUtil.computeEffectiveDuration(stone, mods);
        int finalAmplifier = Math.max(0, (stone.baseLevel() - 1) + mods.levelMod());

        return BuiltInRegistries.MOB_EFFECT.getOptional(stone.effect())
                .map(effect -> new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), finalDuration, finalAmplifier, false, false, true))
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public int getActiveStoneIndex(ItemStack stack) {
        int index = stack.getOrDefault(DataComponentRegistry.ACTIVE_STONE_INDEX.get(), 0);
        int count = getStoneCount(stack);
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
        int total = 0;
        for (ItemStack s : stack.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.<ItemStack>of()))
            total += net.silvertide.alchemical.util.IngredientUtil.getPotency(s);
        for (ItemStack s : stack.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.<ItemStack>of()))
            total += net.silvertide.alchemical.util.IngredientUtil.getPotency(s);
        for (ItemStack s : stack.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.<ItemStack>of()))
            total += net.silvertide.alchemical.util.IngredientUtil.getPotency(s);
        return total;
    }

    @Override
    public boolean isUsable(ItemStack stack) {
        return getStoneCount(stack) >= 1 && getTinctureCount(stack) >= 1;
    }

    @Override
    public int getCooldownSeconds() {
        return AlchemicalConfig.ELIXIR_COOLDOWN_SECONDS.get();
    }

    // ── Tooltip ──────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag flag) {
        if (Screen.hasShiftDown()) {
            buildShiftTooltip(stack, tooltipComponents);
        } else {
            buildDefaultTooltip(stack, tooltipComponents);
        }
        super.appendHoverText(stack, context, tooltipComponents, flag);
    }

    /** Detailed view: computed stats for the active stone + stone list. */
    private void buildShiftTooltip(ItemStack stack, List<Component> tooltipComponents) {
        List<ItemStack> stones = stack.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());

        if (stones.isEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.alchemical.empty_flask"));
            return;
        }

        List<ItemStack> tinctures = stack.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of());
        List<ItemStack> catalysts = stack.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.of());
        int activeIndex = getActiveStoneIndex(stack);
        ItemStack activeStoneStack = stones.get(activeIndex);

        ElixirCalcUtil.resolveStone(activeStoneStack).ifPresent(def -> {
            ElixirCalcUtil.ModifierResult mods = ElixirCalcUtil.computeSharedModifiers(tinctures, catalysts);

            int finalDuration = ElixirCalcUtil.computeEffectiveDuration(def, mods);
            int finalLevel = ElixirCalcUtil.computeEffectiveLevel(def, mods);
            int finalCooldown = ElixirCalcUtil.computeEffectiveCooldown(getCooldownSeconds(), def, mods);

            String effectName = BuiltInRegistries.MOB_EFFECT.getOptional(def.effect())
                    .map(e -> e.getDisplayName().getString())
                    .orElse("Unknown");

            tooltipComponents.add(Component.literal("Effect: " + effectName + " " + toRoman(finalLevel))
                    .withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("Duration: " + formatTicks(finalDuration))
                    .withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("Cooldown: " + formatTime(finalCooldown))
                    .withStyle(ChatFormatting.GRAY));
        });

        // List all stones
        if (stones.size() > 1) {
            tooltipComponents.add(Component.empty());
        }
        for (int i = 0; i < stones.size(); i++) {
            Component name = getIngredientDisplayName(stones.get(i), IngredientType.ESSENCE_STONE);
            if (i == activeIndex) {
                tooltipComponents.add(Component.literal(name.getString())
                        .withStyle(ChatFormatting.GOLD));
            } else {
                tooltipComponents.add(Component.literal(name.getString())
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (stones.size() > 1) {
            tooltipComponents.add(Component.translatable("tooltip.alchemical.shift_to_switch")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    /** Default view: active stone name + shift hint. */
    private void buildDefaultTooltip(ItemStack stack, List<Component> tooltipComponents) {
        List<ItemStack> stones = stack.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());

        if (!stones.isEmpty()) {
            int activeIndex = getActiveStoneIndex(stack);
            ItemStack activeStone = stones.get(activeIndex);
            tooltipComponents.add(Component.translatable("tooltip.alchemical.active_stone",
                    getIngredientDisplayName(activeStone, IngredientType.ESSENCE_STONE)));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.alchemical.empty_flask"));
        }
        tooltipComponents.add(Component.translatable("tooltip.alchemical.elixir_hint"));
    }

    // ── Display name resolution (client-side) ────────────────────────────────

    private static Component getIngredientDisplayName(ItemStack stack, IngredientType type) {
        Optional<String> customName = switch (type) {
            case TINCTURE -> ClientIngredientData.getTincture(stack.getItem()).flatMap(d -> d.name());
            case ESSENCE_STONE -> {
                var stoneType = stack.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());
                yield stoneType != null
                        ? ClientIngredientData.getStone(stoneType).flatMap(EssenceStoneDefinition::name)
                        : Optional.empty();
            }
            case CATALYST -> ClientIngredientData.getCatalyst(stack.getItem()).flatMap(d -> d.name());
            default -> Optional.empty();
        };
        return customName.<Component>map(Component::literal).orElseGet(stack::getHoverName);
    }

    // ── Stone cycling ────────────────────────────────────────────────────────

    private void cycleActiveStone(Player player, ItemStack stack) {
        int next = (getActiveStoneIndex(stack) + 1) % getStoneCount(stack);
        stack.set(DataComponentRegistry.ACTIVE_STONE_INDEX.get(), next);
        player.getInventory().setChanged();

        List<ItemStack> stones = stack.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());
        player.sendSystemMessage(Component.translatable("tooltip.alchemical.stone_switched",
                stones.get(next).getHoverName()));
    }

    // ── Formatting helpers (delegated to FormattingUtil) ────────────────────

    private static String formatTicks(int ticks) {
        return FormattingUtil.ticksToTime(ticks);
    }

    private static String toRoman(int n) {
        return FormattingUtil.toRoman(n);
    }

    private static String formatTime(int totalSeconds) {
        return FormattingUtil.secondsToTime(totalSeconds);
    }
}
