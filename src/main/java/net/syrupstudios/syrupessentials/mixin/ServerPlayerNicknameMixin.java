package net.syrupstudios.syrupessentials.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.syrupstudios.syrupessentials.data.PlayerData;
import net.syrupstudios.syrupessentials.util.DataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerNicknameMixin {
    @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
    private void syrupEssentials$tabNickname(CallbackInfoReturnable<Component> callback) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        PlayerData data = DataManager.getOrCreatePlayer(player).orElse(null);
        if (data != null && data.getNickname() != null && !data.getNickname().isBlank()) {
            Component base = callback.getReturnValue() == null ? player.getDisplayName() : callback.getReturnValue();
            callback.setReturnValue(Component.literal(data.getNickname()).withStyle(base.getStyle()));
        }
    }
}
