package net.silvertide.alchemical.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.silvertide.alchemical.menu.AthanorMenu;
import net.silvertide.alchemical.registry.BlockEntityRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AthanorBlockEntity extends BlockEntity implements MenuProvider {

    public AthanorBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.ATHANOR.get(), pos, state);
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
    // The block entity holds no inventory. Elixir and ingredient slots live in
    // the menu (per-player) and are returned to the player when the GUI closes.

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
