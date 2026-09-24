package net.syrupstudios.syrupessentials.data;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Data;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.syrupstudios.syruplibrary.teleport.SyrupTeleports;
import org.slf4j.Logger;

import java.util.Optional;

@Data
public class WorldData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private Warps warps;
    private String levelName;
    private boolean update = false;

    public WorldData(String levelName){
        this.warps = new Warps();
        this.levelName = levelName;
    }

    public WorldData(Warps warps, String levelName){
        this.warps = warps;
        this.levelName = levelName;
    }

    public void triggerUpdate(){
        this.update = true;
    }

    public void clearUpdate(){
        this.update = false;
        this.warps.clearUpdate();
    }

    public static final Codec<WorldData> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Warps.CODEC.fieldOf("warps").forGetter(worldData -> worldData.warps),
            Codec.STRING.fieldOf("levelName").forGetter(worldData -> worldData.levelName)
        ).apply(builder, WorldData::new));

    public void readNbt(CompoundTag tag){
        Optional<WorldData> worldData = CODEC.parse(NbtOps.INSTANCE, tag.get("worldData"))
                .resultOrPartial(LOGGER::error);

        worldData.ifPresent(data -> {
            warps = data.getWarps();
            levelName = data.getLevelName();
        });
    }

    public CompoundTag writeNbt() {
        CompoundTag tag = new CompoundTag();
        tag.put("worldData",CODEC.encodeStart(NbtOps.INSTANCE, this).resultOrPartial(LOGGER::error).orElse(null));
        clearUpdate();
        return tag;
    }

    public void createWarp(String name, ServerPlayer serverPlayer) {
        this.warps.addLocation(name, SyrupTeleports.capture(serverPlayer));
        triggerUpdate();
    }

    public void deleteWarp(String name) {
        this.warps.removeLocation(name);
        triggerUpdate();
    }

}
