package dlindustries.vigillant.system.module.modules.crystal;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.ModeSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.FakeInvScreen;
import dlindustries.vigillant.system.utils.InventoryUtils;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

public final class AutoInventoryTotem extends Module implements TickListener {
	public enum Mode {
		Blatant, Random
	}

	private final ModeSetting<Mode> mode = new ModeSetting<>(EncryptedString.of("Mode"), Mode.Random, Mode.class)
			.setDescription(EncryptedString.of("Whether to randomize the toteming pattern or no"));
	private final NumberSetting delay = new NumberSetting(
			EncryptedString.of("Delay"),
			0, 250, 50, 1
	).setDescription(EncryptedString.of("Delay before performing totem swaps"));
	private final BooleanSetting hotbar = new BooleanSetting(EncryptedString.of("Hotbar"), true)
			.setDescription(EncryptedString.of("Puts a totem in your hotbar as well, if enabled (Setting below will work if this is enabled)"));private final NumberSetting totemSlot = new NumberSetting(EncryptedString.of("Totem Slot"), 1, 9, 9, 1)
			.setDescription(EncryptedString.of("Your preferred totem slot"));
	private final BooleanSetting autoSwitch = new BooleanSetting(EncryptedString.of("Auto Switch"), true)
			.setDescription(EncryptedString.of("Switches to totem slot when going inside the inventory"));
	private final BooleanSetting forceTotem = new BooleanSetting(EncryptedString.of("Force Totem"), false)
			.setDescription(EncryptedString.of("Puts the totem in the slot, regardless if its space is taken up by something else"));
	private final BooleanSetting autoOpen = new BooleanSetting(EncryptedString.of("Auto Open"), false)
			.setDescription(EncryptedString.of("Automatically opens and closes the inventory for you turning it in to an AutoTotem"));
	private final NumberSetting stayOpenFor = new NumberSetting(
			EncryptedString.of("Stay Open For"),
			0, 250, 50, 1
	).setDescription(EncryptedString.of("How long to keep the inventory open after swaps"));
	private long nextActionTime = 0;
	private boolean performedSwaps = false;
	public AutoInventoryTotem() {
		super(EncryptedString.of("AutoTotem"),
				EncryptedString.of("AutoTotem fully automated may be detected"),
				-1,
				Category.CRYSTAL);
		addSettings(mode, delay, hotbar, totemSlot, autoSwitch, forceTotem, autoOpen, stayOpenFor);
	}
	@Override
	public void onEnable() {
		eventManager.add(TickListener.class, this);
		resetState();
		super.onEnable();
	}
	@Override
	public void onDisable() {
		eventManager.remove(TickListener.class, this);
		super.onDisable();
	}
	private void resetState() {
		nextActionTime = 0;
		performedSwaps = false;
	}
	@Override
	public void onTick() {
		long now = System.currentTimeMillis();
		if (shouldOpenScreen() && autoOpen.getValue()) {
			mc.setScreen(new FakeInvScreen(mc.player));
		}
		if (!(mc.currentScreen instanceof InventoryScreen || mc.currentScreen instanceof FakeInvScreen)) {
			resetState();
			return;
		}
		if (autoSwitch.getValue()) {
			mc.player.getInventory().setSelectedSlot(totemSlot.getValueInt() - 1);
		}
		if (!performedSwaps) {
			if (nextActionTime == 0) {
				nextActionTime = now + (long) delay.getValue();
			}
			if (now < nextActionTime) {
				return;
			}
			performSwaps();
			performedSwaps = true;
			if (autoOpen.getValue() && shouldCloseScreen()) {
				if (stayOpenFor.getValueInt() > 0) {
					nextActionTime = now + (long) stayOpenFor.getValue();
				} else {
					closeInventory();
				}
			} else {
				nextActionTime = Long.MAX_VALUE;
			}
		}
		else if (autoOpen.getValue() && shouldCloseScreen()) {
			if (now < nextActionTime) {
				return;
			}
			closeInventory();
		}
	}
	private void performSwaps() {
		if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
			int slot = mode.isMode(Mode.Blatant) ? InventoryUtils.findTotemSlot() : InventoryUtils.findRandomTotemSlot();
			if (slot != -1) {
				mc.interactionManager.clickSlot(
						((InventoryScreen) mc.currentScreen).getScreenHandler().syncId,
						slot,
						40,
						SlotActionType.SWAP,
						mc.player
				);
				return;
			}
		}
		if (hotbar.getValue()) {
			ItemStack mainHand = mc.player.getMainHandStack();
			if (mainHand.isEmpty() || (forceTotem.getValue() && mainHand.getItem() != Items.TOTEM_OF_UNDYING)) {
				int slot = mode.isMode(Mode.Blatant) ? InventoryUtils.findTotemSlot() : InventoryUtils.findRandomTotemSlot();
				if (slot != -1) {
					mc.interactionManager.clickSlot(
							((InventoryScreen) mc.currentScreen).getScreenHandler().syncId,
							slot,
							mc.player.getInventory().getSelectedSlot(),
							SlotActionType.SWAP,
							mc.player
					);
				}
			}
		}
	}
	private void closeInventory() {
		if (mc.currentScreen != null) {
			mc.currentScreen.close();
		}
		resetState();
	}
	public boolean shouldCloseScreen() {
		if (hotbar.getValue()) {
			return (mc.player.getInventory().getStack(totemSlot.getValueInt() - 1).getItem() == Items.TOTEM_OF_UNDYING
					&& mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING)
					&& mc.currentScreen instanceof FakeInvScreen;
		} else {
			return (mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING)
					&& mc.currentScreen instanceof FakeInvScreen;
		}
	}
	public boolean shouldOpenScreen() {
		if (hotbar.getValue()) {
			return (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING
					|| mc.player.getInventory().getStack(totemSlot.getValueInt() - 1).getItem() != Items.TOTEM_OF_UNDYING)
					&& !(mc.currentScreen instanceof FakeInvScreen)
					&& InventoryUtils.countItemExceptHotbar(item -> item == Items.TOTEM_OF_UNDYING) != 0;
		} else {
			return (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING
					&& !(mc.currentScreen instanceof FakeInvScreen)
					&& InventoryUtils.countItemExceptHotbar(item -> item == Items.TOTEM_OF_UNDYING) != 0);
		}
	}
}