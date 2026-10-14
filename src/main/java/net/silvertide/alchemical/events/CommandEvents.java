package net.silvertide.alchemical.events;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.silvertide.alchemical.Alchemical;
import net.silvertide.alchemical.attachments.ElixirCooldown;

@EventBusSubscriber(modid = Alchemical.MODID)
public final class CommandEvents {
    private CommandEvents() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("alchemical")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("clearcooldown")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(CommandEvents::clearCooldown))));
    }

    private static int clearCooldown(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        ElixirCooldown.set(target, ElixirCooldown.NONE);
        ctx.getSource().sendSuccess(
                () -> Component.translatable("command.alchemical.clearcooldown.success", target.getDisplayName()), true);
        return Command.SINGLE_SUCCESS;
    }
}
