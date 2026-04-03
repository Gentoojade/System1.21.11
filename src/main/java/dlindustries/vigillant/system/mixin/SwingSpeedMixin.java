package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.module.modules.render.SwingSpeed;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class SwingSpeedMixin {
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true)
    private void onGetHandSwingDuration(CallbackInfoReturnable<Integer> cir) {
        if (!(((Object) this) instanceof net.minecraft.entity.player.PlayerEntity)) return;
        SwingSpeed module = SwingSpeed.getInstance();
        if (module != null && module.isEnabled()) {
            cir.setReturnValue(21 - (int) SwingSpeed.speed.getValue());
        }
    }
}