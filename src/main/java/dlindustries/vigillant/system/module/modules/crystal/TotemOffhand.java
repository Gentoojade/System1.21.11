package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.InventoryUtils;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ThreadLocalRandom;

public final class TotemOffhand extends Module implements TickListener {
    private final NumberSetting minSlotDelay = new NumberSetting("Min Slot Delay", 0, 200, 80, 1);
    private final NumberSetting maxSlotDelay = new NumberSetting("Max Slot Delay", 0, 250, 100, 1);
    private final NumberSetting offhandDelay = new NumberSetting("Offhand Delay", 0, 50, 30, 1);
    private final BooleanSetting dynamicJitter = new BooleanSetting("Dynamic Jitter", true);
    private final BooleanSetting switchBack = new BooleanSetting("Switch Back", false);
    private long nextActionTime = 0;
    private int previousSlot = -1;
    private boolean sentSwapPacket = false;
    private boolean isActive = false;
    private long lastTotemTime = 0;
    private final Deque<Long> recentDelays = new ArrayDeque<>();
    private double currentJitterFactor = 1.0;
    public TotemOffhand() {
        super("Auto offhand", "Automatically offhands the totem", -1, Category.CRYSTAL);
        addSettings(minSlotDelay, maxSlotDelay, offhandDelay, dynamicJitter, switchBack);
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
        if (shouldPause()) {
            reset();
            return;
        }
        if (needsTotem()) {
            isActive = true;
            long now = System.currentTimeMillis();

            if (now - lastTotemTime < getPingAdjustedThreshold()) {
                return;
            }

            if (now < nextActionTime) {
                return;
            }
            executeStealthProtocol(now);
        }
    }
    private boolean shouldPause() {
        return mc.currentScreen != null || !mc.player.isAlive() || mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
    }
    private boolean needsTotem() {
        return mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING
                && InventoryUtils.hasItem(Items.TOTEM_OF_UNDYING);
    }
    private int getPing() {
        if (mc.getNetworkHandler() == null) return 0;
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }
    private void executeStealthProtocol(long now) {
        if (sentSwapPacket && switchBack.getValue()) {
            if (previousSlot != -1) {
                mc.player.getInventory().setSelectedSlot(previousSlot);
            }
            reset();
            return;
        }
        if (previousSlot == -1) {
            initializeSlotChange(now);
            return;
        }
        if (!sentSwapPacket) {
            performDeceptiveSwap(now);
            return;
        }
        finalizeSwap(now);
    }
    private void initializeSlotChange(long now) {
        previousSlot = mc.player.getInventory().getSelectedSlot();
        int baseDelay = ThreadLocalRandom.current().nextInt(
                minSlotDelay.getValueInt(),
                maxSlotDelay.getValueInt() + 1
        );
        if (dynamicJitter.getValue()) {
            currentJitterFactor = 0.8 + (ThreadLocalRandom.current().nextDouble() * 0.4);
            baseDelay = (int) (baseDelay * currentJitterFactor);
        }
        nextActionTime = now + baseDelay;
    }
    private void performDeceptiveSwap(long now) {
        if (InventoryUtils.selectItemFromHotbar(Items.TOTEM_OF_UNDYING)) {
            double gaussian = ThreadLocalRandom.current().nextGaussian() * 5;
            int uniformJitter = ThreadLocalRandom.current().nextInt(-10, 10);
            int finalDelay = (int) (offhandDelay.getValueInt() + gaussian + uniformJitter);
            finalDelay = Math.max(25, Math.min(60, finalDelay));
            nextActionTime = now + finalDelay;
            sentSwapPacket = true;
        } else {
            reset();
        }
    }
    private void finalizeSwap(long now) {
        if (mc.getNetworkHandler() == null) return;
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND,
                BlockPos.ORIGIN,
                Direction.DOWN
        ));
        if (switchBack.getValue()) {
            int switchBackDelay = ThreadLocalRandom.current().nextInt(15, 25);
            nextActionTime = now + switchBackDelay;
        } else {
            reset();
        }

        lastTotemTime = now;
    }
    private long getPingAdjustedThreshold() {
        return Math.max(50, getPing() + 20);
    }
    private void reset() {
        nextActionTime = 0;
        previousSlot = -1;
        sentSwapPacket = false;
        isActive = false;
    }
}