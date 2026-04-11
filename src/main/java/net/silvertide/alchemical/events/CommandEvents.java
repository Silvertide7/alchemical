package net.silvertide.alchemical.events;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.network.CB_SyncElixirCooldownPacket;
import net.silvertide.alchemical.util.ElixirAttachmentUtil;

@EventBusSubscriber(modid = Alchemical.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class CommandEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("alchemical")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("clearcooldown")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(CommandEvents::clearCooldown))
                        )
        );
    }

    private static int clearCooldown(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");

        ElixirAttachmentUtil.setCooldown(target, null);
        PacketDistributor.sendToPlayer(target, new CB_SyncElixirCooldownPacket(0L, 0));

        ctx.getSource().sendSuccess(
                () -> Component.translatable("command.alchemical.clearcooldown.success", target.getDisplayName()),
                true);
        return Command.SINGLE_SUCCESS;
    }

    private CommandEvents() {}
}
