package net.syrupstudios.syrupessentials.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.syrupstudios.syrupessentials.data.PlayerData;
import net.syrupstudios.syrupessentials.util.DataManager;
import net.syrupstudios.syrupessentials.util.TeleportPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(TeleportCommand.class)
public class TeleportCommandMixin {

    @Inject(
            method = "performTeleport",
            at = @At("HEAD")
    )
    private static void beforeTeleport(
            CommandSourceStack source,
            Entity target,
            ServerLevel level,
            double x,
            double y,
            double z,
            Set<?> relativeMovements,
            float yRot,
            float xRot,
            @Coerce Object lookAt,
            CallbackInfo ci
    ) {
        if (!(target instanceof ServerPlayer serverPlayer)) {
            return;
        }

        DataManager.getOrCreatePlayer(serverPlayer)
                .orElseThrow()
                .addTeleportHistory(
                        new TeleportPos(target.level(), target.position(), target.getXRot(), target.getYRot()));
    }
}
