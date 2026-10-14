package net.silvertide.alchemical.menu;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.silvertide.alchemical.config.AlchemicalConfig;
import net.silvertide.alchemical.data.IngredientManager;
import net.silvertide.alchemical.item.ElixirItem;
import net.silvertide.alchemical.item.IngredientType;
import net.silvertide.alchemical.registry.BlockRegistry;
import net.silvertide.alchemical.registry.DataComponentRegistry;
import net.silvertide.alchemical.registry.MenuRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Two slots: a flask and one staged ingredient. [Add] moves the ingredient into the flask's
 * data components, [Empty Elixir] dumps it. Both slots go back to the player on close.
 */
public class AthanorMenu extends AbstractContainerMenu {
    public static final int ELIXIR_SLOT = 0;
    public static final int INGREDIENT_SLOT = 1;
    private static final int PLAYER_INV_START = 2;
    private static final int PLAYER_INV_END = 38;   // exclusive: 27 inventory + 9 hotbar

    public static final int BUTTON_ADD = 0;
    public static final int BUTTON_CLEAR = 1;

    // Slot positions; AthanorScreen lays everything else out around these.
    public static final int ELIXIR_SLOT_X = 106;      // centred in the 230px screen
    public static final int ELIXIR_SLOT_Y = 16;
    public static final int INGREDIENT_SLOT_X = 157;
    public static final int INGREDIENT_SLOT_Y = 16;

    /** Why the staged ingredient can't go in right now, or CAN_ADD. */
    public enum ValidationResult {
        CAN_ADD(null),
        NO_ELIXIR("gui.alchemical.athanor.no_elixir"),
        NO_INGREDIENT("gui.alchemical.athanor.no_ingredient"),
        INVALID_INGREDIENT("gui.alchemical.athanor.invalid_ingredient"),
        AT_CAPACITY("gui.alchemical.athanor.at_capacity"),
        DUPLICATE_STONE("gui.alchemical.athanor.duplicate_stone"),
        NEEDS_STONE("gui.alchemical.athanor.needs_stone"),
        NEEDS_TINCTURE("gui.alchemical.athanor.needs_tincture"),
        MAX_STONES("gui.alchemical.athanor.max_stones");

        private final @Nullable String key;

        ValidationResult(@Nullable String key) {
            this.key = key;
        }

        /** The reason shown to the player. Null for CAN_ADD. */
        public @Nullable Component message() {
            if (key == null) return null;
            return this == MAX_STONES
                    ? Component.translatable(key, AlchemicalConfig.MAX_ESSENCE_STONES.get())
                    : Component.translatable(key);
        }
    }

    private final SimpleContainer container = new SimpleContainer(2);
    private final ContainerLevelAccess access;
    private final Player player;

    // Client side. The server sends no extra data.
    public AthanorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public AthanorMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(MenuRegistry.ATHANOR.get(), containerId);
        this.access = access;
        this.player = playerInventory.player;

        addSlot(new Slot(container, ELIXIR_SLOT, ELIXIR_SLOT_X, ELIXIR_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ElixirItem;
            }
        });

        // Hidden until there's a flask with room in it.
        addSlot(new Slot(container, INGREDIENT_SLOT, INGREDIENT_SLOT_X, INGREDIENT_SLOT_Y) {
            @Override
            public boolean isActive() {
                return flaskHasRoom();
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return flaskHasRoom() && IngredientType.of(stack) != IngredientType.NONE;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 35 + col * 18, 152 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 35 + col * 18, 210));
        }
    }

    private boolean flaskHasRoom() {
        ItemStack flask = container.getItem(ELIXIR_SLOT);
        return flask.getItem() instanceof ElixirItem && !ElixirItem.isFull(flask);
    }

    public ItemStack getElixirStack() {
        return container.getItem(ELIXIR_SLOT);
    }

    public ItemStack getIngredientStack() {
        return container.getItem(INGREDIENT_SLOT);
    }

    // --- Keeping the hidden slot empty ---

    /**
     * Runs every server tick. If the ingredient slot has stopped being usable (flask taken
     * out, or filled up) hand whatever's in it back, so nothing sits in a hidden slot.
     */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!player.level().isClientSide() && !getSlot(INGREDIENT_SLOT).isActive()) {
            ItemStack staged = container.removeItemNoUpdate(INGREDIENT_SLOT);
            if (!staged.isEmpty()) player.getInventory().placeItemBackInInventory(staged);
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        access.execute((level, pos) -> clearContainer(player, container));
    }

    // --- Validation ---

    public ValidationResult canAddIngredient() {
        ItemStack flask = container.getItem(ELIXIR_SLOT);
        ItemStack ingredient = container.getItem(INGREDIENT_SLOT);
        if (!(flask.getItem() instanceof ElixirItem)) return ValidationResult.NO_ELIXIR;
        if (ingredient.isEmpty()) return ValidationResult.NO_INGREDIENT;

        IngredientType type = IngredientType.of(ingredient);
        if (type == IngredientType.NONE) return ValidationResult.INVALID_INGREDIENT;

        // Build order is stone, then tincture, then catalyst.
        List<ItemStack> stones = ElixirItem.stones(flask);
        if (type != IngredientType.ESSENCE_STONE && stones.isEmpty()) return ValidationResult.NEEDS_STONE;
        if (type == IngredientType.CATALYST && ElixirItem.tinctures(flask).isEmpty()) return ValidationResult.NEEDS_TINCTURE;
        if (ElixirItem.loadedCount(flask) + IngredientManager.potency(ingredient) > ElixirItem.capacity()) return ValidationResult.AT_CAPACITY;

        if (type == IngredientType.ESSENCE_STONE) {
            if (stones.size() >= AlchemicalConfig.MAX_ESSENCE_STONES.get()) return ValidationResult.MAX_STONES;
            ResourceLocation newType = ingredient.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get());
            for (ItemStack stone : stones) {
                if (Objects.equals(newType, stone.get(DataComponentRegistry.ESSENCE_STONE_TYPE.get()))) {
                    return ValidationResult.DUPLICATE_STONE;
                }
            }
        }
        return ValidationResult.CAN_ADD;
    }

    // --- Buttons (server side; the game broadcasts changes after a successful click) ---

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return switch (id) {
            case BUTTON_ADD -> addIngredient();
            case BUTTON_CLEAR -> clearElixir(player);
            default -> false;
        };
    }

    private boolean addIngredient() {
        if (canAddIngredient() != ValidationResult.CAN_ADD) return false;
        ItemStack flask = container.getItem(ELIXIR_SLOT);
        ItemStack ingredient = container.removeItem(INGREDIENT_SLOT, 1);

        DataComponentType<List<ItemStack>> list = Objects.requireNonNull(IngredientType.of(ingredient).component).get();
        List<ItemStack> updated = new ArrayList<>(flask.getOrDefault(list, List.of()));
        updated.add(ingredient);
        flask.set(list, updated);
        return true;
    }

    /** Dumps the flask. Each stone rolls to survive; tinctures and catalysts are always lost. */
    private boolean clearElixir(Player player) {
        ItemStack flask = container.getItem(ELIXIR_SLOT);
        if (!(flask.getItem() instanceof ElixirItem)) return false;

        double breakChance = AlchemicalConfig.ESSENCE_STONE_BREAK_CHANCE.get();
        for (ItemStack stone : ElixirItem.stones(flask)) {
            if (player.getRandom().nextDouble() >= breakChance) {
                player.getInventory().placeItemBackInInventory(stone.copy());
            }
        }
        flask.remove(DataComponentRegistry.ESSENCE_STONES.get());
        flask.remove(DataComponentRegistry.TINCTURES.get());
        flask.remove(DataComponentRegistry.CATALYSTS.get());
        flask.remove(DataComponentRegistry.ACTIVE_STONE_INDEX.get());
        return true;
    }

    // --- Vanilla plumbing ---

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, BlockRegistry.ATHANOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < PLAYER_INV_START
                ? moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, true)
                : moveItemStackTo(stack, ELIXIR_SLOT, ELIXIR_SLOT + 1, false)
                        || moveItemStackTo(stack, INGREDIENT_SLOT, INGREDIENT_SLOT + 1, false);
        if (!moved) return ItemStack.EMPTY;

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }
}
