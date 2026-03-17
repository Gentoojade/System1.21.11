package dlindustries.vigillant.system.module.modules.sword;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import dlindustries.vigillant.system.utils.KeyUtils;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

public final class LavaKey extends Module implements TickListener {

    private final KeybindSetting activateKey =
            new KeybindSetting(EncryptedString.of("Activate Key"), -1, false);
    private final NumberSetting delayMs = new NumberSetting(
            EncryptedString.of("Delay"),
            0, 250, 50, 1
    ).setDescription(EncryptedString.of("Delay between actions in milliseconds"));

    private boolean active;
    private int previousSlot = -1;
    private int tickStage = 0;
    private boolean wasKeyPressed = false;
    private int waitTicksRemaining = 0;   // ticks left to wait
    private BlockPos placedLavaPos;
    public LavaKey() {
        super(
                EncryptedString.of("Lava Key"),
                EncryptedString.of("Places lava and picks it back up using interactItem"),
                -1,
                Category.sword
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
        if (previousSlot != -1 && previousSlot != mc.player.getInventory().getSelectedSlot()) {
            InventoryUtils.setInvSlot(previousSlot);
        }
        eventManager.remove(TickListener.class, this);
        super.onDisable();
    }
    private int msToTicks(int ms) {
        if (ms <= 0) return 0;
        return (int) Math.ceil(ms / 50.0);
    }
    @Override
    public void onTick() {
        if (mc.currentScreen != null)
            return;

        boolean keyPressed = KeyUtils.isKeyPressed(activateKey.getKey());
        if (keyPressed && !wasKeyPressed && !active) {
            active = true;
        }
        wasKeyPressed = keyPressed;

        if (!active)
            return;

        if (previousSlot == -1)
            previousSlot = mc.player.getInventory().getSelectedSlot();

        switch (tickStage) {
            case 0 -> {
                InventoryUtils.selectItemFromHotbar(Items.LAVA_BUCKET);
                waitTicksRemaining = msToTicks((int) delayMs.getValue());
                tickStage++;
            }
            case 1 -> {
                if (waitTicksRemaining > 0) {
                    waitTicksRemaining--;
                    return;
                }
                if (mc.player.getMainHandStack().getItem() != Items.LAVA_BUCKET) {
                    reset();
                    return;
                }
                if (mc.crosshairTarget instanceof BlockHitResult blockHit) {
                    placedLavaPos = blockHit.getBlockPos().offset(blockHit.getSide());
                } else {
                    reset();
                    return;
                }
                ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                if (result.isAccepted()) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                    tickStage++;
                } else {
                    reset();
                }
            }
            case 2 -> {
                waitTicksRemaining = msToTicks((int) delayMs.getValue());
                tickStage++;
            }
            case 3 -> {
                if (waitTicksRemaining > 0) {
                    waitTicksRemaining--;
                    return;
                }
                if (mc.player.getMainHandStack().getItem() != Items.BUCKET) {
                    reset();
                    return;
                }
                ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                if (result.isAccepted()) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                }
                tickStage++;
            }
            case 4 -> {
                if (previousSlot != -1)
                    InventoryUtils.setInvSlot(previousSlot);
                reset();
            }
        }
    }
    private void reset() {
        previousSlot = -1;
        tickStage = 0;
        active = false;
        waitTicksRemaining = 0;
        placedLavaPos = null;
    }
}