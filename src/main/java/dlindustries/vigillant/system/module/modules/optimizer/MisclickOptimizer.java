package dlindustries.vigillant.system.module.modules.optimizer;

import dlindustries.vigillant.system.event.events.AttackListener;
import dlindustries.vigillant.system.event.events.BlockBreakingListener;
import dlindustries.vigillant.system.event.events.ItemUseListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.utils.BlockUtils;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.BlockHitResult;

public final class MisclickOptimizer extends Module implements ItemUseListener, AttackListener, BlockBreakingListener {
	private final BooleanSetting doubleGlowstone = new BooleanSetting(EncryptedString.of("Double Glowstone"), false)
			.setDescription(EncryptedString.of("Makes it so you can't charge the anchor again if it's already charged (don't use for safe anchor)"));
	private final BooleanSetting antiCharge = new BooleanSetting(EncryptedString.of("Anti Glowstone Charge"), false)
			.setDescription(EncryptedString.of("Prevents all charging of glowstone anchors for more accurate anchor macro"));
	private final BooleanSetting glowstoneMisplace = new BooleanSetting(EncryptedString.of("Glowstone Misclick"), false)
			.setDescription(EncryptedString.of("Makes it so you cant place glowstone if not to charge an anchor"));
	private final BooleanSetting anchorOnAnchor = new BooleanSetting(EncryptedString.of("Anchor on anchor"), false)
			.setDescription(EncryptedString.of("Makes it so you can't place an anchor on to another anchor unless charged"));
	private final BooleanSetting obiPunch = new BooleanSetting(EncryptedString.of("Auto-crystal bypass"), true)
			.setDescription(EncryptedString.of("Removes flags from Grim and Vulkan up to 85%"));
	private final BooleanSetting glowstoneOnGlowstone = new BooleanSetting(EncryptedString.of("Glowstone on Glowstone"), false)
			.setDescription(EncryptedString.of("Makes it so you can't place glowstone on glowstone (Use for safe anchor)"));
	private final BooleanSetting echestClick = new BooleanSetting(EncryptedString.of("E-chest click"), true)
			.setDescription(EncryptedString.of("Makes it so you can't click on e-chests with PvP items"));
	private final BooleanSetting anvilClick = new BooleanSetting(EncryptedString.of("Anvil click"), true)
			.setDescription(EncryptedString.of("Makes it so you can't click on anvils with PvP items"));
	public MisclickOptimizer() {
		super(EncryptedString.of("Misclick Optimizer"),
				EncryptedString.of("Prevents you from certain actions in pvp"),
				-1,
				Category.optimizer);
		addSettings(doubleGlowstone, glowstoneMisplace, anchorOnAnchor, obiPunch, glowstoneOnGlowstone, antiCharge, echestClick, anvilClick);
	}
	@Override
	public void onEnable() {
		eventManager.add(BlockBreakingListener.class, this);
		eventManager.add(AttackListener.class, this);
		eventManager.add(ItemUseListener.class, this);
		super.onEnable();
	}
	@Override
	public void onDisable() {
		eventManager.remove(BlockBreakingListener.class, this);
		eventManager.remove(AttackListener.class, this);
		eventManager.remove(ItemUseListener.class, this);
		super.onDisable();
	}
	@Override
	public void onAttack(AttackEvent event) {
		if (mc.crosshairTarget instanceof BlockHitResult hit) {
			if (obiPunch.getValue() && mc.player.isHolding(Items.END_CRYSTAL)) {
				event.cancel();
			}
		}
	}
	@Override
	public void onBlockBreaking(BlockBreakingEvent event) {
		if (mc.crosshairTarget instanceof BlockHitResult hit) {
			if (obiPunch.getValue() && mc.player.isHolding(Items.END_CRYSTAL)) {
				event.cancel();
			}
		}
	}
	@Override
	public void onItemUse(ItemUseEvent event) {
		if (!(mc.crosshairTarget instanceof BlockHitResult hit)) return;
		if (doubleGlowstone.getValue() && mc.player.isHolding(Items.GLOWSTONE) && BlockUtils.isAnchorCharged(hit.getBlockPos())) {
			event.cancel();
			return;
		}
		if (glowstoneMisplace.getValue() && mc.player.isHolding(Items.GLOWSTONE) && !BlockUtils.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
			event.cancel();
			return;
		}
		if (anchorOnAnchor.getValue() && mc.player.isHolding(Items.RESPAWN_ANCHOR) && BlockUtils.isAnchorNotCharged(hit.getBlockPos())) {
			event.cancel();
			return;
		}
		if (glowstoneOnGlowstone.getValue() && mc.player.isHolding(Items.GLOWSTONE) && BlockUtils.isBlock(hit.getBlockPos(), Blocks.GLOWSTONE)) {
			event.cancel();
			return;
		}
		if (antiCharge.getValue() && BlockUtils.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR) && mc.player.isHolding(Items.GLOWSTONE)) {
			event.cancel();
		}
		if (echestClick.getValue() && BlockUtils.isBlock(hit.getBlockPos(), Blocks.ENDER_CHEST)) {
			if (mc.player.getMainHandStack().isIn(ItemTags.SWORDS)
					|| mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL
					|| mc.player.getMainHandStack().getItem() == Items.OBSIDIAN
					|| mc.player.getMainHandStack().getItem() == Items.RESPAWN_ANCHOR
					|| mc.player.getMainHandStack().getItem() == Items.ENDER_CHEST
					|| mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL
					|| mc.player.getMainHandStack().getItem() == Items.GLOWSTONE) {
				event.cancel();
			}
		}
		if (anvilClick.getValue() && (BlockUtils.isBlock(hit.getBlockPos(), Blocks.ANVIL)
				|| BlockUtils.isBlock(hit.getBlockPos(), Blocks.CHIPPED_ANVIL)
				|| BlockUtils.isBlock(hit.getBlockPos(), Blocks.DAMAGED_ANVIL))) {
			if (mc.player.getMainHandStack().isIn(ItemTags.SWORDS)
					|| mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL
					|| mc.player.getMainHandStack().getItem() == Items.OBSIDIAN
					|| mc.player.getMainHandStack().getItem() == Items.RESPAWN_ANCHOR
					|| mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL
					|| mc.player.getMainHandStack().getItem() == Items.ANVIL
					|| mc.player.getMainHandStack().getItem() == Items.DAMAGED_ANVIL
					|| mc.player.getMainHandStack().getItem() == Items.CHIPPED_ANVIL
					|| mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING
					|| mc.player.getMainHandStack().getItem() == Items.GLOWSTONE) {
				event.cancel();
			}
		}

	}
}