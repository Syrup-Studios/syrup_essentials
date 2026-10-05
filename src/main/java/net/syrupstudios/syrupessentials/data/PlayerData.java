package net.syrupstudios.syrupessentials.data;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Data;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.syrupstudios.syrupessentials.config.SyrupEssentialsConfig;
import net.syrupstudios.syrupessentials.util.DataManager;
import net.syrupstudios.syruplibrary.teleport.SyrupTeleports;
import net.syrupstudios.syruplibrary.teleport.TeleportTarget;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.Optional;
import java.util.UUID;

@Data
public class PlayerData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final UUID playerId;
    private final String playerName;
    @Nullable
    private TeleportTarget lastLocation;
    private Homes homes;
    private LinkedList<TeleportTarget> locationHistory;
    private boolean update;
    private boolean isMuted;
    private boolean canFly;
    private boolean godmode;
    private String nickname;

    public PlayerData(UUID playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.update = false;
        this.lastLocation = null;
        this.homes = new Homes();
        this.locationHistory = new LinkedList<>();
        this.isMuted = false;
        this.canFly = false;
        this.nickname = "";
    }

    public PlayerData(UUID playerId, String playerName, Homes homes, boolean isMuted, boolean canFly, boolean godmode, String nickname) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.homes = homes;
        this.isMuted = isMuted;
        this.canFly = canFly;
        this.godmode = godmode;
        this.nickname = nickname;
    }

    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            UUIDUtil.CODEC.fieldOf("playerId").forGetter(playerData -> playerData.playerId),
            Codec.STRING.fieldOf("playerName").forGetter(playerData ->playerData.playerName),
            Homes.CODEC.fieldOf("homes").forGetter(playerData -> playerData.homes),
            Codec.BOOL.fieldOf("isMuted").forGetter(playerData -> playerData.isMuted),
            Codec.BOOL.fieldOf("canFly").forGetter(playerData -> playerData.canFly),
            Codec.BOOL.fieldOf("godmode").forGetter(playerData -> playerData.godmode),
            Codec.STRING.fieldOf("nickname").forGetter(playerData ->playerData.nickname)
        ).apply(builder, PlayerData::new));

    public void triggerUpdate(){
        this.update = true;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
        triggerUpdate();
    }

    public void clearUpdate(){
        this.update = false;
        this.getHomes().clearUpdate();
    }

    public boolean checkForHomeUpdates() {
        return homes.requiresUpdate();
    }

    public static void addTeleportHistory(ServerPlayer player, ResourceKey<Level> dimension, Vec3 pos) {
        DataManager.getOrCreatePlayer(player).ifPresent(data -> data.addTeleportHistory(
                new TeleportTarget(dimension, pos, player.getYRot(), player.getXRot())));
    }

    public void addTeleportHistory(ServerPlayer player) {
        addTeleportHistory(SyrupTeleports.capture(player));
    }

    public void addTeleportHistory(TeleportTarget pos) {
        locationHistory.add(pos);
        trimTeleportHistory();
    }

    public void addHome(String name, ServerPlayer serverPlayer){
        this.homes.addLocation(name, SyrupTeleports.capture(serverPlayer));
        triggerUpdate();
    }

    public void removeHome(String name) {
        this.homes.removeLocation(name);
        triggerUpdate();
    }

    public Optional<TeleportTarget> popLocationHistory() {
        trimTeleportHistory();
        if (!locationHistory.isEmpty()) {
            return Optional.of(locationHistory.removeLast());
        }
        return Optional.empty();
    }

    private void trimTeleportHistory() {
        int maximumHistory = SyrupEssentialsConfig.get().teleportation().back().maxHistory();
        while (locationHistory.size() > maximumHistory) {
            locationHistory.removeFirst();
        }
    }

    public void readNbt(CompoundTag tag){
        Optional<PlayerData> readPlayer = CODEC.parse(NbtOps.INSTANCE, tag.get("playerData"))
                .resultOrPartial(LOGGER::error);

        if (readPlayer.isPresent()){
            PlayerData playerData = readPlayer.get();
            this.lastLocation = playerData.lastLocation;
            this.homes = playerData.homes;
            this.isMuted = playerData.isMuted;
            this.canFly = playerData.canFly;
            this.godmode = playerData.godmode;
            this.nickname = playerData.nickname;
        }
    }

    public CompoundTag writeNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("playerData",CODEC.encodeStart(NbtOps.INSTANCE, this).resultOrPartial(LOGGER::error).orElse(null));
        clearUpdate();
        return tag;
    }
}
