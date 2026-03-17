package dlindustries.vigillant.system.module.modules.mace;

import dlindustries.vigillant.system.event.events.AttackListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.utils.EncryptedString;

public final class SpearSwap extends Module implements AttackListener, TickListener {
    private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true)
            .setDescription(EncryptedString.of("Switch back to previous slot after attack"));
    private final NumberSetting switchDelay = new NumberSetting(
            EncryptedString.of("Switch Delay"),
            0, 250, 50, 1
    ).setDescription(EncryptedString.of("Delay after attacking before switching back"));

    private final NumberSetting breachSlot = new NumberSetting(EncryptedString.of("Spear's Slot"), 1, 9, 8, 1)
            .setDescription(EncryptedString.of("Slot 1-9 for where you put your spear"));

    private final BooleanSetting requireKey = new BooleanSetting(EncryptedString.of("Require Key"), true)
            .setDescription(EncryptedString.of("Require holding the key to trigger spear swap"));

    private final KeybindSetting activateKey = new KeybindSetting(
            EncryptedString.of("Activate Key"),
            -1,
            false
    );
    private boolean shouldSwitchBack;
    private int originalSlot = -1;
    private int waitTicksRemaining;   // ticks left before switching back
    public SpearSwap() {
        super(EncryptedString.of("Spear Swap"),
                EncryptedString.of("Swaps to selected spear after attacking"),
                -1,
                Category.mace);
        addSettings(switchBack, switchDelay, breachSlot, requireKey, activateKey);
    }
    @Override
    public void onEnable() {
        eventManager.add(AttackListener.class, this);
        eventManager.add(TickListener.class, this);
        resetState();
        super.onEnable();
    }
    @Override
    public void onDisable() {
        eventManager.remove(AttackListener.class, this);
        eventManager.remove(TickListener.class, this);
        if (shouldSwitchBack && originalSlot != -1) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
        super.onDisable();
    }
    @Override
    public void onAttack(AttackEvent event) {
        if (requireKey.getValue()) {
            if (activateKey.getKey() == 0 || !activateKey.isPressed()) return;
        }
        if (shouldSwitchBack) {
            return;
        }
        int slotIndex = breachSlot.getValueInt() - 1;
        if (switchBack.getValue() && originalSlot == -1) {
            originalSlot = mc.player.getInventory().getSelectedSlot();
        }
        mc.player.getInventory().setSelectedSlot(slotIndex);
        if (switchBack.getValue()) {
            shouldSwitchBack = true;
            waitTicksRemaining = msToTicks(switchDelay.getValueInt());
        }
    }
    @Override
    public void onTick() {
        if (!shouldSwitchBack || originalSlot == -1) return;
        if (waitTicksRemaining > 0) {
            waitTicksRemaining--;
            return;
        }
        mc.player.getInventory().setSelectedSlot(originalSlot);
        resetState();
    }
    private int msToTicks(int ms) {
        if (ms <= 0) return 0;
        return (int) Math.ceil(ms / 50.0);
    }
    private void resetState() {
        shouldSwitchBack = false;
        originalSlot = -1;
        waitTicksRemaining = 0;
    }
}