package dlindustries.vigillant.system.module.modules.mace;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import dlindustries.vigillant.system.utils.KeyUtils;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public final class PearlCatch extends Module implements TickListener {
    private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), -1, false);

    private final NumberSetting delay = new NumberSetting(EncryptedString.of("Delay"), 1, 250, 10, 1)
            .setDescription(EncryptedString.of("Delay after throwing pearl before using wind charge in milliseconds"));
    private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true);
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 1, 250, 10, 1)
            .setDescription(EncryptedString.of("Delay after using wind charge before switching back in milliseconds"));
    private enum State { IDLE, PEARL_THROWN, WIND_USED, DONE }
    private State state = State.IDLE;
    private boolean activatedByKey = false;
    private int previousSlot = -1;
    private long stepTime = 0;
    public PearlCatch() {
        super(EncryptedString.of("Pearl catch"), EncryptedString.of("Throws a pearl and then a Windcharge"), -1, Category.mace);
        addSettings(activateKey, delay, switchBack, switchDelay);
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
        super.onDisable();
    }
    @Override
    public void onTick() {
        if (mc.currentScreen != null) return;
        boolean keyPressed = KeyUtils.isKeyPressed(activateKey.getKey());
        if (keyPressed && !activatedByKey && state == State.IDLE) {
            activatedByKey = true;
            state = State.PEARL_THROWN;
        }
        if (!keyPressed) {
            activatedByKey = false;
        }
        long currentTime = System.currentTimeMillis();
        switch (state) {
            case PEARL_THROWN:
                if (previousSlot == -1)
                    previousSlot = mc.player.getInventory().getSelectedSlot();
                if (InventoryUtils.selectItemFromHotbar(Items.ENDER_PEARL)) {
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
                stepTime = currentTime;
                state = State.WIND_USED;
                break;
                case WIND_USED:
                if (currentTime - stepTime < delay.getValueInt()) return;
                if (InventoryUtils.selectItemFromHotbar(Items.WIND_CHARGE)) {
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
                stepTime = currentTime;
                if (switchBack.getValue()) {
                    state = State.DONE;
                } else {
                    reset();
                }
                break;
                case DONE:
                if (currentTime - stepTime >= switchDelay.getValueInt()) {
                    if (previousSlot != -1) {
                        InventoryUtils.setInvSlot(previousSlot);
                    }
                    reset();
                }
                break;
                case IDLE: default:
                break;
        }
    }
    private void reset() {
        previousSlot = -1;
        stepTime = 0;
        state = State.IDLE;
    }
}