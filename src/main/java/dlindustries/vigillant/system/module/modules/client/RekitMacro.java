package dlindustries.vigillant.system.module.modules.client;

import dlindustries.vigillant.system.event.events.TickListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.KeybindSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.module.setting.StringSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.KeyUtils;
import net.minecraft.client.gui.screen.ChatScreen;

public final class RekitMacro extends Module implements TickListener {
    private final StringSetting  message     = new StringSetting(EncryptedString.of("RekitCommand"), "/Kit1")
            .setDescription(EncryptedString.of("The servers kit getting command (k1, kit1, etc)"));
    private final KeybindSetting messageKey  = new KeybindSetting(EncryptedString.of("Rekit Key"), -1, false)
            .setDescription(EncryptedString.of("Key to trigger the rekit"));
    private final NumberSetting  delay       = new NumberSetting(EncryptedString.of("Delay"), 0, 500, 50, 1)
            .setDescription(EncryptedString.of("Delay before entering the command"));
    private final BooleanSetting silent      = new BooleanSetting(EncryptedString.of("Silent"), false)
            .setDescription(EncryptedString.of("Silently does it"));
    private final NumberSetting  chatVisible = new NumberSetting(EncryptedString.of("Chat open/close delay"), 0, 500, 150, 1)
            .setDescription(EncryptedString.of("How long it takes to type and enter the message"));
    private boolean keyWasDown = false;
    private long    lastSent   = 0;
    private int     stage      = 0;
    private long    stageStart = 0;
    public RekitMacro() {
        super(EncryptedString.of("RekitMacro"),
                EncryptedString.of("Sends a command to rekit on keybind press, may not work in some servers, don't use while sprinting"),
                -1,
                Category.CLIENT);
        addSettings(message, messageKey, delay, silent, chatVisible);
    }
    @Override
    public void onEnable() {
        eventManager.add(TickListener.class, this);
        keyWasDown = false;
        lastSent   = 0;
        stage      = 0;
        stageStart = 0;
        super.onEnable();
    }
    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        super.onDisable();
    }
    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (stage != 0) {
            long now = System.currentTimeMillis();
            switch (stage) {
                case 1:
                    stage      = 2;
                    stageStart = now;
                    break;
                case 2:
                    if (now - stageStart >= chatVisible.getValueInt()) {
                        mc.setScreen(null);
                        stage = 0;
                    }
                    break;
            }
            return;
        }
        if (mc.currentScreen != null && silent.getValue()) return;
        boolean keyDown = KeyUtils.isKeyPressed(messageKey.getKey());
        if (keyDown && !keyWasDown) {
            long now = System.currentTimeMillis();
            if (now - lastSent >= delay.getValueInt()) {
                if (!silent.getValue()) {
                    if (mc.currentScreen != null) return;
                    send(message.getValue());
                    mc.setScreen(new ChatScreen("", false));
                    stage      = 1;
                    stageStart = now;
                } else {
                    send(message.getValue());
                }
                lastSent = now;
            }
        }
        keyWasDown = keyDown;
    }
    private void send(String input) {
        if (input == null || input.isBlank()) return;
        if (input.startsWith("/")) {
            mc.player.networkHandler.sendChatCommand(input.substring(1));
        } else {
            mc.player.networkHandler.sendChatMessage(input);
        }
    }
}