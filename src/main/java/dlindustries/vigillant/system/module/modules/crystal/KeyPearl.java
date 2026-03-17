package dlindustries.vigillant.system.module.modules.crystal;

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
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public final class KeyPearl extends Module implements TickListener {

    private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), -1, false);
    private final NumberSetting delay = new NumberSetting(
            EncryptedString.of("Delay"),
            0, 250, 50, 1
    ).setDescription(EncryptedString.of("Delay after selecting pearl before throwing "));
    private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true)
            .setDescription(EncryptedString.of("Return to the original slot after throwing"));
    private final NumberSetting switchDelay = new NumberSetting(
            EncryptedString.of("Switch Delay"),
            0, 250, 50, 1
    ).setDescription(EncryptedString.of("Delay in milliseconds after throwing before switching back "));
    private final NumberSetting boostMultiplier = new NumberSetting(
            EncryptedString.of("Pearl Boost Multiplier"),
            1.0, 5.0, 1.0, 0.01
    ).setDescription(EncryptedString.of("How much faster pearls travel when thrown by this module (1.0 = normal), detected by AC"));
    private final BooleanSetting boostFirstTickOnly = new BooleanSetting(
            EncryptedString.of("Only First Tick"), true
    ).setDescription(EncryptedString.of("Boost only applies on the first tick"));
    private enum Stage {
        IDLE,
        WAITING_TO_THROW,
        WAITING_TO_SWITCH_BACK
    }
    private boolean active;
    private int originalSlot = -1;
    private long nextActionTime = 0;
    private Stage stage = Stage.IDLE;
    public KeyPearl() {
        super(EncryptedString.of("Pearl Optimizer"),
                EncryptedString.of("Optimizes and throws pearl for you"),
                -1,
                Category.CRYSTAL);
        addSettings(activateKey, delay, switchBack, switchDelay, boostMultiplier, boostFirstTickOnly);
    }
    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        PearlBoostAccessor.INSTANCE.reset();
        reset();
        super.onEnable();
    }
    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        PearlBoostAccessor.INSTANCE.reset();
        if (originalSlot != -1 && originalSlot != mc.player.getInventory().getSelectedSlot()) {
            InventoryUtils.setInvSlot(originalSlot);
        }
        super.onDisable();
    }
    @Override
    public void onTick() {
        if (mc.currentScreen != null) return;
        long now = System.currentTimeMillis();
        if (KeyUtils.isKeyPressed(activateKey.getKey())) {
            if (!active) {
                active = true;
                originalSlot = mc.player.getInventory().getSelectedSlot();
                applyBoost();
                InventoryUtils.selectItemFromHotbar(Items.ENDER_PEARL);
                nextActionTime = now + (long) delay.getValue();
                stage = Stage.WAITING_TO_THROW;
            }
        }
        if (!active) return;
        switch (stage) {
            case WAITING_TO_THROW:
                if (now < nextActionTime) break;
                ActionResult result = mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                if (result.isAccepted()) {
                    mc.player.swingHand(Hand.MAIN_HAND);
                }

                if (switchBack.getValue()) {
                    nextActionTime = now + (long) switchDelay.getValue();
                    stage = Stage.WAITING_TO_SWITCH_BACK;
                } else {
                    reset();
                }
                break;

            case WAITING_TO_SWITCH_BACK:
                if (now < nextActionTime) break;
                if (originalSlot != -1) {
                    InventoryUtils.setInvSlot(originalSlot);
                }
                reset();
                break;
                case IDLE:
                break;
        }
    }
    private void applyBoost() {
        PearlBoostAccessor accessor = PearlBoostAccessor.INSTANCE;
        accessor.enabled = true;
        accessor.multiplier = boostMultiplier.getValue();
        accessor.firstTickOnly = boostFirstTickOnly.getValue();
    }
    private void reset() {
        PearlBoostAccessor.INSTANCE.reset();
        active = false;
        originalSlot = -1;
        stage = Stage.IDLE;
        nextActionTime = 0;
    }
    public static final class PearlBoostAccessor {
        public static final PearlBoostAccessor INSTANCE = new PearlBoostAccessor();
        public boolean enabled = false;
        public double multiplier = 1.0;
        public boolean firstTickOnly = true;
        private PearlBoostAccessor() {}
        public void reset() {
            enabled = false;
            multiplier = 1.0;
            firstTickOnly = true;
        }
    }
}