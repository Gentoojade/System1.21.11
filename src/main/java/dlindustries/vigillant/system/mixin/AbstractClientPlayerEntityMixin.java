package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.module.modules.client.SkinSpoofer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void spoofSkin(CallbackInfoReturnable<SkinTextures> cir) {
        SkinSpoofer mod = SkinSpoofer.INSTANCE;
        if (mod == null || !mod.isEnabled() || mod.spoofedSkin == null) return;
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity)(Object)this;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (!self.getUuid().equals(mc.player.getUuid())) return;
        PlayerSkinType type = mod.isSlim() ? PlayerSkinType.SLIM : PlayerSkinType.WIDE;
        AssetInfo.TextureAsset skinAsset = new AssetInfo.TextureAsset() {
            @Override
            public Identifier id() { return mod.spoofedSkin; }
            @Override
            public Identifier texturePath() { return mod.spoofedSkin; }
        };
        AssetInfo.TextureAsset capeAsset = mod.spoofedCape == null ? null : new AssetInfo.TextureAsset() {
            @Override
            public Identifier id() { return mod.spoofedCape; }
            @Override
            public Identifier texturePath() { return mod.spoofedCape; }
        };
        cir.setReturnValue(new SkinTextures(
                skinAsset,
                capeAsset,
                capeAsset,
                type,
                false
        ));
    }
}