package dlindustries.vigillant.system.mixin;

import dlindustries.vigillant.system.module.modules.optimizer.AutoTool;
import dlindustries.vigillant.system.module.modules.optimizer.AutoPickaxe;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class AutoToolMixin {
    @Inject(method = "attackBlock", at = @At("HEAD"))
    private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        BlockState state = mc.world.getBlockState(pos);

        AutoTool autoTool = AutoTool.getInstance();
        if (autoTool != null) autoTool.onStartBreaking(state);

        AutoPickaxe autoPickaxe = AutoPickaxe.getInstance();
        if (autoPickaxe != null) autoPickaxe.onStartBreaking(pos, state);
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void onUpdateBreaking(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        BlockState state = mc.world.getBlockState(pos);

        AutoTool autoTool = AutoTool.getInstance();
        if (autoTool != null) autoTool.onStartBreaking(state);

        AutoPickaxe autoPickaxe = AutoPickaxe.getInstance();
        if (autoPickaxe != null) autoPickaxe.onStartBreaking(pos, state);
    }

    @Inject(method = "cancelBlockBreaking", at = @At("HEAD"))
    private void onCancelBreaking(CallbackInfo ci) {
        AutoTool autoTool = AutoTool.getInstance();
        if (autoTool != null) autoTool.onStopBreaking();

        AutoPickaxe autoPickaxe = AutoPickaxe.getInstance();
        if (autoPickaxe != null) autoPickaxe.onStopBreaking();
    }
}