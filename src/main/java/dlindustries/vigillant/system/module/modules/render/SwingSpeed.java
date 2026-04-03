package dlindustries.vigillant.system.module.modules.render;

import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;

public final class SwingSpeed extends Module {

    private static SwingSpeed instance;
    public static final NumberSetting speed = new NumberSetting(EncryptedString.of("Animation Speed"), 1, 20, 14, 1);
    public SwingSpeed() {
        super(EncryptedString.of("Swing Speed"),
                EncryptedString.of("Modifies the speed of your hand swing animation, only visual."),
                -1,
                Category.RENDER);
        addSettings(speed);
        instance = this;
    }
    public static SwingSpeed getInstance() {
        return instance;
    }
}