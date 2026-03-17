package dlindustries.vigillant.system.module.modules.render;

import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.Utils;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.awt.*;

public final class SuperVision extends Module {
    private final NumberSetting range = new NumberSetting(EncryptedString.of("Range"), 8, 256, 256, 1)
            .setDescription(EncryptedString.of("Maximum target distance"));
    private final NumberSetting fillAlpha = new NumberSetting(EncryptedString.of("Fill Alpha"), 0, 255, 80, 1)
            .setDescription(EncryptedString.of("Fill opacity"));
    private final NumberSetting outlineAlpha = new NumberSetting(EncryptedString.of("Outline Alpha"), 0, 255, 220, 1)
            .setDescription(EncryptedString.of("Outline opacity"));
    private final BooleanSetting solid = new BooleanSetting(EncryptedString.of("Solid"), true)
            .setDescription(EncryptedString.of("Draw filled boxes"));
    private final BooleanSetting mobsAndAnimals = new BooleanSetting(EncryptedString.of("Mobs & Animals"), false)
            .setDescription(EncryptedString.of("Show animals and hostile mobs"));
    private final BooleanSetting self = new BooleanSetting(EncryptedString.of("Self"), false)
            .setDescription(EncryptedString.of("Show yourself"));
    private final BooleanSetting items = new BooleanSetting(EncryptedString.of("Items"), false)
            .setDescription(EncryptedString.of("Show dropped items"));

    private boolean worldRenderHookRegistered;

    public SuperVision() {
        super(EncryptedString.of("EntityESP"),
                EncryptedString.of("Renders entities trough walls"),
                -1,
                Category.ESP);
        addSettings(range, fillAlpha, outlineAlpha, solid, mobsAndAnimals, self, items);
        registerWorldRenderHook();
    }
    private void registerWorldRenderHook() {
        if (worldRenderHookRegistered) return;
        worldRenderHookRegistered = true;
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(this::onWorldRender);
    }
    private void onWorldRender(WorldRenderContext context) {
        if (!isEnabled() || mc.world == null || mc.player == null) return;
        WorldRenderer worldRenderer = context.worldRenderer();
        GameRenderer gameRenderer = context.gameRenderer();
        if (worldRenderer == null || gameRenderer == null) return;
        Camera camera = gameRenderer.getCamera();
        if (camera == null) return;
        float tickDelta = mc.getRenderTickCounter().getDynamicDeltaTicks();
        double rangeSq = range.getValue() * range.getValue();
        Color baseColor = Utils.getMainColor(255, 0);
        Color fillColor = applyAlpha(baseColor, fillAlpha.getValueInt());
        Color outlineColor = applyAlpha(baseColor.brighter(), outlineAlpha.getValueInt());
        DrawStyle style = solid.getValue()
                ? DrawStyle.filledAndStroked(outlineColor.getRGB(), 1.5f, fillColor.getRGB())
                : DrawStyle.stroked(outlineColor.getRGB(), 1.5f);
        try (var ignored = worldRenderer.startDrawingGizmos()) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player && !self.getValue()) continue;
                if (!player.isAlive() || player.isSpectator()) continue;
                if (mc.player.squaredDistanceTo(player) > rangeSq) continue;

                Box worldBox = getInterpolatedBox(player, tickDelta);
                GizmoDrawing.box(worldBox, style).ignoreOcclusion();
            }
            if (mobsAndAnimals.getValue()) {
                for (Entity entity : mc.world.getEntities()) {
                    if (entity instanceof PlayerEntity) continue;
                    if (!(entity instanceof AnimalEntity || entity instanceof Monster)) continue;
                    if (!entity.isAlive()) continue;
                    if (mc.player.squaredDistanceTo(entity) > rangeSq) continue;

                    Box worldBox = getInterpolatedBox(entity, tickDelta);
                    GizmoDrawing.box(worldBox, style).ignoreOcclusion();
                }
            }
            if (items.getValue()) {
                for (Entity entity : mc.world.getEntities()) {
                    if (!(entity instanceof ItemEntity)) continue;
                    if (!entity.isAlive()) continue;
                    if (mc.player.squaredDistanceTo(entity) > rangeSq) continue;

                    Box worldBox = getInterpolatedBox(entity, tickDelta);
                    GizmoDrawing.box(worldBox, style).ignoreOcclusion();
                }
            }
        }
    }
    private static Box getInterpolatedBox(Entity entity, float tickDelta) {
        Vec3d currentPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
        Vec3d interpOffset = lerpedPos.subtract(currentPos);
        return entity.getBoundingBox().offset(interpOffset.x, interpOffset.y, interpOffset.z);
    }
    private Color applyAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}