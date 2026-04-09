package net.silvertide.alchemical.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.silvertide.alchemical.util.IngredientUtil;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.silvertide.alchemical.block.entity.AthanorBlockEntity;
import net.silvertide.alchemical.config.AlchemicalConfig;
import net.silvertide.alchemical.item.IElixir;
import net.silvertide.alchemical.item.IngredientType;
import net.silvertide.alchemical.registry.BlockRegistry;
import net.silvertide.alchemical.registry.DataComponentRegistry;
import net.silvertide.alchemical.registry.MenuRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AthanorMenu extends AbstractContainerMenu {
    public static final int ELIXIR_SLOT_INDEX = 0;
    public static final int INGREDIENT_SLOT_INDEX = 1;
    private static final int PLAYER_INV_START = 2;
    private static final int PLAYER_INV_END = 29;
    private static final int HOTBAR_START = 29;
    private static final int HOTBAR_END = 38;

    // GUI slot positions: elixir centred, ingredient in right panel
    private static final int ELIXIR_SLOT_X     = 79;   // (176-18)/2
    private static final int ELIXIR_SLOT_Y     = 16;
    private static final int INGREDIENT_SLOT_X = 130;
    private static final int INGREDIENT_SLOT_Y = 16;

    public enum ValidationResult {
        CAN_ADD,
        NO_ELIXIR,
        NO_INGREDIENT,
        INVALID_INGREDIENT,
        AT_CAPACITY,
        DUPLICATE_STONE,
        NEEDS_STONE
    }

    private final AthanorBlockEntity blockEntity;
    private final Player menuPlayer;

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
        this.menuPlayer  = playerInventory.player;

        // Slot 0 — Elixir
        addSlot(new Slot(blockEntity.getContainer(), AthanorBlockEntity.ELIXIR_SLOT, ELIXIR_SLOT_X, ELIXIR_SLOT_Y) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() instanceof IElixir;
            }
        });

        // Slot 1 — Ingredient (hidden and locked when no elixir is present or elixir is full)
        addSlot(new Slot(blockEntity.getContainer(), AthanorBlockEntity.INGREDIENT_SLOT, INGREDIENT_SLOT_X, INGREDIENT_SLOT_Y) {
            @Override
            public boolean isActive() {
                ItemStack elixir = blockEntity.getElixirStack();
                if (elixir.isEmpty() || !(elixir.getItem() instanceof IElixir iElixir)) return false;
                return iElixir.getLoadedCount(elixir) < iElixir.getCapacity();
            }

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                ItemStack elixir = blockEntity.getElixirStack();
                return !elixir.isEmpty()
                        && elixir.getItem() instanceof IElixir
                        && IngredientType.of(stack) != IngredientType.NONE;
            }
        });

        // Player inventory (slots 2–28)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 152 + row * 18));
            }
        }

        // Hotbar (slots 29–37)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 210));
        }

        addDataSlots(containerData);
    }

    // Client-side constructor
    public AthanorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, getBlockEntity(playerInventory, buf));
    }

    private static AthanorBlockEntity getBlockEntity(Inventory playerInventory, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof AthanorBlockEntity athanorBE) return athanorBE;
        throw new IllegalStateException("No AthanorBlockEntity at " + pos);
    }

    // --- Container change listener ---

    /**
     * Called every server tick while the menu is open. Guarantees the ingredient
     * slot is ejected whenever the elixir slot is empty or the elixir is full,
     * regardless of which interaction path removed the elixir.
     */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!menuPlayer.level().isClientSide()) {
            ItemStack elixir = blockEntity.getElixirStack();
            boolean shouldEject = elixir.isEmpty() || !(elixir.getItem() instanceof IElixir);
            if (shouldEject) {
                returnSlotToPlayer(menuPlayer, INGREDIENT_SLOT_INDEX);
            }
        }
    }

    @Override
    public void slotsChanged(@NotNull Container container) {
        super.slotsChanged(container);
        if (container == blockEntity.getContainer() && !menuPlayer.level().isClientSide()) {
            // Eject the staged ingredient whenever the slot should be inactive:
            // - elixir removed, OR
            // - elixir just became full (so the now-invisible slot can't trap items)
            ItemStack elixir = blockEntity.getElixirStack();
            boolean slotShouldBeInactive = elixir.isEmpty()
                    || !(elixir.getItem() instanceof IElixir iElixir)
                    || iElixir.getLoadedCount(elixir) >= iElixir.getCapacity();
            if (slotShouldBeInactive) {
                returnSlotToPlayer(menuPlayer, INGREDIENT_SLOT_INDEX);
            }
        }
    }

    // --- Validation ---

    public ValidationResult canAddIngredient() {
        return canAddIngredient(blockEntity.getElixirStack(), blockEntity.getIngredientStack());
    }

    public ValidationResult canAddIngredient(ItemStack elixir, ItemStack ingredient) {
        if (elixir.isEmpty() || !(elixir.getItem() instanceof IElixir iElixir)) return ValidationResult.NO_ELIXIR;
        if (ingredient.isEmpty()) return ValidationResult.NO_INGREDIENT;
        if (IngredientType.of(ingredient) == IngredientType.NONE) return ValidationResult.INVALID_INGREDIENT;

        // Require at least one essence stone before tinctures or catalysts can be added
        IngredientType ingType = IngredientType.of(ingredient);
        if (ingType != IngredientType.ESSENCE_STONE && iElixir.getStoneCount(elixir) < 1)
            return ValidationResult.NEEDS_STONE;

        if (iElixir.getLoadedCount(elixir) + IngredientUtil.getPotency(ingredient) > iElixir.getCapacity())
            return ValidationResult.AT_CAPACITY;

        // Prevent adding a duplicate essence stone type
        if (IngredientType.of(ingredient) == IngredientType.ESSENCE_STONE) {
            ResourceLocation newType = ingredient.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());
            if (newType != null) {
                List<ItemStack> existing = elixir.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of());
                for (ItemStack stone : existing) {
                    if (newType.equals(stone.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get()))) {
                        return ValidationResult.DUPLICATE_STONE;
                    }
                }
            }
        }

        return ValidationResult.CAN_ADD;
    }

    // --- Button handlers ---

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        return switch (id) {
            case 0 -> handleAddIngredient(player);
            case 1 -> handleClearElixir(player);
            default -> false;
        };
    }

    private boolean handleAddIngredient(Player player) {
        ItemStack elixir = blockEntity.getElixirStack();
        ItemStack ingredient = blockEntity.getIngredientStack();

        if (canAddIngredient(elixir, ingredient) != ValidationResult.CAN_ADD) return false;

        IngredientType type = IngredientType.of(ingredient);
        Supplier<DataComponentType<List<ItemStack>>> component = switch (type) {
            case TINCTURE -> DataComponentRegistry.TINCTURES;
            case ESSENCE_STONE -> DataComponentRegistry.ESSENCE_STONES;
            case CATALYST -> DataComponentRegistry.CATALYSTS;
            default -> null;
        };
        if (component == null) return false;

        appendIngredientToComponent(elixir, component, ingredient);
        blockEntity.getContainer().removeItem(AthanorBlockEntity.INGREDIENT_SLOT, 1);
        blockEntity.setChanged();
        broadcastChanges();
        return true;
    }

    private boolean handleClearElixir(Player player) {
        ItemStack elixir = blockEntity.getElixirStack();
        if (elixir.isEmpty() || !(elixir.getItem() instanceof IElixir)) return false;

        // Each stone rolls against break chance — survivors returned to player
        List<ItemStack> stones = new ArrayList<>(elixir.getOrDefault(DataComponentRegistry.ESSENCE_STONES.get(), List.of()));
        double breakChance = AlchemicalConfig.ESSENCE_STONE_BREAK_CHANCE.get();
        for (ItemStack stone : stones) {
            if (player.getRandom().nextDouble() >= breakChance) {
                if (!player.getInventory().add(stone.copy())) {
                    player.drop(stone.copy(), false);
                }
            }
        }

        // Tinctures and catalysts are destroyed — no return
        elixir.remove(DataComponentRegistry.TINCTURES.get());
        elixir.remove(DataComponentRegistry.ESSENCE_STONES.get());
        elixir.remove(DataComponentRegistry.CATALYSTS.get());
        elixir.remove(DataComponentRegistry.ACTIVE_STONE_INDEX.get());

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

    private void appendIngredientToComponent(ItemStack elixir,
                                              Supplier<DataComponentType<List<ItemStack>>> component,
                                              ItemStack ingredient) {
        List<ItemStack> current = new ArrayList<>(elixir.getOrDefault(component.get(), List.of()));
        current.add(ingredient.copyWithCount(1));
        elixir.set(component.get(), current);
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
                if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
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
