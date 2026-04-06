package net.silvertide.alchemical.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.silvertide.alchemical.block.entity.AthanorBlockEntity;
import net.silvertide.alchemical.item.IElixir;
import net.silvertide.alchemical.item.IngredientType;
import net.silvertide.alchemical.registry.BlockRegistry;
import net.silvertide.alchemical.registry.DataComponentRegistry;
import net.silvertide.alchemical.registry.MenuRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AthanorMenu extends AbstractContainerMenu {
    public static final int ELIXIR_SLOT_INDEX = 0;
    public static final int INGREDIENT_SLOT_INDEX = 1;
    private static final int PLAYER_INV_START = 2;
    private static final int PLAYER_INV_END = 29;   // 2 + 27
    private static final int HOTBAR_START = 29;
    private static final int HOTBAR_END = 38;        // 29 + 9

    // GUI slot positions
    private static final int ELIXIR_SLOT_X = 44;
    private static final int ELIXIR_SLOT_Y = 35;
    private static final int INGREDIENT_SLOT_X = 116;
    private static final int INGREDIENT_SLOT_Y = 35;

    public enum ValidationResult {
        CAN_ADD,
        NO_ELIXIR,
        NO_INGREDIENT,
        INVALID_INGREDIENT,
        AT_CAPACITY
    }

    private final AthanorBlockEntity blockEntity;

    // Synced to client: index 0 = loadedCount, index 1 = capacity
    private final SimpleContainerData containerData = new SimpleContainerData(2) {
        @Override
        public int get(int index) {
            ItemStack elixir = blockEntity.getElixirStack();
            if (!(elixir.getItem() instanceof IElixir iElixir)) return 0;
            return switch (index) {
                case 0 -> iElixir.getLoadedCount(elixir);
                case 1 -> iElixir.getCapacity();
                default -> 0;
            };
        }
        @Override
        public void set(int index, int value) { /* read-only */ }
        @Override
        public int getCount() { return 2; }
    };

    // Server-side constructor
    public AthanorMenu(int containerId, Inventory playerInventory, AthanorBlockEntity blockEntity) {
        super(MenuRegistry.ATHANOR.get(), containerId);
        this.blockEntity = blockEntity;

        // Slot 0 — Elixir (only accepts IElixir items)
        addSlot(new Slot(blockEntity.getContainer(), AthanorBlockEntity.ELIXIR_SLOT, ELIXIR_SLOT_X, ELIXIR_SLOT_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof IElixir;
            }
        });

        // Slot 1 — Ingredient preview (only accepts valid ingredient types)
        addSlot(new Slot(blockEntity.getContainer(), AthanorBlockEntity.INGREDIENT_SLOT, INGREDIENT_SLOT_X, INGREDIENT_SLOT_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return IngredientType.of(stack) != IngredientType.NONE;
            }
        });

        // Player inventory (slots 2–28)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar (slots 29–37)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        addDataSlots(containerData);
    }

    // Client-side constructor (called by MenuRegistry factory with FriendlyByteBuf)
    public AthanorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buf));
    }

    private static AthanorBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof AthanorBlockEntity athanorBE) return athanorBE;
        // Fallback for safety — should never happen in normal play
        throw new IllegalStateException("No AthanorBlockEntity at " + pos);
    }

    // --- Validation ---

    public ValidationResult canAddIngredient() {
        return canAddIngredient(blockEntity.getElixirStack(), blockEntity.getIngredientStack());
    }

    public ValidationResult canAddIngredient(ItemStack elixir, ItemStack ingredient) {
        if (elixir.isEmpty() || !(elixir.getItem() instanceof IElixir iElixir)) return ValidationResult.NO_ELIXIR;
        if (ingredient.isEmpty()) return ValidationResult.NO_INGREDIENT;
        if (IngredientType.of(ingredient) == IngredientType.NONE) return ValidationResult.INVALID_INGREDIENT;
        if (iElixir.getLoadedCount(elixir) >= iElixir.getCapacity()) return ValidationResult.AT_CAPACITY;
        return ValidationResult.CAN_ADD;
    }

    // --- Button: Add ingredient ---

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        if (id != 0) return false;

        ItemStack elixir = blockEntity.getElixirStack();
        ItemStack ingredient = blockEntity.getIngredientStack();

        if (canAddIngredient(elixir, ingredient) != ValidationResult.CAN_ADD) return false;

        IngredientType type = IngredientType.of(ingredient);

        // Append ingredient to the correct DataComponent list — always create a new list
        switch (type) {
            case TINCTURE -> {
                List<ItemStack> current = new ArrayList<>(elixir.getOrDefault(DataComponentRegistry.TINCTURES.get(), List.of()));
                current.add(ingredient.copyWithCount(1));
                elixir.set(DataComponentRegistry.TINCTURES.get(), current);
            }
            case ESSENCE_STONE -> {
                List<ItemStack> current = new ArrayList<>(elixir.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of()));
                current.add(ingredient.copyWithCount(1));
                elixir.set(DataComponentRegistry.ESSENCE_STONES.get(), current);
            }
            case CATALYST -> {
                List<ItemStack> current = new ArrayList<>(elixir.getOrDefault(DataComponentRegistry.CATALYSTS.get(), List.of()));
                current.add(ingredient.copyWithCount(1));
                elixir.set(DataComponentRegistry.CATALYSTS.get(), current);
            }
            default -> { return false; }
        }

        // Consume the ingredient
        blockEntity.getContainer().removeItem(AthanorBlockEntity.INGREDIENT_SLOT, 1);

        blockEntity.setChanged();
        broadcastChanges();
        return true;
    }

    // --- Helpers for the screen ---

    public ItemStack getElixirStack() {
        return getSlot(ELIXIR_SLOT_INDEX).getItem();
    }

    public ItemStack getIngredientStack() {
        return getSlot(INGREDIENT_SLOT_INDEX).getItem();
    }

    public int getLoadedCount() {
        return containerData.get(0);
    }

    public int getCapacity() {
        return containerData.get(1);
    }

    // --- On close: return elixir and any staged ingredient to the player ---

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            returnSlotToPlayer(player, ELIXIR_SLOT_INDEX);
            returnSlotToPlayer(player, INGREDIENT_SLOT_INDEX);
        }
    }

    private void returnSlotToPlayer(Player player, int slotIndex) {
        ItemStack stack = blockEntity.getContainer().getItem(slotIndex);
        if (!stack.isEmpty()) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            blockEntity.getContainer().setItem(slotIndex, ItemStack.EMPTY);
        }
    }

    // --- Standard menu boilerplate ---

    @Override
    public boolean stillValid(@NotNull Player player) {
        return AbstractContainerMenu.stillValid(
                ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player,
                BlockRegistry.ATHANOR.get());
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack remainder = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            remainder = stack.copy();

            if (index < PLAYER_INV_START) {
                // Moving from block slots to player inventory
                if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to block slots — try elixir first, then ingredient
                if (!moveItemStackTo(stack, ELIXIR_SLOT_INDEX, ELIXIR_SLOT_INDEX + 1, false)) {
                    if (!moveItemStackTo(stack, INGREDIENT_SLOT_INDEX, INGREDIENT_SLOT_INDEX + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return remainder;
    }
}
