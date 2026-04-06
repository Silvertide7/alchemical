package net.silvertide.alchemical.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.silvertide.alchemical.menu.AthanorMenu;
import net.silvertide.alchemical.registry.BlockEntityRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AthanorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int ELIXIR_SLOT = 0;
    public static final int INGREDIENT_SLOT = 1;
    public static final int CONTAINER_SIZE = 2;

    private final SimpleContainer container = new SimpleContainer(CONTAINER_SIZE);

    public AthanorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ATHANOR.get(), pos, state);
        container.addListener(c -> this.setChanged());
    }

    public SimpleContainer getContainer() {
        return container;
    }

    public ItemStack getElixirStack() {
        return container.getItem(ELIXIR_SLOT);
    }

    public ItemStack getIngredientStack() {
        return container.getItem(INGREDIENT_SLOT);
    }

    // --- MenuProvider ---

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.alchemical.athanor");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory playerInventory, @NotNull Player player) {
        return new AthanorMenu(containerId, playerInventory, this);
    }

    // --- Serialization ---
    // The elixir and ingredient slots are always returned to the player on GUI close,
    // so there is nothing to persist here. saveAdditional/loadAdditional are not overridden.

    // --- Client Sync ---

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
