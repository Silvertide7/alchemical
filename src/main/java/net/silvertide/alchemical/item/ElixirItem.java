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
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.records.CatalystDefinition;
import net.silvertide.alchemical.records.EssenceStoneDefinition;
import net.silvertide.alchemical.records.TinctureDefinition;
import net.silvertide.alchemical.registry.DataComponentRegistry;
import net.silvertide.alchemical.util.ElixirAttachmentUtil;
import net.silvertide.alchemical.util.IngredientUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ElixirItem extends Item implements IElixir {
    private static final int DRINK_DURATION_TICKS = 32;

    private final int capacity;

    public ElixirItem(int capacity) {
        super(new Item.Properties().stacksTo(1));
        this.capacity = capacity;
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

            // Aggregate cooldown modifiers from all loaded ingredients
            List<ItemStack> catalystStacks = stack.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.of());
            List<ItemStack> tinctureStacks = stack.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of());
            List<ItemStack> stoneStacks = stack.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());

            // Accumulate cooldown modifiers — use array wrappers for use in lambdas
            float[] cooldownMult = {1.0f};
            int[] flatCooldown = {0};

            // Active stone contributes its cooldown modifier
            if (!stoneStacks.isEmpty()) {
                ItemStack activeStoneStack = stoneStacks.get(getActiveStoneIndex(stack));
                var stoneType = activeStoneStack.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());
                if (stoneType != null) {
                    IngredientManager.getStone(stoneType).ifPresent(def -> {
                        cooldownMult[0] *= def.elixirCooldownMultiplier();
                        flatCooldown[0] += def.elixirCooldownFlat();
                    });
                }
            }

            for (ItemStack tinctureStack : tinctureStacks) {
                IngredientManager.getTincture(tinctureStack.getItem()).ifPresent(def -> {
                    cooldownMult[0] *= def.elixirCooldownMultiplier();
                    flatCooldown[0] += def.elixirCooldownFlat();
                });
            }
            for (ItemStack catalystStack : catalystStacks) {
                IngredientManager.getCatalyst(catalystStack.getItem()).ifPresent(def -> {
                    cooldownMult[0] *= def.elixirCooldownMultiplier();
                    flatCooldown[0] += def.elixirCooldownFlat();
                });
            }

            int effectiveCooldown = Math.max(0, (int)((getCooldownSeconds() + flatCooldown[0]) * cooldownMult[0]));
            ElixirAttachmentUtil.applyNewCooldown(player, effectiveCooldown);
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
    public List<MobEffectInstance> deriveEffect(ItemStack stack) {
        List<ItemStack> tinctures = stack.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of());
        List<ItemStack> stones = stack.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());
        List<ItemStack> catalysts = stack.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.of());

        if (stones.isEmpty() || tinctures.isEmpty()) return List.of();

        ItemStack activeStoneStack = stones.get(getActiveStoneIndex(stack));
        var stoneType = activeStoneStack.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());
        if (stoneType == null) return List.of();
        Optional<EssenceStoneDefinition> stoneDef = IngredientManager.getStone(stoneType);
        if (stoneDef.isEmpty()) return List.of();

        EssenceStoneDefinition stone = stoneDef.get();

        // Collect tincture and catalyst definitions
        List<TinctureDefinition> tinctureDefinitions = new ArrayList<>();
        for (ItemStack tinctureStack : tinctures) {
            IngredientManager.getTincture(tinctureStack.getItem()).ifPresent(tinctureDefinitions::add);
        }
        List<CatalystDefinition> catalystDefinitions = new ArrayList<>();
        for (ItemStack catalystStack : catalysts) {
            IngredientManager.getCatalyst(catalystStack.getItem()).ifPresent(catalystDefinitions::add);
        }

        // Aggregate duration and level modifiers from tinctures + catalysts
        float totalDurationMult = 1.0f;
        int totalDurationFlat = 0;
        int totalLevelMod = 0;

        for (TinctureDefinition def : tinctureDefinitions) {
            totalDurationMult *= def.effectDurationMultiplier();
            totalDurationFlat += def.effectDurationFlat();
            totalLevelMod += def.effectLevelModifier();
        }
        for (CatalystDefinition def : catalystDefinitions) {
            totalDurationMult *= def.effectDurationMultiplier();
            totalDurationFlat += def.effectDurationFlat();
            totalLevelMod += def.effectLevelModifier();
        }

        int finalDuration = Math.max(1, (int)((stone.baseDuration() + totalDurationFlat) * totalDurationMult));
        int finalAmplifier = Math.max(0, (stone.baseLevel() - 1) + totalLevelMod);

        return BuiltInRegistries.MOB_EFFECT.getOptional(stone.effect())
                .map(effect -> new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), finalDuration, finalAmplifier))
                .map(List::of)
                .orElse(List.of());
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
        int total = 0;
        for (ItemStack s : stack.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.<ItemStack>of()))
            total += IngredientUtil.getPotency(s);
        for (ItemStack s : stack.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.<ItemStack>of()))
            total += IngredientUtil.getPotency(s);
        for (ItemStack s : stack.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.<ItemStack>of()))
            total += IngredientUtil.getPotency(s);
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

    // appendHoverText is always client-side — no environment guard needed
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
        List<ItemStack> stones = stack.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());

        if (Screen.hasShiftDown()) {
            if (stones.isEmpty()) {
                tooltipComponents.add(Component.translatable("tooltip.alchemical.empty_flask"));
            } else {
                List<ItemStack> tinctures = stack.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of());
                List<ItemStack> catalysts = stack.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.of());
                int activeIndex = getActiveStoneIndex(stack);
                ItemStack activeStoneStack = stones.get(activeIndex);
                var stoneType = activeStoneStack.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());

                // Compute effective stats for the active stone
                if (stoneType != null) {
                    ClientIngredientData.getStone(stoneType).ifPresent(def -> {
                        // Aggregate tincture + catalyst modifiers
                        float durMult = 1.0f;
                        int durFlat = 0;
                        int levelMod = 0;
                        float cdMult = def.elixirCooldownMultiplier();
                        int cdFlat = def.elixirCooldownFlat();

                        for (ItemStack t : tinctures) {
                            var td = ClientIngredientData.getTincture(t.getItem());
                            if (td.isPresent()) {
                                durMult *= td.get().effectDurationMultiplier();
                                durFlat += td.get().effectDurationFlat();
                                levelMod += td.get().effectLevelModifier();
                                cdMult *= td.get().elixirCooldownMultiplier();
                                cdFlat += td.get().elixirCooldownFlat();
                            }
                        }
                        for (ItemStack c : catalysts) {
                            var cd = ClientIngredientData.getCatalyst(c.getItem());
                            if (cd.isPresent()) {
                                durMult *= cd.get().effectDurationMultiplier();
                                durFlat += cd.get().effectDurationFlat();
                                levelMod += cd.get().effectLevelModifier();
                                cdMult *= cd.get().elixirCooldownMultiplier();
                                cdFlat += cd.get().elixirCooldownFlat();
                            }
                        }

                        int finalDuration = Math.max(1, (int)((def.baseDuration() + durFlat) * durMult));
                        int finalLevel = Math.max(1, def.baseLevel() + levelMod);
                        int finalCooldown = Math.max(0, (int)((getCooldownSeconds() + cdFlat) * cdMult));

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
                }

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
        } else {
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
        super.appendHoverText(stack, context, tooltipComponents, flag);
    }

    private static String formatTicks(int ticks) {
        return formatTime(ticks / 20);
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> String.valueOf(n);
        };
    }

    private static String formatTime(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h ");
        if (hours > 0 || minutes > 0) sb.append(minutes).append("m ");
        sb.append(seconds).append("s");
        return sb.toString();
    }
}
