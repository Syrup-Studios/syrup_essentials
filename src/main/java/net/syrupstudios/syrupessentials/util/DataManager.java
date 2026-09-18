package net.syrupstudios.syrupessentials.util;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.syrupstudios.syrupessentials.config.SyrupEssentialsConfig;
import net.syrupstudios.syrupessentials.data.PlayerData;
import net.syrupstudios.syrupessentials.data.WorldData;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static net.syrupstudios.syrupessentials.util.SNBTFormatter.formatString;

public class DataManager {
    private static final String MOD_PATH = "syrup_essential_data";
    private static final String WORLD_PATH = "world_data";
    private static final String PLAYER_PATH = "player_data";
    private final File dataDirectory;
    private final File worldDirectory;
    private final File playerDirectory;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final MinecraftServer minecraftServer;
    private static final Map<UUID, PlayerData> PLAYERS = new HashMap<>();
    private static WorldData WORLD_DATA;
    private static long currentTick;

    public DataManager(MinecraftServer server) {
        this.dataDirectory = server.getWorldPath(LevelResource.ROOT).resolve(MOD_PATH).toFile();
        this.worldDirectory = dataDirectory.toPath().resolve(WORLD_PATH).toFile();
        this.playerDirectory = dataDirectory.toPath().resolve(PLAYER_PATH).toFile();
        this.minecraftServer = server;
        
        mkDirsIfNotExisting(dataDirectory);
        mkDirsIfNotExisting(worldDirectory);
        mkDirsIfNotExisting(playerDirectory);
    }

    private void mkDirsIfNotExisting(File file) {
        if(!file.exists()){
            file.mkdirs();
        }
    }

    public void loadPlayer(UUID playerUUID){
        Path path = playerDirectory.toPath().resolve(playerUUID + ".snbt");
        PlayerData playerData = getOrCreatePlayer(minecraftServer,playerUUID).orElseThrow();
        if (Files.exists(path)) {
            try {
                //? if >=1.21.11 {
                /*playerData.readNbt(TagParser.parseCompoundFully(new String(Files.readAllBytes(path))));
                *///?} else {
                playerData.readNbt(TagParser.parseTag(new String(Files.readAllBytes(path))));
                //?}
                PLAYERS.put(playerUUID, playerData);
            } catch (Exception e) {
                LOGGER.error("Error while reading player data: {}", e.toString());
            }
        }
        else {
            try {
                Files.createFile(path);
                PLAYERS.put(playerUUID, playerData);
                playerData.triggerUpdate();
                savePlayer(playerData);
            } catch (Exception e) {
                LOGGER.error("Error while creating player data: {}", e.toString());
            }
        }
    }

    public void loadWorld(MinecraftServer minecraftServer){
        Path path = worldDirectory.toPath().resolve(minecraftServer.getWorldData().getLevelName() + ".snbt");
        WorldData worldData = getOrCreateWorld(minecraftServer).orElseThrow();
        if(Files.exists(path)){
            try {
                //? if >=1.21.11 {
                /*worldData.readNbt(TagParser.parseCompoundFully(new String(Files.readAllBytes(path))));
                *///?} else {
                worldData.readNbt(TagParser.parseTag(new String(Files.readAllBytes(path))));
                //?}
                WORLD_DATA = worldData;
            } catch (Exception e) {
                LOGGER.error("Error while reading world data: {}", e.toString());
            }
        }
        else {
            try {
                Files.createFile(path);
                WORLD_DATA = worldData;
                WORLD_DATA.triggerUpdate();
                saveWorld(minecraftServer);
            } catch (Exception e) {
                LOGGER.error("Error while creating world data: {}", e.toString());
            }
        }
    }

    public void savePlayer(PlayerData playerData) {
        boolean homeUpdate = playerData.checkForHomeUpdates();
        if(playerData.isUpdate() || homeUpdate){
            Path playerFile = playerDirectory.toPath().resolve(playerData.getPlayerId()+".snbt");
            try{
                Files.writeString(playerFile, formatString(playerData.writeNbt().toString()));
                LOGGER.info("Saved Player File for {}", playerData.getPlayerName());
                playerData.clearUpdate();
            } catch (Exception e) {
                LOGGER.error("Error saving player",e);
            }
        }
    }

    public void saveWorld(MinecraftServer server){
        if(WORLD_DATA.isUpdate()){
            Path worldFile = worldDirectory.toPath().resolve(server.getWorldData().getLevelName()+".snbt");
            try{
                Files.writeString(worldFile, formatString(WORLD_DATA.writeNbt().toString()));
                LOGGER.info("Saved world file to {}", worldFile);
                WORLD_DATA.clearUpdate();
            } catch (Exception e){
                LOGGER.error("Error saving world", e);
            }
        }
    }

    public void savePlayers(MinecraftServer server){
        LOGGER.info("Saving all player data..");
        server.getPlayerList().getPlayers().stream().map(ServerPlayer::getUUID).forEach(
                UUID -> {
                    if(PLAYERS.containsKey(UUID)){
                        savePlayer(PLAYERS.get(UUID));
                    }
                }
        );
        LOGGER.info("All player data saved.");
    }

    public static Optional<PlayerData> getOrCreatePlayer(MinecraftServer server, UUID playerId) {
        if (PLAYERS.containsKey(playerId)) {
            return Optional.of(PLAYERS.get(playerId));
        }

        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        return Optional.ofNullable(player)
                .map(found -> PLAYERS.computeIfAbsent(playerId,
                        k -> new PlayerData(playerId, found.getName().getString())));
    }

    public static Optional<PlayerData> getOrCreatePlayer(@Nullable Player player) {
        if (player == null) {
            return Optional.empty();
        }

        return Optional.of(PLAYERS.computeIfAbsent(player.getUUID(),
                k -> new PlayerData(player.getUUID(), player.getName().getString())));
    }

    public static Optional<WorldData> getOrCreateWorld(MinecraftServer server){
        return Optional.of(Objects.requireNonNullElseGet(WORLD_DATA, () -> new WorldData(server.getWorldData().getLevelName())));
    }

    public void onServerTick(){
        currentTick++;
        if(currentTick % SyrupEssentialsConfig.get().persistence().autosaveIntervalTicks() == 0){
            savePlayers(minecraftServer);
            saveWorld(minecraftServer);
        }
    }

    public void flush() {
        WORLD_DATA = null;
        PLAYERS.clear();
        currentTick = 0;
    }
}
