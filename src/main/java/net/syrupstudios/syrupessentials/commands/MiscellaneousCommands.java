package net.syrupstudios.syrupessentials.commands;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.storage.LevelResource;
import net.syrupstudios.syruplibrary.command.SyrupCommands;
import net.syrupstudios.syrupessentials.config.SyrupEssentialsConfig;
import net.syrupstudios.syrupessentials.data.PlayerData;
import net.syrupstudios.syrupessentials.util.DataManager;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

import com.mojang.brigadier.arguments.StringArgumentType;

public final class MiscellaneousCommands {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("########0.00",
            DecimalFormatSymbols.getInstance(Locale.ROOT));
    private static final List<Leaderboard> LEADERBOARDS = List.of(
            new Leaderboard("deaths", stats -> stats.getValue(Stats.CUSTOM.get(Stats.DEATHS)), value -> Long.toString((long) value)),
            new Leaderboard("time_played", stats -> stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)), MiscellaneousCommands::formatTime),
            new Leaderboard("deaths_per_hour", stats -> {
                int deaths = stats.getValue(Stats.CUSTOM.get(Stats.DEATHS));
                int time = stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
                return time < 72000 ? 0 : deaths * 72000.0 / time;
            }, value -> DECIMAL_FORMAT.format(value)),
            new Leaderboard("player_kills", stats -> stats.getValue(Stats.CUSTOM.get(Stats.PLAYER_KILLS)), value -> Long.toString((long) value)),
            new Leaderboard("mob_kills", stats -> stats.getValue(Stats.CUSTOM.get(Stats.MOB_KILLS)), value -> Long.toString((long) value)),
            new Leaderboard("damage_dealt", stats -> stats.getValue(Stats.CUSTOM.get(Stats.DAMAGE_DEALT)) / 10.0, value -> DECIMAL_FORMAT.format(value)),
            new Leaderboard("jumps", stats -> stats.getValue(Stats.CUSTOM.get(Stats.JUMP)), value -> Long.toString((long) value)),
            new Leaderboard("distance_walked", stats -> stats.getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM)), MiscellaneousCommands::formatDistance),
            new Leaderboard("time_since_death", stats -> stats.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH)), MiscellaneousCommands::formatTime)
    );

    private MiscellaneousCommands() {
    }

    public static void register(SyrupCommands.Registrar registrar) {
        registrar.command("leaderboard", SyrupCommands.Access.EVERYONE,
                source -> SyrupEssentialsConfig.get().miscellaneous().leaderboardEnabled(),
                command -> {
                    command.executes(MiscellaneousCommands::listLeaderboards);
                    for (Leaderboard leaderboard : LEADERBOARDS) {
                        command.then(Commands.literal(leaderboard.name())
                                .executes(context -> showLeaderboard(context, leaderboard)));
                    }
                });
        registerNickname(registrar, "nickname");
    }

    private static void registerNickname(SyrupCommands.Registrar registrar, String commandName) {
        registrar.command(commandName, SyrupCommands.Access.EVERYONE,
                source -> SyrupEssentialsConfig.get().miscellaneous().nicknameEnabled(),
                command -> command.executes(context -> setNickname(context, ""))
                        .then(Commands.argument("nickname", StringArgumentType.greedyString())
                                .executes(context -> setNickname(context,
                                        StringArgumentType.getString(context, "nickname")))));
    }

    private static int setNickname(CommandContext<CommandSourceStack> context, String nickname) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(Component.translatableWithFallback(
                    "syrup_essentials.nickname.player_only", "Only a player can set a nickname."));
            return 0;
        }
        String cleanedNickname = nickname.trim();
        if (cleanedNickname.length() > 32) {
            context.getSource().sendFailure(Component.translatableWithFallback(
                    "syrup_essentials.nickname.too_long", "Nickname is too long (maximum 32 characters)."));
            return 0;
        }

        PlayerData data = DataManager.getOrCreatePlayer(player).orElseThrow();
        data.setNickname(cleanedNickname);
        context.getSource().getServer().getPlayerList().broadcastAll(
                new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
                        net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
                        player));
        Component message = cleanedNickname.isBlank()
                ? Component.translatableWithFallback("syrup_essentials.nickname.reset", "Nickname reset.")
                : Component.translatableWithFallback("syrup_essentials.nickname.changed",
                        "Nickname changed to '%s'.", cleanedNickname);
        context.getSource().sendSuccess(() -> message, false);
        return 1;
    }

    private static int showLeaderboard(CommandContext<CommandSourceStack> context, Leaderboard leaderboard) {
        var server = context.getSource().getServer();
        File statsDirectory = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile();
        File[] files = statsDirectory.listFiles((directory, name) -> name.endsWith(".json"));
        context.getSource().sendSuccess(() -> Component.translatableWithFallback(
                "syrup_essentials.leaderboard.title", "Leaderboard [ %s ]",
                leaderboard.name().replace('_', ' ')), false);
        if (files == null) files = new File[0];

        List<LeaderboardRow> rows = new ArrayList<>();
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        for (File file : files) {
            try {
                UUID playerId = UUID.fromString(file.getName().substring(0, file.getName().length() - 5));
                ServerPlayer online = server.getPlayerList().getPlayer(playerId);
                ServerStatsCounter stats = online == null ? readStats(server, file.toPath()) : online.getStats();
                double value = leaderboard.value().applyAsDouble(stats);
                if (!Double.isFinite(value) || value == 0) {
                    continue;
                }
                String name = online == null ? playerId.toString()
                        : online.getDisplayName().getString();
                PlayerData saved = DataManager.getSavedPlayerData(server, playerId).orElse(null);
                if (saved != null && saved.getNickname() != null && !saved.getNickname().isBlank()) {
                    name = saved.getNickname();
                } else if (saved != null) {
                    name = saved.getPlayerName();
                }
                rows.add(new LeaderboardRow(playerId, name, value, leaderboard.format().apply(value)));
                seen.add(playerId);
            } catch (Exception exception) {
                net.syrupstudios.syrupessentials.SyrupEssentials.LOGGER.debug(
                        "Could not read leaderboard stats file {}", file, exception);
            }
        }
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            if (seen.add(online.getUUID())) {
                double value = leaderboard.value().applyAsDouble(online.getStats());
                if (Double.isFinite(value) && value != 0) {
                    rows.add(new LeaderboardRow(online.getUUID(), online.getDisplayName().getString(), value,
                            leaderboard.format().apply(value)));
                }
            }
        }
        rows.sort(Comparator.comparingDouble(LeaderboardRow::value).reversed());
        if (rows.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.translatableWithFallback(
                    "syrup_essentials.leaderboard.empty", "No data.")
                    .withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        UUID callerId = context.getSource().getPlayer() == null ? null : context.getSource().getPlayer().getUUID();
        for (int i = 0; i < Math.min(20, rows.size()); i++) {
            LeaderboardRow row = rows.get(i);
            int rank = i + 1;
            ChatFormatting rankColor = rank == 1 ? ChatFormatting.GOLD
                    : rank == 2 ? ChatFormatting.AQUA : rank == 3 ? ChatFormatting.RED : ChatFormatting.GRAY;
            Component line = Component.literal(String.format(Locale.ROOT, "#%02d ", rank)).withStyle(rankColor)
                    .append(Component.literal(row.name()).withStyle(row.playerId().equals(callerId)
                            ? ChatFormatting.GREEN : ChatFormatting.YELLOW))
                    .append(Component.literal(": " + row.formattedValue()).withStyle(ChatFormatting.WHITE));
            context.getSource().sendSuccess(() -> line, false);
        }
        return rows.size();
    }

    private static String formatTime(double ticks) {
        double seconds = ticks / 20;
        double minutes = seconds / 60;
        double hours = minutes / 60;
        double days = hours / 24;
        double years = days / 365;
        if (years > 0.5) return DECIMAL_FORMAT.format(years) + " y";
        if (days > 0.5) return DECIMAL_FORMAT.format(days) + " d";
        if (hours > 0.5) return DECIMAL_FORMAT.format(hours) + " h";
        return minutes > 0.5 ? DECIMAL_FORMAT.format(minutes) + " m" : DECIMAL_FORMAT.format(seconds) + " s";
    }

    private static int listLeaderboards(CommandContext<CommandSourceStack> context) {
        String options = LEADERBOARDS.stream().map(Leaderboard::name).collect(java.util.stream.Collectors.joining(", "));
        context.getSource().sendSuccess(() -> Component.translatableWithFallback(
                "syrup_essentials.leaderboard.options", "Available leaderboards: %s", options), false);
        return 1;
    }

    private static ServerStatsCounter readStats(net.minecraft.server.MinecraftServer server, Path path) {
        //? if >=1.21.11 {
        /*return new ServerStatsCounter(server, path);
        *///?} else {
        return new ServerStatsCounter(server, path.toFile());
        //?}
    }

    private static String formatDistance(double centimeters) {
        double meters = centimeters / 100;
        double kilometers = meters / 1000;
        if (kilometers > 0.5) return DECIMAL_FORMAT.format(kilometers) + " km";
        return meters > 0.5 ? DECIMAL_FORMAT.format(meters) + " m" : DECIMAL_FORMAT.format(centimeters) + " cm";
    }

    private record Leaderboard(String name, ToDoubleFunction<ServerStatsCounter> value,
                               java.util.function.DoubleFunction<String> format) {
    }

    private record LeaderboardRow(UUID playerId, String name, double value, String formattedValue) {
    }
}
