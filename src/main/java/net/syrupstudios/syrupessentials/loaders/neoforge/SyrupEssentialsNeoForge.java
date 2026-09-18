package net.syrupstudios.syrupessentials.loaders.neoforge;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.syrupstudios.syrupessentials.SyrupEssentials;

@Mod(SyrupEssentials.MOD_ID)
public final class SyrupEssentialsNeoForge {
    public SyrupEssentialsNeoForge() {
        SyrupEssentials.initialize();
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        SyrupEssentials.registerCommands(event.getDispatcher());
    }

    @SubscribeEvent
    public void serverStarted(ServerStartedEvent event) {
        SyrupEssentials.serverStarted(event.getServer());
    }

    @SubscribeEvent
    public void serverTick(ServerTickEvent.Pre event) {
        SyrupEssentials.tick(event.getServer());
    }

    @SubscribeEvent
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SyrupEssentials.playerJoin(player);
        }
    }

    @SubscribeEvent
    public void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SyrupEssentials.playerLeave(player);
        }
    }

    @SubscribeEvent
    public void playerDied(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SyrupEssentials.saveDeathLocation(player);
        }
    }

    @SubscribeEvent
    public void serverStopping(ServerStoppingEvent event) {
        SyrupEssentials.serverStopping(event.getServer());
    }
}
