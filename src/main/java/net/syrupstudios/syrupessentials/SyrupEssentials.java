package net.syrupstudios.syrupessentials;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.syrupstudios.syruplibrary.command.SyrupCommands;
import net.syrupstudios.syruplibrary.teleport.SyrupTeleports;
import net.syrupstudios.syrupessentials.commands.ConfigCommands;
import net.syrupstudios.syrupessentials.commands.MiscellaneousCommands;
import net.syrupstudios.syrupessentials.commands.TeleportCommands;
import net.syrupstudios.syrupessentials.config.SyrupEssentialsConfig;
import net.syrupstudios.syrupessentials.data.PlayerData;
import net.syrupstudios.syrupessentials.util.DataManager;
import net.syrupstudios.syrupessentials.util.TeleportManager;
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
		SyrupCommands.register(MOD_ID, "syrupessentials",
				SyrupEssentialsConfig.get().registerToNamespace(),
				SyrupEssentialsConfig.get().registerAliasAsWellAsNamespace(),
				TeleportCommands::register);
		SyrupCommands.register(MOD_ID, "syrupessentials",
				SyrupEssentialsConfig.get().registerToNamespace(),
				SyrupEssentialsConfig.get().registerAliasAsWellAsNamespace(),
				MiscellaneousCommands::register);
		SyrupCommands.register(MOD_ID, "syrupessentials", true, false, ConfigCommands::register);
	}

	public static void serverStarted(MinecraftServer server) {
		dataManager = new DataManager(server);
		teleportManager = new TeleportManager();
		createWorld(server);
	}

	public static void saveDeathLocation(ServerPlayer player) {
		PlayerData playerData = DataManager.getOrCreatePlayer(player).orElseThrow();
		playerData.addTeleportHistory(SyrupTeleports.capture(player));
	}

	public static void playerJoin(ServerPlayer player) {
		try {
			dataManager.loadPlayer(player.getUUID());
			((net.minecraft.server.level.ServerLevel) player.level()).getServer().getPlayerList().broadcastAll(new net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket(
					net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
					player));
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
