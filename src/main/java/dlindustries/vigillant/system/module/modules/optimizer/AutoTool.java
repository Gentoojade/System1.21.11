package dlindustries.vigillant.system.module.modules.optimizer;

import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

public final class AutoTool extends Module {

    private static AutoTool instance;

    private final BooleanSetting restoreSlot = new BooleanSetting(
            EncryptedString.of("Restore Slot"),
            true
    ).setDescription(EncryptedString.of("Switches back to your previous slot after breaking"));

    private int previousSlot = -1;
    private boolean switched = false;

    public AutoTool() {
        super(
                EncryptedString.of("AutoTool"),
                EncryptedString.of("Automatically switches to the best tool for the block being mined"),
                -1,
                Category.optimizer
        );
        addSettings(restoreSlot);
        instance = this;
    }
    public static AutoTool getInstance() {
        return instance;
    }
    @Override
    public void onDisable() {
        restoreSlot();
        super.onDisable();
    }
    public void onStartBreaking(BlockState blockState) {
        if (mc.player == null || !isEnabled()) return;
        ItemStack held = mc.player.getInventory().getStack(mc.player.getInventory().getSelectedSlot());
        if (held.isIn(ItemTags.SWORDS)) return;
        int bestSlot = -1;
        float bestSpeed = -1f;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            float speed = stack.getMiningSpeedMultiplier(blockState);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        int currentSlot = mc.player.getInventory().getSelectedSlot();
        if (bestSlot != -1 && bestSlot != currentSlot) {
            previousSlot = currentSlot;
            InventoryUtils.setInvSlot(bestSlot);
            switched = true;
        }
    }
    public void onStopBreaking() {
        if (!isEnabled() || !switched || mc.player == null) return;
        restoreSlot();
    }
    private void restoreSlot() {
        if (switched && previousSlot != -1 && restoreSlot.getValue()) {
            InventoryUtils.setInvSlot(previousSlot);
        }
        previousSlot = -1;
        switched = false;
    }
}