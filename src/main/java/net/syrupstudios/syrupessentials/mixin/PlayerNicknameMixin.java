package net.syrupstudios.syrupessentials.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.syrupstudios.syrupessentials.data.PlayerData;
import net.syrupstudios.syrupessentials.util.DataManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerNicknameMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void syrupEssentials$displayNickname(CallbackInfoReturnable<Component> callback) {
        Player player = (Player) (Object) this;
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        PlayerData data = DataManager.getOrCreatePlayer(player).orElse(null);
        if (data != null && data.getNickname() != null && !data.getNickname().isBlank()) {
            callback.setReturnValue(Component.literal(data.getNickname())
                    .withStyle(callback.getReturnValue().getStyle()));
        }
    }
}
