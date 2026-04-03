package dlindustries.vigillant.system.module.modules.mace;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.KeyUtils;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public final class KeyWindCharge extends Module implements TickListener {

    private final KeybindSetting activateKey =
            new KeybindSetting(EncryptedString.of("Activate Key"), -1, false);
    private final NumberSetting delayMs =
            new NumberSetting(EncryptedString.of("Delay"), 0, 250, 50, 1);

    private boolean wasKeyPressed = false;
    private int originalSlot = -1;
    private boolean needsSwitchBack = false;

    private long lastThrowTime = 0;
    private long switchBackTime = 0;

    public KeyWindCharge() {
        super(
                EncryptedString.of("Key Wind Charge"),
                EncryptedString.of("Optimizes your wind charge usage speed"),
                -1,
                Category.mace
        );
        addSettings(activateKey, delayMs);
    }

    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (needsSwitchBack && originalSlot != -1) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
        eventManager.remove(TickListener.class, this);
        reset();
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.currentScreen != null) return;

        long now = System.currentTimeMillis();
        boolean keyPressed = KeyUtils.isKeyPressed(activateKey.getKey());

        if (keyPressed && !wasKeyPressed && (now - lastThrowTime) >= (int) delayMs.getValue()) {
            throwWindCharge(now);
            lastThrowTime = now;
        }

        wasKeyPressed = keyPressed;

        if (needsSwitchBack && now >= switchBackTime) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
            needsSwitchBack = false;
            originalSlot = -1;
        }
    }

    private void throwWindCharge(long now) {
        int slot = findWindChargeSlot();
        if (slot == -1) return;

        if (mc.player.getItemCooldownManager().isCoolingDown(
                new net.minecraft.item.ItemStack(Items.WIND_CHARGE))) return;

        originalSlot = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);

        ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        if (result.isAccepted()) mc.player.swingHand(Hand.MAIN_HAND);

        needsSwitchBack = true;
        switchBackTime = now + (int) delayMs.getValue();
    }

    private int findWindChargeSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.WIND_CHARGE) return i;
        }
        return -1;
    }

    private void reset() {
        originalSlot = -1;
        needsSwitchBack = false;
        wasKeyPressed = false;
        lastThrowTime = 0;
        switchBackTime = 0;
    }
}