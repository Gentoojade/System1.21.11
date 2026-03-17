package dlindustries.vigillant.system.module.modules.mace;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import dlindustries.vigillant.system.utils.KeyUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import java.util.Set;

public final class KeyElytra extends Module implements TickListener {

    private final KeybindSetting activateKey =
            new KeybindSetting(EncryptedString.of("Activate Key"), -1, false);

    private final NumberSetting useDelay =
            new NumberSetting(EncryptedString.of("Use Delay"), 0, 250, 50, 1)
                    .setDescription(EncryptedString.of("Milliseconds to elytra/chestplate"));
    private boolean active;
    private boolean keyWasDown;
    private int previousSlot = -1;
    private int targetSlot = -1;
    private int currentStage = 0;
    private long stageStartTime = 0;
    private static final Set<net.minecraft.item.Item> CHESTPLATE_ITEMS = Set.of(
            Items.LEATHER_CHESTPLATE,
            Items.CHAINMAIL_CHESTPLATE,
            Items.IRON_CHESTPLATE,
            Items.GOLDEN_CHESTPLATE,
            Items.DIAMOND_CHESTPLATE,
            Items.NETHERITE_CHESTPLATE
    );
    public KeyElytra() {
        super(
                EncryptedString.of("Elytra Swap"),
                EncryptedString.of("Auto‑detects elytra/chestplate in hotbar and equips it instantly"),
                -1,
                Category.mace
        );
        addSettings(activateKey, useDelay);
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
        if (mc.currentScreen != null)
            return;
        boolean keyDown = KeyUtils.isKeyPressed(activateKey.getKey());
        if (keyDown && !keyWasDown) {
            int foundSlot = findEquippableChestItem();
            if (foundSlot != -1) {
                active = true;
                previousSlot = mc.player.getInventory().getSelectedSlot();
                targetSlot = foundSlot;
                currentStage = 0;
                stageStartTime = System.currentTimeMillis();
            }
        }
        keyWasDown = keyDown;
        if (!active)
            return;
        long now = System.currentTimeMillis();
        switch (currentStage) {
            case 0:
                InventoryUtils.setInvSlot(targetSlot);
                currentStage = 1;
                stageStartTime = now;
                break;
                case 1:
                if (now - stageStartTime >= useDelay.getValueInt()) {
                    ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    if (result.isAccepted())
                        mc.player.swingHand(Hand.MAIN_HAND);
                    currentStage = 2;
                    stageStartTime = now;
                }
                break;

            case 2:
                if (previousSlot != -1) {
                    mc.player.getInventory().setSelectedSlot(previousSlot);
                }
                reset();
                break;
        }
    }
    private int findEquippableChestItem() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == Items.ELYTRA) {
                return i;
            }
            if (CHESTPLATE_ITEMS.contains(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }
    private void reset() {
        active = false;
        previousSlot = -1;
        targetSlot = -1;
        currentStage = 0;
        stageStartTime = 0;
    }
}