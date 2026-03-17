package dlindustries.vigillant.system.module.modules.mace;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.KeyUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class FireworkMacro extends Module implements TickListener {

    private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), 32, false);
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay (ms)"), 0, 250, 50, 1)
            .setDescription(EncryptedString.of("Delay in milliseconds for using firework"));
    private static final int USE_DELAY_MS = 50;
    private boolean active, hasSwitched, hasUsed, hasSwitchedBack;
    private long switchStartTime, useStartTime, switchBackStartTime;
    private boolean wasKeyPressed;
    private int originalSlot;
    private int fireworkSlotIndex = -1;
    public FireworkMacro() {
        super(
                EncryptedString.of("Firework Macro"),
                EncryptedString.of("Press a key while flying to automatically use fireworks"),
                -1,
                Category.mace
        );
        addSettings(activateKey, switchDelay);
    }
    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        reset();
        super.onEnable();
    }
    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        reset();
        super.onDisable();
    }
    @Override
    public void onTick() {
        if (mc.currentScreen != null) return;

        handleKeyDetection();
        handleFlightMonitoring();
        handleFireworkSequence();
    }
    private void handleKeyDetection() {
        boolean keyPressed = KeyUtils.isKeyPressed(activateKey.getKey());
        if (keyPressed && !wasKeyPressed) {
            if (mc.player != null && mc.player.isGliding() && !active) {
                int foundSlot = findFireworkSlot();
                if (foundSlot != -1) {
                    originalSlot = mc.player.getInventory().getSelectedSlot();
                    fireworkSlotIndex = foundSlot;
                    active = true;
                }
            }
        }
        wasKeyPressed = keyPressed;
    }
    private int findFireworkSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == Items.FIREWORK_ROCKET) {
                return i;
            }
        }
        return -1;
    }
    private void handleFlightMonitoring() {
        if (mc.player == null) return;
        if (active && !mc.player.isGliding()) {
            reset();
        }
    }
    private void handleFireworkSequence() {
        if (!active || mc.player == null) return;
        long now = System.currentTimeMillis();
        if (!hasSwitched) {
            if (switchStartTime == 0) {
                switchStartTime = now;
            } else if (now - switchStartTime >= switchDelay.getValueInt()) {
                mc.player.getInventory().setSelectedSlot(fireworkSlotIndex);
                hasSwitched = true;
                switchStartTime = 0;
            }
        } else if (!hasUsed) {
            if (useStartTime == 0) {
                useStartTime = now;
            } else if (now - useStartTime >= USE_DELAY_MS) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                hasUsed = true;
                useStartTime = 0;
            }
        } else if (!hasSwitchedBack) {
            if (switchBackStartTime == 0) {
                switchBackStartTime = now;
            } else if (now - switchBackStartTime >= switchDelay.getValueInt()) {
                mc.player.getInventory().setSelectedSlot(originalSlot);
                hasSwitchedBack = true;
                switchBackStartTime = 0;
                reset();
            }
        }
    }
    private void reset() {
        active = false;
        hasSwitched = false;
        hasUsed = false;
        hasSwitchedBack = false;
        switchStartTime = 0;
        useStartTime = 0;
        switchBackStartTime = 0;
        fireworkSlotIndex = -1;
    }
}