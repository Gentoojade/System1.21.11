package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.ItemUseListener;
import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.*;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AutoCrystal extends Module implements TickListener, ItemUseListener {
	private final KeybindSetting activateKey = new KeybindSetting(EncryptedString.of("Activate Key"), GLFW.GLFW_MOUSE_BUTTON_RIGHT, false)
			.setDescription(EncryptedString.of("Key that does the crystalling"));
	private final NumberSetting placeDelay   = new NumberSetting(EncryptedString.of("Place Delay"), 0, 20, 2, 1);
	private final NumberSetting breakDelay   = new NumberSetting(EncryptedString.of("Break Delay"), 0, 20, 2, 1);

	private final NumberSetting placeChance  = new NumberSetting(EncryptedString.of("Place Chance"), 0, 100, 100, 1)
			.setDescription(EncryptedString.of("Randomization"));
	private final NumberSetting breakChance  = new NumberSetting(EncryptedString.of("Break Chance"), 0, 100, 100, 1)
			.setDescription(EncryptedString.of("Randomization"));
	private final BooleanSetting LootProtect     = new BooleanSetting(EncryptedString.of("Loot protect"), false)
			.setDescription(EncryptedString.of("Won't crystal if a dead player is nearby"));
	private final BooleanSetting fakePunch       = new BooleanSetting(EncryptedString.of("Fake Punch"), true)
			.setDescription(EncryptedString.of("Will hit every entity and block if you miss a hitcrystal"));
	private final BooleanSetting clickSimulation = new BooleanSetting(EncryptedString.of("Click Simulation"), false)
			.setDescription(EncryptedString.of("Makes the CPS hud think you're legit"));
	private final BooleanSetting damageTick      = new BooleanSetting(EncryptedString.of("Damage Tick"), true)
			.setDescription(EncryptedString.of("Times your crystals for a perfect d-tap"));
	private final BooleanSetting antiWeakness    = new BooleanSetting(EncryptedString.of("Anti-Weakness"), false)
			.setDescription(EncryptedString.of("Silently switches to a sword and then hits the crystal if you have weakness"));
	private final NumberSetting particleChance   = new NumberSetting(EncryptedString.of("Particle Chance"), 0, 100, 35, 1)
			.setDescription(EncryptedString.of("Adds block breaking particles to make it seem more legit from your POV (Only works with fake punch)"));
	private final BooleanSetting noAir           = new BooleanSetting(EncryptedString.of("No Air"), true)
			.setDescription(EncryptedString.of("Doesn't crystal if in air"));

	// ── Visual (fixed values, not shown in GUI) ───────────────────────────────
	private final BooleanSetting visual = new BooleanSetting(EncryptedString.of("Visual"), true)
			.setDescription(EncryptedString.of("Renders a box on the target obsidian block"));

	private static final float FILL_ALPHA    = 85f;
	private static final float OUTLINE_ALPHA = 255f;
	private static final float FADE_SPEED    = 1f;
	private static final float LINGER_TIME   = 2f; // seconds
	private static class BlockVisual {
		BlockPos pos;
		float alpha;
		boolean active;
		int lingerTicksLeft;
		boolean lingering;
		BlockVisual(BlockPos pos, float initialAlpha) {
			this.pos             = pos;
			this.alpha           = initialAlpha;
			this.active          = true;
			this.lingerTicksLeft = 0;
			this.lingering       = false;
		}
	}
	private int placeClock;
	private int breakClock;
	public boolean crystalling;
	private volatile BlockPos currentTarget = null;
	private final List<BlockVisual> visuals  = new ArrayList<>();
	private boolean worldRenderHookRegistered = false;
	public AutoCrystal() {
		super(EncryptedString.of("Auto Crystal"),
				EncryptedString.of("Automatically crystals fast for you"),
				-1,
				Category.CRYSTAL);
		addSettings(activateKey, placeDelay, breakDelay, placeChance, breakChance,
				LootProtect, fakePunch, clickSimulation, noAir, damageTick, antiWeakness,
				particleChance, visual);
		registerWorldRenderHook();
	}
	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		eventManager.add(ItemUseListener.class, this);
		placeClock    = 0;
		breakClock    = 0;
		crystalling   = false;
		currentTarget = null;
		visuals.clear();
		super.onEnable();
	}
	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		eventManager.remove(ItemUseListener.class, this);
		currentTarget = null;
		visuals.clear();
		super.onDisable();
	}
	@Override
	public void onTick() {
		if (mc.currentScreen != null) return;
		for (BlockVisual bv : visuals) {
			if (bv.lingerTicksLeft > 0) {
				bv.lingerTicksLeft--;
				bv.lingering = bv.lingerTicksLeft > 0;
			}
		}
		boolean dontPlace = (placeClock != 0);
		boolean dontBreak = (breakClock != 0);
		if (LootProtect.getValue() &&
				(WorldUtils.isDeadBodyNearby() || WorldUtils.isValuableLootNearby())) {
			setCurrentTarget(null);
			return;
		}
		if (noAir.getValue() && !mc.player.isOnGround()) {
			setCurrentTarget(null);
			return;
		}
		int randomInt = MathUtils.randomInt(1, 100);
		if (dontPlace) placeClock--;
		if (dontBreak) breakClock--;
		if (mc.player.isUsingItem()) return;
		if (damageTick.getValue() && damageTickCheck()) return;
		if (activateKey.getKey() != -1 && !KeyUtils.isKeyPressed(activateKey.getKey())) {
			placeClock  = 0;
			breakClock  = 0;
			crystalling = false;
			setCurrentTarget(null);
			return;
		} else crystalling = true;
		if (mc.player.getMainHandStack().getItem() != Items.END_CRYSTAL) {
			setCurrentTarget(null);
			return;
		}
		if (mc.crosshairTarget instanceof BlockHitResult hit) {
			if (mc.crosshairTarget.getType() == HitResult.Type.BLOCK) {
				boolean validBase = BlockUtils.isBlock(hit.getBlockPos(), Blocks.OBSIDIAN)
						|| BlockUtils.isBlock(hit.getBlockPos(), Blocks.BEDROCK);
				boolean canPlace  = CrystalUtils.canPlaceCrystalClientAssumeObsidian(hit.getBlockPos());
				setCurrentTarget(validBase && canPlace ? hit.getBlockPos() : null);
				if (!dontPlace && randomInt <= placeChance.getValueInt()) {
					if (validBase && canPlace) {
						if (clickSimulation.getValue())
							MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
						WorldUtils.placeBlock(hit, true);
						if (fakePunch.getValue()) {
							if (randomInt <= particleChance.getValue())
								if (canPlace && hit.getSide() == Direction.UP)
									mc.player.swingHand(Hand.MAIN_HAND);
						}
						placeClock = placeDelay.getValueInt();
					}
				}
				if (fakePunch.getValue()) {
					if (!dontBreak && randomInt <= breakChance.getValueInt()) {
						if (validBase) return;
						if (clickSimulation.getValue()) {
							if (validBase) {
								if (canPlace)
									MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
							} else MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
						}
						mc.interactionManager.attackBlock(hit.getBlockPos(), hit.getSide());
						mc.player.swingHand(Hand.MAIN_HAND);
						mc.interactionManager.updateBlockBreakingProgress(hit.getBlockPos(), hit.getSide());
						breakClock = breakDelay.getValueInt();
					}
					if (!dontPlace && randomInt <= placeChance.getValueInt() && dontBreak) {
						if (clickSimulation.getValue())
							MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
					}
				}
			}
			if (mc.crosshairTarget.getType() == HitResult.Type.MISS) {
				setCurrentTarget(null);
				if (fakePunch.getValue()) {
					if (!dontBreak && randomInt <= breakChance.getValueInt()) {
						if (mc.interactionManager.hasLimitedAttackSpeed())
							mc.attackCooldown = 10;
						if (clickSimulation.getValue())
							MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
						mc.player.swingHand(Hand.MAIN_HAND);
						breakClock = breakDelay.getValueInt();
					}
					if (!dontPlace && randomInt <= placeChance.getValueInt() && dontBreak) {
						if (clickSimulation.getValue())
							MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
					}
				}
			}
		} else {
			setCurrentTarget(null);
		}
		randomInt = MathUtils.randomInt(1, 100);
		if (mc.crosshairTarget instanceof EntityHitResult hit) {
			if (!dontBreak && randomInt <= breakChance.getValueInt()) {
				Entity entity = hit.getEntity();
				if (!fakePunch.getValue() && !(entity instanceof EndCrystalEntity || entity instanceof SlimeEntity))
					return;
				int previousSlot = mc.player.getInventory().getSelectedSlot();
				if (entity instanceof EndCrystalEntity || entity instanceof SlimeEntity)
					if (antiWeakness.getValue() && cantBreakCrystal())
						InventoryUtils.selectSword();
				if (clickSimulation.getValue())
					MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);
				WorldUtils.hitEntity(entity, true);
				breakClock = breakDelay.getValueInt();
				if (antiWeakness.getValue())
					InventoryUtils.setInvSlot(previousSlot);
			}
		}
	}
	private void registerWorldRenderHook() {
		if (worldRenderHookRegistered) return;
		worldRenderHookRegistered = true;
		WorldRenderEvents.BEFORE_DEBUG_RENDER.register(this::onWorldRender);
	}
	private void onWorldRender(WorldRenderContext context) {
		if (!isEnabled() || !visual.getValue() || mc.world == null || mc.player == null) {
			visuals.clear();
			return;
		}
		WorldRenderer worldRenderer = context.worldRenderer();
		GameRenderer  gameRenderer  = context.gameRenderer();
		if (worldRenderer == null || gameRenderer == null) return;
		Iterator<BlockVisual> it = visuals.iterator();
		while (it.hasNext()) {
			BlockVisual bv = it.next();
			if (bv.active) {
				bv.alpha = Math.min(bv.alpha + FADE_SPEED, FILL_ALPHA);
			} else if (bv.lingering) {
				bv.alpha = FILL_ALPHA;
			} else {
				bv.alpha = Math.max(bv.alpha - (FADE_SPEED * 3f), 0f);
				if (bv.alpha <= 0f) {
					it.remove();
					continue;
				}
			}
			float alphaFraction = bv.alpha / 255f;
			Color themeColor    = Utils.getMainColor(255, 0);
			Color brighter      = themeColor.brighter();
			int fillRGBA    = withAlpha(themeColor,
					Math.max(0, Math.min(255, (int)(FILL_ALPHA * alphaFraction)))).getRGB();int outlineRGBA = withAlpha(brighter,
					Math.max(0, Math.min(255, (int)(OUTLINE_ALPHA * alphaFraction)))).getRGB();
					DrawStyle drawStyle = DrawStyle.filledAndStroked(outlineRGBA, 1.5f, fillRGBA);
					try (var ignored = worldRenderer.startDrawingGizmos()) {
				GizmoDrawing.box(new Box(bv.pos), drawStyle).ignoreOcclusion();
			}
		}
	}
	private void setCurrentTarget(BlockPos pos) {
		BlockPos prev = currentTarget;

		if (pos != null && (prev == null || !pos.equals(prev))) {
			BlockVisual existing = findVisual(pos);
			if (existing != null) {
				existing.active          = true;
				existing.lingerTicksLeft = 0;
				existing.lingering       = false;
			} else {
				visuals.add(new BlockVisual(pos, 0f));
			}
		}
		if (prev != null && !prev.equals(pos)) {
			BlockVisual prevVisual = findVisual(prev);
			if (prevVisual != null) {
				prevVisual.active          = false;
				prevVisual.lingerTicksLeft = (int)(LINGER_TIME * 20.0);
				prevVisual.lingering       = prevVisual.lingerTicksLeft > 0;
			}
		}
		if (pos == null && prev != null) {
			for (BlockVisual bv : visuals) {
				if (bv.active) {
					bv.active          = false;
					bv.lingerTicksLeft = (int)(LINGER_TIME * 20.0);
					bv.lingering       = bv.lingerTicksLeft > 0;
				}
			}
		}
		currentTarget = pos;
	}
	private BlockVisual findVisual(BlockPos pos) {
		for (BlockVisual bv : visuals) {
			if (bv.pos.equals(pos)) return bv;
		}
		return null;
	}
	private static Color withAlpha(Color base, int alpha) {
		return new Color(base.getRed(), base.getGreen(), base.getBlue(),
				Math.max(0, Math.min(255, alpha)));
	}
	@Override
	public void onItemUse(ItemUseEvent event) {
		if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
			if ((mc.crosshairTarget instanceof BlockHitResult h
					&& mc.crosshairTarget.getType() == HitResult.Type.BLOCK
					&& (BlockUtils.isBlock(h.getBlockPos(), Blocks.OBSIDIAN)
					|| BlockUtils.isBlock(h.getBlockPos(), Blocks.BEDROCK)))) {
				event.cancel();
			}
		}
	}
	private boolean cantBreakCrystal() {
		assert mc.player != null;
		StatusEffectInstance weakness = mc.player.getStatusEffect(StatusEffects.WEAKNESS);
		StatusEffectInstance strength = mc.player.getStatusEffect(StatusEffects.STRENGTH);
		return (!(weakness == null || strength != null
				&& strength.getAmplifier() > weakness.getAmplifier()
				|| WorldUtils.isTool(mc.player.getMainHandStack())));
	}
	private boolean damageTickCheck() {
		return mc.world.getPlayers().parallelStream()
				.filter(e -> e != mc.player)
				.filter(e -> e.squaredDistanceTo(mc.player) < 36)
				.filter(e -> e.getLastAttacker() == null)
				.filter(e -> !e.isOnGround())
				.anyMatch(e -> e.hurtTime >= 2)
				&& !(mc.player.getAttacking() instanceof PlayerEntity);
	}
}