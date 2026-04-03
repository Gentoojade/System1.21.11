package dlindustries.vigillant.system.module.modules.sword;

import dlindustries.vigillant.system.event.events.ItemUseListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.InventoryUtils;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashSet;
import java.util.Set;

public final class AutoDrain extends Module implements TickListener, ItemUseListener {

    private final NumberSetting delayMs = new NumberSetting(
            EncryptedString.of("Delay"),
            0, 250, 25, 1
    ).setDescription(EncryptedString.of("Delay between drain actions in milliseconds"));
    private final BooleanSetting skipOwn = new BooleanSetting(
            EncryptedString.of("No Own"), true
    ).setDescription(EncryptedString.of("Don't drain water you placed yourself"));

    private final Set<BlockPos> ownedWater = new HashSet<>();
    private int previousSlot = -1;
    private int tickStage = 0;
    private int waitTicksRemaining = 0;
    public AutoDrain() {
        super(
                EncryptedString.of("AutoDrain"),
                EncryptedString.of("Automatically picks up water sources you look at"),
                -1,
                Category.sword
        );
        addSettings(delayMs, skipOwn);
    }
    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        eventManager.add(ItemUseListener.class, this);
        reset();
        super.onEnable();
    }
    @Override
    public void onDisable() {
        if (previousSlot != -1 && previousSlot != mc.player.getInventory().getSelectedSlot()) {
            InventoryUtils.setInvSlot(previousSlot);
        }
        eventManager.remove(TickListener.class, this);
        eventManager.remove(ItemUseListener.class, this);
        reset();
        super.onDisable();
    }
    private int msToTicks(int ms) {
        if (ms <= 0) return 0;
        return (int) Math.ceil(ms / 50.0);
    }
    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        switch (tickStage) {
            case 0 -> {
                if (!(mc.crosshairTarget instanceof BlockHitResult blockHit)) return;
                BlockPos pos = blockHit.getBlockPos();
                var fluidState = mc.world.getFluidState(pos);
                if (!fluidState.isOf(Fluids.WATER) || !fluidState.isStill()) return;
                if (skipOwn.getValue() && ownedWater.contains(pos)) return;
                if (!InventoryUtils.selectItemFromHotbar(Items.BUCKET)) return;
                previousSlot = mc.player.getInventory().getSelectedSlot();
                waitTicksRemaining = msToTicks((int) delayMs.getValue());
                tickStage = 1;
            }
            case 1 -> {
                if (waitTicksRemaining > 0) {
                    waitTicksRemaining--;
                    return;
                }
                var result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                if (result.isAccepted()) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                    if (mc.crosshairTarget instanceof BlockHitResult blockHit) {
                        ownedWater.remove(blockHit.getBlockPos());
                    }
                }
                waitTicksRemaining = msToTicks((int) delayMs.getValue());
                tickStage = 2;
            }
            case 2 -> {
                if (waitTicksRemaining > 0) {
                    waitTicksRemaining--;
                    return;
                }
                if (previousSlot != -1) {
                    InventoryUtils.setInvSlot(previousSlot);
                }
                reset();
            }
        }
    }
    @Override
    public void onItemUse(ItemUseEvent event) {
        if (!(mc.crosshairTarget instanceof BlockHitResult hitResult)) return;
        if (hitResult.getType() != HitResult.Type.BLOCK) return;
        if (!mc.player.getMainHandStack().isOf(Items.WATER_BUCKET)) return;
        BlockPos pos = hitResult.getBlockPos();
        Direction dir = hitResult.getSide();
        if (!mc.world.getBlockState(pos).isReplaceable()) {
            switch (dir) {
                case UP    -> ownedWater.add(pos.add(0,  1,  0));
                case DOWN  -> ownedWater.add(pos.add(0, -1,  0));
                case EAST  -> ownedWater.add(pos.add(1,  0,  0));
                case WEST  -> ownedWater.add(pos.add(-1, 0,  0));
                case NORTH -> ownedWater.add(pos.add(0,  0, -1));
                case SOUTH -> ownedWater.add(pos.add(0,  0,  1));
            }
        } else {
            ownedWater.add(pos);
        }
    }
    private void reset() {
        previousSlot = -1;
        tickStage = 0;
        waitTicksRemaining = 0;
    }
}