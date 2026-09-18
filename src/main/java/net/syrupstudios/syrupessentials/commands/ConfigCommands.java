package net.syrupstudios.syrupessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigIssue;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigIssueSeverity;
import net.syrupstudios.syruplibrary.config.diagnostic.ConfigLoadResult;
import net.syrupstudios.syrupessentials.config.SyrupEssentialsConfig;

public final class ConfigCommands {
    private ConfigCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("syrupessentials")
                .then(Commands.literal("reload")
                        //? if >=1.21.11 {
                        /*.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))*/
                        //?} else {
                        .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        //?}
                        .executes(ConfigCommands::reload)));
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        ConfigLoadResult result = SyrupEssentialsConfig.reload();
        if (!result.successful()) {
            String error = result.cause() == null || result.cause().getMessage() == null
                    ? "see the server log"
                    : result.cause().getMessage();
            context.getSource().sendFailure(Component.literal(
                    "Unable to reload Syrup Essentials config: " + error
            ));
            return 0;
        }

        for (ConfigIssue issue : result.issues()) {
            if (issue.severity() == ConfigIssueSeverity.WARNING) {
                context.getSource().sendSuccess(() -> Component.literal(
                        "Config warning at " + issue.path() + ": " + issue.message()
                ), false);
            }
        }

        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            context.getSource().getServer().getCommands().sendCommands(player);
        }

        String message = "Reloaded Syrup Essentials config from " + SyrupEssentialsConfig.getPath()
                + ". Command namespace settings take effect after a restart";
        long warningCount = result.issues().stream()
                .filter(issue -> issue.severity() == ConfigIssueSeverity.WARNING)
                .count();
        if (warningCount > 0) {
            message += " with " + warningCount + " warning(s)";
        }
        String successMessage = message;
        context.getSource().sendSuccess(() -> Component.literal(successMessage), true);
        return 1;
    }
}
