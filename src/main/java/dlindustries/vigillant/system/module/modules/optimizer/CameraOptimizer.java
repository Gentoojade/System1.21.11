package dlindustries.vigillant.system.module.modules.optimizer;

import dlindustries.vigillant.system.event.events.CameraUpdateListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.KeyUtils;

public final class CameraOptimizer extends Module implements CameraUpdateListener {
    private final KeybindSetting toggleKey = new KeybindSetting(
            EncryptedString.of("Toggle Key"),
            -1,
            false
    );
    private final BooleanSetting noClip = new BooleanSetting(
            EncryptedString.of("No Clip"),
            true
    ).setDescription(EncryptedString.of("Allows camera to pass through blocks"));
    private final BooleanSetting noOverlay = new BooleanSetting(
            EncryptedString.of("No Overlay"),
            true
    ).setDescription(EncryptedString.of("Removes water and lava visual effects"));
    private final BooleanSetting alwaysOn = new BooleanSetting(
            EncryptedString.of("Always On"),
            true
    ).setDescription(EncryptedString.of("If enabled, effects are active whenever the module is on"));
    public CameraOptimizer() {
        super(
                EncryptedString.of("Camera Optimizer"),
                EncryptedString.of("Improves camera behavior and removes restrictions Pairs well with the Freelook Mod"),
                -1,
                Category.RENDER
        );
        addSettings(toggleKey, noClip, noOverlay, alwaysOn);
    }
    @Override
    public void onEnable() {
        eventManager.add(CameraUpdateListener.class, this);
        super.onEnable();
    }
    @Override
    public void onDisable() {
        eventManager.remove(CameraUpdateListener.class, this);
        super.onDisable();
    }
    @Override
    public void onCameraUpdate(CameraUpdateEvent event) {
    }
    public boolean isToggleKeyPressed() {
        return toggleKey.getKey() != -1 && KeyUtils.isKeyPressed(toggleKey.getKey());
    }
    public boolean isNoClipEnabled() {
        if (!isEnabled() || !noClip.getValue()) return false;
        return alwaysOn.getValue() || isToggleKeyPressed();
    }
    public boolean isNoOverlayEnabled() {
        if (!isEnabled() || !noOverlay.getValue()) return false;
        return alwaysOn.getValue() || isToggleKeyPressed();
    }
}