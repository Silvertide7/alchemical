package net.silvertide.alchemical.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.silvertide.alchemical.block.entity.AthanorBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AthanorBlock extends BaseEntityBlock {
    public static final MapCodec<AthanorBlock> CODEC = simpleCodec(AthanorBlock::new);

    @Override
    public @NotNull MapCodec<AthanorBlock> codec() {
        return CODEC;
    }

    public AthanorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    // --- Block Entity ---

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new AthanorBlockEntity(pos, state);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    // --- Interaction ---

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level,
                                                        @NotNull BlockPos pos, @NotNull Player player,
                                                        @NotNull BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (level.getBlockEntity(pos) instanceof AthanorBlockEntity blockEntity
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(blockEntity, pos);
        }
        return InteractionResult.CONSUME;
    }
}
