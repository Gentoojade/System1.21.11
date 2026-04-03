package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.ItemUseListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.*;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import java.util.HashSet;
import java.util.Set;

public final class AirAnchor extends Module implements TickListener, ItemUseListener {
    private final BooleanSetting LootProtect    = new BooleanSetting(EncryptedString.of("Loot Protect"), false)
            .setDescription(EncryptedString.of("Doesn't anchor if body nearby"));
    private final KeybindSetting airplaceKey    = new KeybindSetting(EncryptedString.of("Airplace Key"), -1, false)
            .setDescription(EncryptedString.of("Hold to charge and explode anchors"));
    private final NumberSetting  switchDelay    = new NumberSetting(EncryptedString.of("Switch Delay"),    0, 20,  1, 1);
    private final NumberSetting  glowstoneDelay = new NumberSetting(EncryptedString.of("Glowstone Delay"), 0, 20,  0, 1);
    private final NumberSetting  explodeDelay   = new NumberSetting(EncryptedString.of("Explode Delay"),   0, 20,  1, 1);
    private final NumberSetting  explodeSlot    = new NumberSetting(EncryptedString.of("Explode Slot"),    1, 9,   9, 1);
    private final NumberSetting  airplaceDelay  = new NumberSetting(EncryptedString.of("Airplace Delay"),  0, 500, 25, 1)
            .setDescription(EncryptedString.of("Milliseconds to wait after explosion before placing the second anchor"));
    private int switchClock    = 0;
    private int glowstoneClock = 0;
    private int explodeClock   = 0;
    private final Set<BlockPos> ownedAnchors = new HashSet<>();
    private BlockPos optimizerPos   = null;
    private int      optimizerCount = 0;
    private enum AirplaceState { IDLE, AWAIT_PLACE, AWAIT_CHARGE, AWAIT_EXPLODE }
    private AirplaceState  airplaceState    = AirplaceState.IDLE;
    private BlockHitResult airplaceHit      = null;
    private int            airplaceClock    = 0;
    private int            airplaceTriggers = 0;
    private boolean        wasKeyHeld       = false;
    private long           placeAt          = 0L;
    public AirAnchor() {
        super(EncryptedString.of("Double Anchor"),
                EncryptedString.of("Automatically places an anchor and air-places the second one, might not work on some servers"),
                -1,
                Category.CRYSTAL);
        addSettings(LootProtect, airplaceKey,
                switchDelay,
                glowstoneDelay,
                explodeDelay, explodeSlot,
                airplaceDelay);
    }
    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        eventManager.add(ItemUseListener.class, this);
        switchClock      = 0;
        glowstoneClock   = 0;
        explodeClock     = 0;
        optimizerPos     = null;
        optimizerCount   = 0;
        airplaceState    = AirplaceState.IDLE;
        airplaceHit      = null;
        airplaceClock    = 0;
        airplaceTriggers = 0;
        wasKeyHeld       = false;
        placeAt          = 0L;
        super.onEnable();
    }
    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        eventManager.remove(ItemUseListener.class, this);
        super.onDisable();
    }
    @Override
    public void onTick() {
        if (mc.currentScreen != null) return;
        boolean keyHeld        = airplaceKey.getKey() != -1
                && KeyUtils.isKeyPressed(airplaceKey.getKey());
        boolean keyJustPressed = keyHeld && !wasKeyHeld;
        if (!keyHeld && wasKeyHeld) {
            airplaceTriggers = 0;
        }
        wasKeyHeld = keyHeld;
        if (airplaceState != AirplaceState.IDLE) {
            tickAirplaceSequence();
            return;
        }
        if (keyJustPressed) {
            if (LootProtect.getValue()
                    && (WorldUtils.isDeadBodyNearby() || WorldUtils.isValuableLootNearby())) {
                return;
            }
            if (mc.crosshairTarget instanceof BlockHitResult hit
                    && hit.getType() == HitResult.Type.BLOCK) {
                InventoryUtils.selectItemFromHotbar(Items.RESPAWN_ANCHOR);
                if (mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) {
                    WorldUtils.placeBlock(hit, false);
                }
            }
            return;
        }
        if (mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)
                && mc.crosshairTarget instanceof BlockHitResult optHit
                && BlockUtils.isAnchorCharged(optHit.getBlockPos())
                && keyHeld) {

            if (!optHit.getBlockPos().equals(optimizerPos)) {
                optimizerPos   = optHit.getBlockPos();
                optimizerCount = 0;
            }
            if (optimizerCount < 1) {
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, optHit);
                optimizerCount++;
            }
        }
        if (!keyHeld) return;
        if (mc.crosshairTarget instanceof BlockHitResult hit) {
            if (BlockUtils.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
                mc.options.useKey.setPressed(false);
                if (BlockUtils.isAnchorNotCharged(hit.getBlockPos())) {
                    if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                        if (switchClock != switchDelay.getValueInt()) {
                            switchClock++;
                            return;
                        }
                        switchClock = 0;
                        InventoryUtils.selectItemFromHotbar(Items.GLOWSTONE);
                    }
                    if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                        if (glowstoneClock != glowstoneDelay.getValueInt()) {
                            glowstoneClock++;
                            return;
                        }
                        glowstoneClock = 0;
                        WorldUtils.placeBlock(hit, true);
                    }
                }
                if (BlockUtils.isAnchorCharged(hit.getBlockPos())) {
                    int slot = explodeSlot.getValueInt() - 1;
                    if (mc.player.getInventory().getSelectedSlot() != slot) {
                        if (switchClock != switchDelay.getValueInt()) {
                            switchClock++;
                            return;
                        }
                        switchClock = 0;
                        mc.player.getInventory().setSelectedSlot(slot);
                    }
                    if (mc.player.getInventory().getSelectedSlot() == slot) {
                        if (explodeClock != explodeDelay.getValueInt()) {
                            explodeClock++;
                            return;
                        }
                        explodeClock = 0;
                        WorldUtils.placeBlock(hit, false);
                        ownedAnchors.remove(hit.getBlockPos());
                        mc.player.getInventory().setSelectedSlot(explodeSlot.getValueInt() - 1);

                        if (airplaceTriggers < 1) {
                            airplaceHit      = hit;
                            airplaceState    = AirplaceState.AWAIT_PLACE;
                            airplaceClock    = 0;
                            airplaceTriggers++;
                            placeAt          = System.currentTimeMillis() + airplaceDelay.getValueInt();
                        }
                    }
                }
            }
        }
    }
    private void tickAirplaceSequence() {
        mc.options.useKey.setPressed(false);
        if (airplaceHit == null) {
            resetAirplaceSequence();
            return;
        }
        if (mc.player.squaredDistanceTo(airplaceHit.getPos()) > 36) {
            resetAirplaceSequence();
            return;
        }
        switch (airplaceState) {
            case AWAIT_PLACE -> {
                if (System.currentTimeMillis() < placeAt) return;
                if (!mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) {
                    InventoryUtils.selectItemFromHotbar(Items.RESPAWN_ANCHOR);
                }
                if (!mc.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) {
                    resetAirplaceSequence();
                    return;
                }
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, airplaceHit);
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.player.getInventory().setSelectedSlot(explodeSlot.getValueInt() - 1);
                airplaceState = AirplaceState.AWAIT_CHARGE;
                airplaceClock = 0;
            }
            case AWAIT_CHARGE -> {
                if (airplaceClock < glowstoneDelay.getValueInt()) {
                    airplaceClock++;
                    return;
                }
                if (!(mc.crosshairTarget instanceof BlockHitResult ch)
                        || !ch.getBlockPos().equals(airplaceHit.getBlockPos())) {
                    resetAirplaceSequence();
                    return;
                }
                InventoryUtils.selectItemFromHotbar(Items.GLOWSTONE);
                if (!mc.player.getMainHandStack().isOf(Items.GLOWSTONE)) {
                    resetAirplaceSequence();
                    return;
                }
                WorldUtils.placeBlock(airplaceHit, true);
                airplaceState = AirplaceState.AWAIT_EXPLODE;
                airplaceClock = 0;
            }
            case AWAIT_EXPLODE -> {
                if (airplaceClock < explodeDelay.getValueInt()) {
                    airplaceClock++;
                    return;
                }
                if (!(mc.crosshairTarget instanceof BlockHitResult ch)
                        || !ch.getBlockPos().equals(airplaceHit.getBlockPos())) {
                    resetAirplaceSequence();
                    return;
                }
                int slot = explodeSlot.getValueInt() - 1;
                mc.player.getInventory().setSelectedSlot(slot);
                WorldUtils.placeBlock(airplaceHit, true);
                resetAirplaceSequence();
            }
            default -> resetAirplaceSequence();
        }
    }
    private void resetAirplaceSequence() {
        airplaceState = AirplaceState.IDLE;
        airplaceHit   = null;
        airplaceClock = 0;
        placeAt       = 0L;
    }
    @Override
    public void onItemUse(ItemUseEvent event) {
        if (mc.crosshairTarget instanceof BlockHitResult hitResult
                && hitResult.getType() == HitResult.Type.BLOCK) {

            if (mc.player.getMainHandStack().getItem() == Items.RESPAWN_ANCHOR) {
                Direction dir = hitResult.getSide();
                BlockPos pos  = hitResult.getBlockPos();
                if (!mc.world.getBlockState(pos).isReplaceable()) {
                    switch (dir) {
                        case UP    -> ownedAnchors.add(pos.add(0,  1,  0));
                        case DOWN  -> ownedAnchors.add(pos.add(0, -1,  0));
                        case EAST  -> ownedAnchors.add(pos.add(1,  0,  0));
                        case WEST  -> ownedAnchors.add(pos.add(-1, 0,  0));
                        case NORTH -> ownedAnchors.add(pos.add(0,  0, -1));
                        case SOUTH -> ownedAnchors.add(pos.add(0,  0,  1));
                    }
                } else {
                    ownedAnchors.add(pos);
                }
            }
            BlockPos bp = hitResult.getBlockPos();
            if (BlockUtils.isAnchorCharged(bp))
                ownedAnchors.remove(bp);
        }
    }
}