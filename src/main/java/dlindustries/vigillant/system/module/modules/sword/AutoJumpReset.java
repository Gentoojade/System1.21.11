package dlindustries.vigillant.system.module.modules.sword;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.MathUtils;
import org.lwjgl.glfw.GLFW;

public final class AutoJumpReset extends Module implements TickListener {
    private final NumberSetting chance = new NumberSetting(EncryptedString.of("Chance"), 0, 100, 100, 1);
    private final BooleanSetting simulateKey = new BooleanSetting(EncryptedString.of("Simulate Key Press"), true);
    private final BooleanSetting disableOnS = new BooleanSetting(EncryptedString.of("Disable on S press"), true);
    private int lastHitTick = -1;
    private int scheduledJumpTick = -1;
    private boolean isSimulatingKey = false;
    private int keyHoldTicks = 0;
    private int lastHurtTime = 0;
    private float prevFallDistance = 0f;
    public AutoJumpReset() {
        super(EncryptedString.of("Jump Reset"),
                EncryptedString.of("Human-like jump reset"),
                -1,
                Category.sword);
        addSettings(chance, simulateKey, disableOnS);
    }
    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        resetState();
        super.onEnable();
    }
    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        releaseJumpKey();
        super.onDisable();
    }
    private void resetState() {
        lastHitTick = -1;
        scheduledJumpTick = -1;
        isSimulatingKey = false;
        keyHoldTicks = 0;
        lastHurtTime = 0;
        prevFallDistance = 0f;
        releaseJumpKey();
    }
    private void releaseJumpKey() {
        if (mc.options == null) return;
        mc.options.jumpKey.setPressed(false);
    }
    private boolean isPressingS() {
        if (mc.getWindow() == null) return false;
        long windowHandle = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(windowHandle, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
    }
    private boolean isFallDamage() {
        return prevFallDistance > 3.0f;
    }
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        // Track fallDistance before it gets reset on landing
        prevFallDistance = (float) mc.player.fallDistance;
        if (isSimulatingKey) {
            if (--keyHoldTicks <= 0) {
                releaseJumpKey();
                isSimulatingKey = false;
            }
        }
        if (scheduledJumpTick != -1 && (mc.player.isDead() || mc.currentScreen != null)) {
            scheduledJumpTick = -1;
        }
        int currentHurtTime = mc.player.hurtTime;
        if (currentHurtTime > lastHurtTime && currentHurtTime == mc.player.maxHurtTime) {
            lastHitTick = mc.player.age;
            scheduledJumpTick = -1;

            if (mc.player.isOnGround()
                    && !mc.player.isOnFire()
                    && !isFallDamage()
                    && MathUtils.randomInt(1, 100) <= chance.getValueInt()
                    && (!disableOnS.getValue() || !isPressingS())) {
                int delay = MathUtils.randomInt(2, 3);
                scheduledJumpTick = lastHitTick + delay;
            }
        }
        lastHurtTime = currentHurtTime;
        if (scheduledJumpTick != -1 && mc.player.age >= scheduledJumpTick) {
            if (mc.currentScreen == null
                    && !mc.player.isUsingItem()
                    && mc.player.isOnGround()
                    && !mc.player.isOnFire()
                    && mc.player.hurtTime > 0
                    && (!disableOnS.getValue() || !isPressingS())) {
                if (simulateKey.getValue()) {
                    if (!isSimulatingKey) {
                        mc.options.jumpKey.setPressed(true);
                        isSimulatingKey = true;
                        keyHoldTicks = MathUtils.randomInt(1, 2);
                    }
                } else {
                    mc.player.jump();
                }
            }
            scheduledJumpTick = -1;
        }
        if (isSimulatingKey && (mc.player == null || mc.world == null)) {
            releaseJumpKey();
            isSimulatingKey = false;
        }
    }
}