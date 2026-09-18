package net.syrupstudios.syrupessentials;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.syrupstudios.syrupessentials.commands.ConfigCommands;
import net.syrupstudios.syrupessentials.commands.TeleportCommands;
import net.syrupstudios.syrupessentials.config.SyrupEssentialsConfig;
import net.syrupstudios.syrupessentials.data.PlayerData;
import net.syrupstudios.syrupessentials.util.DataManager;
import net.syrupstudios.syrupessentials.util.TeleportManager;
import net.syrupstudios.syrupessentials.util.TeleportPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SyrupEssentials {
	public static final String MOD_ID = "syrup_essentials";
	private static DataManager dataManager;
	private static TeleportManager teleportManager;

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static void initialize() {
		LOGGER.info("Initializing Syrup Essentials");
		SyrupEssentialsConfig.initialize();
	}

	public static void registerCommands(CommandDispatcher<CommandSourceStack> commandDispatcher) {
		TeleportCommands.register(commandDispatcher);
		ConfigCommands.register(commandDispatcher);
	}

	public static void serverStarted(MinecraftServer server) {
		dataManager = new DataManager(server);
		teleportManager = new TeleportManager();
		createWorld(server);
	}

	public static void saveDeathLocation(ServerPlayer player) {
		PlayerData playerData = DataManager.getOrCreatePlayer(player).orElseThrow();
		TeleportPos deathLoc = new TeleportPos(player.level(), player.position(), player.getXRot(), player.getYRot());
		playerData.addTeleportHistory(deathLoc);
	}

	public static void playerJoin(ServerPlayer player) {
		try {
			dataManager.loadPlayer(player.getUUID());
		}
		catch (Exception e) {
			LOGGER.error("Error Loading Player: {}", player.getDisplayName().getString());
		}
	}

	private static void createWorld(MinecraftServer server){
		try {
			dataManager.loadWorld(server);
		} catch (Exception e) {
			LOGGER.error("Error Loading World: {}", server.getWorldData().getLevelName());
		}
	}

	public static void playerLeave(ServerPlayer player) {
		try {
			dataManager.savePlayer(DataManager.getOrCreatePlayer(player).orElseThrow());
		}
		catch (Exception e) {
			LOGGER.error("Error Saving Player: {}", player.getDisplayName().getString());
		}
	}

	public static void tick(MinecraftServer minecraftServer) {
		dataManager.onServerTick();
		teleportManager.onServerTick();
	}

	public static void serverStopping(MinecraftServer server) {
		dataManager.saveWorld(server);
		dataManager.savePlayers(server);
		dataManager.flush();
		teleportManager.flush();
	}
}
