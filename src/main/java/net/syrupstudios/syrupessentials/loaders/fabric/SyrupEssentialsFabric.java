package net.syrupstudios.syrupessentials.loaders.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.syrupstudios.syrupessentials.SyrupEssentials;

public final class SyrupEssentialsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SyrupEssentials.initialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                SyrupEssentials.registerCommands(dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(SyrupEssentials::serverStarted);
        ServerTickEvents.START_SERVER_TICK.register(SyrupEssentials::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                SyrupEssentials.playerJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                SyrupEssentials.playerLeave(handler.getPlayer()));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
                SyrupEssentials.saveDeathLocation(player);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(SyrupEssentials::serverStopping);
    }
}
