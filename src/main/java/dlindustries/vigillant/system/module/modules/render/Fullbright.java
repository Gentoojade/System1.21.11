package dlindustries.vigillant.system.module.modules.render;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public final class Fullbright extends Module implements TickListener {
    public Fullbright() {
        super(EncryptedString.of("FullBright"),
                EncryptedString.of("Night vision"),
                -1,
                Category.RENDER);
    }
    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        super.onEnable();
    }
    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        if (mc.player != null) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
        super.onDisable();
    }
    @Override
    public void onTick() {
        if (mc.player != null) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false, false));
        }
    }
}