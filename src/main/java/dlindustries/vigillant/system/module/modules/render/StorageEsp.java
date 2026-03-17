package dlindustries.vigillant.system.module.modules.render;

import dlindustries.vigillant.system.event.events.PacketReceiveListener;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.EnchantingTableBlockEntity;
import net.minecraft.block.entity.FurnaceBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DrawStyle;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

import java.awt.*;

public final class StorageEsp extends Module implements PacketReceiveListener {
    private final BooleanSetting donutBypass = new BooleanSetting("Bypass?", true)
            .setDescription("Cancels chunk delta packets used by anti-ESP checks");
    private final NumberSetting range = new NumberSetting("Range", 8, 256, 256, 1)
            .setDescription("Maximum render distance");
    private final NumberSetting fillAlpha = new NumberSetting("Fill Alpha", 0, 255, 80, 1)
            .setDescription("Fill opacity");
    private final NumberSetting outlineAlpha = new NumberSetting("Outline Alpha", 0, 255, 220, 1)
            .setDescription("Outline opacity");
    private final BooleanSetting solid = new BooleanSetting("Solid", true)
            .setDescription("Draw filled boxes");
    private final BooleanSetting chests = new BooleanSetting("Chests", true);
    private final BooleanSetting trappedChests = new BooleanSetting("Trapped Chests", true);
    private final BooleanSetting enderChests = new BooleanSetting("Ender Chests", false);
    private final BooleanSetting shulkers = new BooleanSetting("Shulkers", true);
    private final BooleanSetting barrels = new BooleanSetting("Barrels", true);
    private final BooleanSetting furnaces = new BooleanSetting("Furnaces", false);
    private final BooleanSetting enchantTables = new BooleanSetting("Enchant Tables", false);
    private final BooleanSetting spawners = new BooleanSetting("Spawners", true);
    private boolean worldRenderHookRegistered;

    public StorageEsp() {
        super(EncryptedString.of("Storage ESP"),
                EncryptedString.of("Renders block entities through walls"),
                -1,
                Category.ESP);
        addSettings(
                donutBypass,
                range,
                fillAlpha,
                outlineAlpha,
                solid,
                chests,
                trappedChests,
                enderChests,
                shulkers,
                barrels,
                furnaces,
                enchantTables,
                spawners
        );
        registerWorldRenderHook();
    }
    @Override
    public void onEnable() {
        eventManager.add(PacketReceiveListener.class, this);
    }
    @Override
    public void onDisable() {
        eventManager.remove(PacketReceiveListener.class, this);
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
        int chunkRadius = Math.max(1, (int) Math.ceil(range.getValue() / 16.0));
        int playerChunkX = mc.player.getChunkPos().x;
        int playerChunkZ = mc.player.getChunkPos().z;

        try (var ignored = worldRenderer.startDrawingGizmos()) {
            for (int chunkX = playerChunkX - chunkRadius; chunkX <= playerChunkX + chunkRadius; chunkX++) {
                for (int chunkZ = playerChunkZ - chunkRadius; chunkZ <= playerChunkZ + chunkRadius; chunkZ++) {
                    WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(chunkX, chunkZ, false);
                    if (chunk == null) continue;

                    for (BlockPos pos : chunk.getBlockEntityPositions()) {
                        BlockEntity blockEntity = mc.world.getBlockEntity(pos);
                        StorageStyle style = getStyle(blockEntity);
                        if (style == null) continue;

                        Vec3d center = Vec3d.ofCenter(pos);
                        if (mc.player.squaredDistanceTo(center) > rangeSq) continue;

                        int fillColor = withAlpha(style.color(), fillAlpha.getValueInt()).getRGB();
                        int outlineColor = withAlpha(style.color(), outlineAlpha.getValueInt()).getRGB();
                        DrawStyle drawStyle = solid.getValue()
                                ? DrawStyle.filledAndStroked(outlineColor, 1.5f, fillColor)
                                : DrawStyle.stroked(outlineColor, 1.5f);

                        Box worldBox = new Box(pos);
                        GizmoDrawing.box(worldBox, drawStyle).ignoreOcclusion();
                    }
                }
            }
        }
    }
    private StorageStyle getStyle(BlockEntity blockEntity) {
        if (blockEntity == null) return null;

        if (trappedChests.getValue() && blockEntity instanceof TrappedChestBlockEntity) {
            return new StorageStyle(new Color(200, 91, 0));
        }
        if (chests.getValue() && blockEntity instanceof ChestBlockEntity) {
            return new StorageStyle(new Color(156, 91, 0));
        }
        if (enderChests.getValue() && blockEntity instanceof EnderChestBlockEntity) {
            return new StorageStyle(new Color(131, 44, 236));
        }
        if (shulkers.getValue() && blockEntity instanceof ShulkerBoxBlockEntity) {
            return new StorageStyle(new Color(0, 153, 158));
        }
        if (barrels.getValue() && blockEntity instanceof BarrelBlockEntity) {
            return new StorageStyle(new Color(255, 0, 0));
        }
        if (furnaces.getValue() && blockEntity instanceof FurnaceBlockEntity) {
            return new StorageStyle(new Color(125, 125, 125));
        }
        if (enchantTables.getValue() && blockEntity instanceof EnchantingTableBlockEntity) {
            return new StorageStyle(new Color(80, 80, 255));
        }
        if (spawners.getValue() && blockEntity instanceof MobSpawnerBlockEntity) {
            return new StorageStyle(new Color(27, 207, 0));
        }

        return null;
    }
    private static Color withAlpha(Color base, int alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
    }
    @Override
    public void onPacketReceive(PacketReceiveListener.PacketReceiveEvent event) {
        if (donutBypass.getValue() && event.packet instanceof ChunkDeltaUpdateS2CPacket) {
            event.cancel();
        }
    }
    private record StorageStyle(Color color) {
    }
}
