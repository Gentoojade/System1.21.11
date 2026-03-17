package dlindustries.vigillant.system.module.modules.mace;

import dlindustries.vigillant.system.event.events.AttackListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.WorldUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MaceSwap extends Module implements AttackListener, TickListener {
    private final BooleanSetting switchBack = new BooleanSetting(EncryptedString.of("Switch Back"), true)
            .setDescription(EncryptedString.of("Switch back to sword after attack"));

    // Now in milliseconds (0-1000ms, default 250ms ≈ 5 ticks)
    private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 250, 50, 1)
            .setDescription(EncryptedString.of("Delay after attacking before switching back, in milliseconds"));

    private final NumberSetting breachSlot = new NumberSetting(EncryptedString.of("Breach Slot"), 1, 9, 7, 1)
            .setDescription(EncryptedString.of("Slot 1-9 for where you put your breach mace"));

    private final BooleanSetting swordOnly = new BooleanSetting(EncryptedString.of("Sword Only"), false)
            .setDescription(EncryptedString.of("Only trigger when holding a sword"));

    private final BooleanSetting alwaysSwap = new BooleanSetting(EncryptedString.of("Always Swap"), false)
            .setDescription(EncryptedString.of("When activated makes you swap on each attack, looks way more legit but doesn't work with the AutoMace"));

    private final BooleanSetting autoBreach = new BooleanSetting(EncryptedString.of("Auto Mace"), false)
            .setDescription(EncryptedString.of("Automatically swaps to the mace and attacks"));

    private final NumberSetting minFallHeight = new NumberSetting(EncryptedString.of("Min Fall Height"), 0, 10, 1.5, 0.5)
            .setDescription(EncryptedString.of("Minimum fall height (in blocks) to trigger auto breach"));

    private final BooleanSetting dualMace = new BooleanSetting(EncryptedString.of("Double Mace"), false)
            .setDescription(EncryptedString.of("Enable using two maces height threshold selects a mace to use, if under the threshold Breach will be used if not Density"));

    private final NumberSetting densitySlot = new NumberSetting(EncryptedString.of("Density Slot"), 1, 9, 8, 1)
            .setDescription(EncryptedString.of("Slot 1-9 for where you put your density mace"));

    private final NumberSetting maceHeightThreshold = new NumberSetting(EncryptedString.of("Density Height Threshold"), 0, 10, 4.0, 0.5)
            .setDescription(EncryptedString.of("When dual mace is enabled, fall height below this uses breach mace, above uses density mace"));

    private final BooleanSetting respectShield = new BooleanSetting(EncryptedString.of("Shield ignore"), false)
            .setDescription(EncryptedString.of("When enabled, won't attack players holding up a shield, turn off for shield features"));

    private final BooleanSetting stunSlam = new BooleanSetting(EncryptedString.of("Stun Slam"), false)
            .setDescription(EncryptedString.of("Break shield with axe then instantly follow up with mace – requires at least 1.5 blocks of fall height"));

    private final NumberSetting minShieldHold = new NumberSetting(EncryptedString.of("Min Shield Hold"), 0, 250, 50, 1)
            .setDescription(EncryptedString.of("Minimum time opponent must hold shield before StunSlamming"));

    private final NumberSetting stunMaceDelayMs = new NumberSetting(EncryptedString.of("Mace Hit Delay"), 0, 50, 30, 1)
            .setDescription(EncryptedString.of("Delay between axe hit and mace hit in milliseconds"));

    private boolean shouldSwitchBack;
    private int originalSlot = -1;
    private long switchTimerStart = 0;          // timestamp when switch-back timer started
    private boolean autoSwapped;
    private boolean sessionActive;
    private final Map<UUID, Long> shieldStartTimes = new HashMap<>();
    private PlayerEntity currentTarget;

    private boolean stunSlamActive;
    private int stunSlamPhase;                  // 0 = idle, 1 = wait for axe hit, 2 = wait for mace delay
    private long stunSlamTimerStart;             // timestamp for delay
    private PlayerEntity stunSlamTarget;
    private int stunSlamAxeSlot;
    private static final float STUN_MIN_FALL = 1.5f;

    public MaceSwap() {
        super(EncryptedString.of("Mace Swap"),
                EncryptedString.of("Swaps to selected breach mace slot when attacking with sword or does everything for you"),
                -1,
                Category.mace);
        addSettings(
                switchBack, switchDelay, breachSlot, swordOnly, alwaysSwap,
                autoBreach, minFallHeight,
                dualMace, densitySlot, maceHeightThreshold,
                respectShield,
                stunSlam, minShieldHold,
                stunMaceDelayMs
        );
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
        } else if (autoSwapped && originalSlot != -1) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
        }
        shieldStartTimes.clear();
        super.onDisable();
    }

    private boolean isMaceInSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= 9) return false;
        ItemStack stack = mc.player.getInventory().getStack(slotIndex);
        return stack.getItem() == Items.MACE;
    }

    private int findAxeSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    private int getTargetMaceSlot() {
        int targetSlot;
        if (dualMace.getValue()) {
            if (mc.player.fallDistance < maceHeightThreshold.getValue()) {
                targetSlot = breachSlot.getValueInt() - 1;
            } else {
                targetSlot = densitySlot.getValueInt() - 1;
            }
        } else {
            targetSlot = breachSlot.getValueInt() - 1;
        }
        return isMaceInSlot(targetSlot) ? targetSlot : -1;
    }

    private boolean isValidTarget(Entity entity) {
        return entity instanceof LivingEntity && ((LivingEntity) entity).isAlive();
    }

    private boolean isTargetBlocking(Entity entity) {
        return entity instanceof LivingEntity && ((LivingEntity) entity).isBlocking();
    }

    private void performAttack(Entity target) {
        mc.interactionManager.attackEntity(mc.player, target);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void updateShieldTracking(PlayerEntity target) {
        currentTarget = target;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            UUID uuid = player.getUuid();
            boolean isBlocking = player.isHolding(Items.SHIELD) && player.isBlocking();
            if (isBlocking) {
                if (!shieldStartTimes.containsKey(uuid)) {
                    shieldStartTimes.put(uuid, System.currentTimeMillis());
                }
            } else {
                shieldStartTimes.remove(uuid);
            }
        }
    }

    private boolean shouldBreakShield(PlayerEntity target) {
        if (target == null) return false;
        UUID uuid = target.getUuid();
        if (!shieldStartTimes.containsKey(uuid)) return false;
        long holdTime = System.currentTimeMillis() - shieldStartTimes.get(uuid);
        return holdTime >= minShieldHold.getValue();
    }

    private void startStunSlam(PlayerEntity target) {
        if (sessionActive) {
            if (shouldSwitchBack && originalSlot != -1) {
                mc.player.getInventory().setSelectedSlot(originalSlot);
            }
            sessionActive = false;
            autoSwapped = false;
        }

        int currentSlot = mc.player.getInventory().getSelectedSlot();
        if (switchBack.getValue()) {
            originalSlot = currentSlot;
        }

        int axeSlot = findAxeSlot();
        if (axeSlot == -1) {
            return;
        }
        mc.player.getInventory().setSelectedSlot(axeSlot);

        // Phase 1: waiting to perform axe hit (same tick)
        stunSlamActive = true;
        stunSlamPhase = 1;
        stunSlamTarget = target;
        stunSlamAxeSlot = axeSlot;
    }

    private void continueStunSlam() {
        if (mc.player == null || stunSlamTarget == null || !stunSlamTarget.isAlive()) {
            abortStunSlam();
            return;
        }

        if (stunSlamPhase == 1) {
            // Perform axe hit immediately
            if (!(mc.crosshairTarget instanceof EntityHitResult entityHit) || entityHit.getEntity() != stunSlamTarget) {
                abortStunSlam();
                return;
            }
            if (mc.player.getInventory().getSelectedSlot() != stunSlamAxeSlot) {
                abortStunSlam();
                return;
            }

            performAttack(stunSlamTarget);
            shieldStartTimes.remove(stunSlamTarget.getUuid());

            // If no delay needed, go straight to mace phase
            if (stunMaceDelayMs.getValueInt() <= 0) {
                stunSlamPhase = 3; // skip delay
            } else {
                stunSlamPhase = 2;
                stunSlamTimerStart = System.currentTimeMillis();
            }
        }
        else if (stunSlamPhase == 2) {
            // Wait for delay before mace
            long elapsed = System.currentTimeMillis() - stunSlamTimerStart;
            if (elapsed >= stunMaceDelayMs.getValueInt()) {
                stunSlamPhase = 3;
            } else {
                return; // still waiting
            }
        }

        if (stunSlamPhase == 3) {
            // Swap to mace and attack
            int maceSlot = getTargetMaceSlot();
            if (maceSlot == -1) {
                abortStunSlam();
                return;
            }
            mc.player.getInventory().setSelectedSlot(maceSlot);

            if (!stunSlamTarget.isAlive() || !(mc.crosshairTarget instanceof EntityHitResult entityHit2) || entityHit2.getEntity() != stunSlamTarget) {
                abortStunSlam();
                return;
            }

            performAttack(stunSlamTarget);

            if (switchBack.getValue()) {
                shouldSwitchBack = true;
                switchTimerStart = System.currentTimeMillis();
            } else {
                originalSlot = -1;
            }

            stunSlamActive = false;
            stunSlamPhase = 0;
        }
    }

    private void abortStunSlam() {
        if (switchBack.getValue() && originalSlot != -1) {
            mc.player.getInventory().setSelectedSlot(originalSlot);
            originalSlot = -1;
        }
        stunSlamActive = false;
        stunSlamPhase = 0;
        stunSlamTarget = null;
    }

    @Override
    public void onAttack(AttackEvent event) {
        if (!alwaysSwap.getValue()) {
            if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.ENTITY) return;
            Entity target = ((EntityHitResult) mc.crosshairTarget).getEntity();
            if (!isValidTarget(target)) return;

            boolean shieldEffective = false;
            if (target instanceof PlayerEntity && isTargetBlocking(target)) {
                shieldEffective = !WorldUtils.isShieldFacingAway((PlayerEntity) target);
            }

            if (respectShield.getValue() && shieldEffective) return;

            if (stunSlam.getValue() && shieldEffective && !respectShield.getValue() &&
                    target instanceof PlayerEntity) {
                if (mc.player.fallDistance >= STUN_MIN_FALL) {
                    startStunSlam((PlayerEntity) target);
                    event.cancel();
                    return;
                } else {
                    return;
                }
            }
        }

        if (autoBreach.getValue()) {
            boolean isFalling = !mc.player.isOnGround() && mc.player.getVelocity().y < 0;
            boolean isGliding = mc.player.isGliding();
            boolean enoughFallHeight = mc.player.fallDistance >= minFallHeight.getValue();
            if (!isFalling || isGliding || !enoughFallHeight) {
                return;
            }
        }

        ItemStack currentStack = mc.player.getMainHandStack();
        boolean holdingSword = currentStack.isIn(ItemTags.SWORDS);
        if (swordOnly.getValue() && !holdingSword && !autoSwapped) return;

        if (shouldSwitchBack) {
            // If we are waiting to switch back, just reset timer
            switchTimerStart = System.currentTimeMillis();
            return;
        }

        int targetSlot = getTargetMaceSlot();
        if (targetSlot == -1) return;

        if (switchBack.getValue() && originalSlot == -1) {
            originalSlot = mc.player.getInventory().getSelectedSlot();
        }

        if (mc.player.getInventory().getSelectedSlot() != targetSlot) {
            mc.player.getInventory().setSelectedSlot(targetSlot);
        }

        if (switchBack.getValue()) {
            shouldSwitchBack = true;
            switchTimerStart = System.currentTimeMillis();
        }
    }

    @Override
    public void onTick() {
        // Handle stun slam progression
        if (stunSlamActive) {
            continueStunSlam();
            if (stunSlamActive) return; // still in progress, skip rest
        }

        // Flight monitoring
        if (sessionActive && mc.player.isGliding()) {
            if (shouldSwitchBack && originalSlot != -1) {
                mc.player.getInventory().setSelectedSlot(originalSlot);
            }
            resetState();
        }

        // Auto‑swapped cleanup
        if (!autoBreach.getValue() && autoSwapped && !shouldSwitchBack) {
            if (originalSlot != -1) {
                mc.player.getInventory().setSelectedSlot(originalSlot);
            }
            resetState();
        }

        // Shield tracking (if stun slam enabled)
        boolean handledBlockingPlayer = false;
        if (stunSlam.getValue() && autoBreach.getValue()) {
            PlayerEntity targetPlayer = null;
            if (mc.crosshairTarget instanceof EntityHitResult entityHit &&
                    entityHit.getEntity() instanceof PlayerEntity player &&
                    player != mc.player && player.isAlive()) {
                targetPlayer = player;
            }
            updateShieldTracking(targetPlayer);

            if (targetPlayer != null && shouldBreakShield(targetPlayer) && !respectShield.getValue()) {
                boolean shieldEffective = !WorldUtils.isShieldFacingAway(targetPlayer);
                if (shieldEffective) {
                    if (mc.player.fallDistance >= STUN_MIN_FALL) {
                        startStunSlam(targetPlayer);
                        return;
                    } else {
                        handledBlockingPlayer = true;
                    }
                }
            }
        }

        // Normal auto mace
        if (autoBreach.getValue() && !handledBlockingPlayer) {
            if (mc.currentScreen != null) return;

            ItemStack currentStack = mc.player.getMainHandStack();
            boolean holdingSword = currentStack.isIn(ItemTags.SWORDS);
            if (swordOnly.getValue() && !holdingSword) {
                if (autoSwapped && originalSlot != -1) {
                    mc.player.getInventory().setSelectedSlot(originalSlot);
                    resetState();
                }
                return;
            }

            boolean isFalling = !mc.player.isOnGround() && mc.player.getVelocity().y < 0;
            boolean isGliding = mc.player.isGliding();
            boolean enoughFallHeight = mc.player.fallDistance >= minFallHeight.getValue();

            if (!isFalling || isGliding || !enoughFallHeight) {
                if (sessionActive) {
                    if (shouldSwitchBack) {
                        if (originalSlot != -1) mc.player.getInventory().setSelectedSlot(originalSlot);
                        resetState();
                    } else {
                        resetState();
                    }
                }
                return;
            }

            boolean aimingAtValid = false;
            Entity target = null;
            if (mc.crosshairTarget instanceof EntityHitResult entityHit) {
                Entity hitEntity = entityHit.getEntity();
                if (isValidTarget(hitEntity)) {
                    aimingAtValid = true;
                    target = hitEntity;
                }
            }

            if (!aimingAtValid) {
                if (sessionActive) {
                    if (shouldSwitchBack) {
                        if (originalSlot != -1) mc.player.getInventory().setSelectedSlot(originalSlot);
                        resetState();
                    } else {
                        resetState();
                    }
                }
                return;
            }

            boolean shieldEffective = false;
            if (target instanceof PlayerEntity && isTargetBlocking(target)) {
                shieldEffective = !WorldUtils.isShieldFacingAway((PlayerEntity) target);
            }

            if (respectShield.getValue() && shieldEffective) {
                if (sessionActive) {
                    if (shouldSwitchBack && originalSlot != -1) mc.player.getInventory().setSelectedSlot(originalSlot);
                    resetState();
                }
                return;
            }

            int targetSlot = getTargetMaceSlot();
            if (targetSlot == -1) {
                if (sessionActive) {
                    if (shouldSwitchBack && originalSlot != -1) mc.player.getInventory().setSelectedSlot(originalSlot);
                    resetState();
                }
                return;
            }
            int currentSlot = mc.player.getInventory().getSelectedSlot();
            if (!sessionActive) {
                if (originalSlot == -1) originalSlot = currentSlot;
                if (currentSlot != targetSlot) {
                    mc.player.getInventory().setSelectedSlot(targetSlot);
                    autoSwapped = true;
                } else {
                    autoSwapped = true;
                }
                performAttack(target);
                if (switchBack.getValue()) {
                    shouldSwitchBack = true;
                    switchTimerStart = System.currentTimeMillis();
                }
                sessionActive = true;
            } else {
                if (autoSwapped && currentSlot != targetSlot) {
                    sessionActive = false;
                    autoSwapped = false;
                    originalSlot = -1;
                }
            }
        }

        // Switch‑back timer (milliseconds)
        if (shouldSwitchBack && originalSlot != -1) {
            long elapsed = System.currentTimeMillis() - switchTimerStart;
            if (elapsed < switchDelay.getValueInt()) {
                return;
            }
            mc.player.getInventory().setSelectedSlot(originalSlot);
            shouldSwitchBack = false;
            autoSwapped = false;
            originalSlot = -1;
            switchTimerStart = 0;
        }
    }

    private void resetState() {
        shouldSwitchBack = false;
        originalSlot = -1;
        switchTimerStart = 0;
        autoSwapped = false;
        sessionActive = false;
        stunSlamActive = false;
        stunSlamPhase = 0;
        stunSlamTarget = null;
    }
}