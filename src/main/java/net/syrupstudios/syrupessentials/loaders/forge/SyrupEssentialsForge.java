package net.syrupstudios.syrupessentials.loaders.forge;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.syrupstudios.syrupessentials.SyrupEssentials;

@Mod(SyrupEssentials.MOD_ID)
public final class SyrupEssentialsForge {
    public SyrupEssentialsForge() {
        SyrupEssentials.initialize();
        MinecraftForge.EVENT_BUS.register(this);
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
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            SyrupEssentials.tick(event.getServer());
        }
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
