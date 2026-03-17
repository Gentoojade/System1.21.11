package dlindustries.vigillant.system.module.modules.render;

import dlindustries.vigillant.system.event.events.HudListener;
import dlindustries.vigillant.system.gui.ClickGui;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.modules.client.NameProtect;
import dlindustries.vigillant.system.module.setting.BooleanSetting;
import dlindustries.vigillant.system.module.setting.NumberSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import dlindustries.vigillant.system.utils.RenderUtils;
import dlindustries.vigillant.system.utils.TextRenderer;
import dlindustries.vigillant.system.utils.Utils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import java.awt.*;
import java.util.List;

public final class HUD extends Module implements HudListener {
	private static final CharSequence system = EncryptedString.of("System |");
	private final BooleanSetting info = new BooleanSetting(EncryptedString.of("Info"), true);
	private final BooleanSetting coords = new BooleanSetting(EncryptedString.of("Coords"), true)
			.setDescription(EncryptedString.of("Renders your current coordinates"));
	private final BooleanSetting modules = new BooleanSetting("Modules", false)
			.setDescription(EncryptedString.of("Renders module list"));
	private final BooleanSetting radar = new BooleanSetting(EncryptedString.of("Radar"), true)
			.setDescription(EncryptedString.of("Renders a player radar on the right side"));
	private final NumberSetting radarRange = new NumberSetting(EncryptedString.of("Radar Range"), 20, 200, 200, 1)
			.setDescription(EncryptedString.of("Detection range of the radar in blocks"));
	private final NumberSetting radarRadius = new NumberSetting(EncryptedString.of("Radar Size"), 50, 200, 125, 1)
			.setDescription(EncryptedString.of("Visual radius of the radar circle"));
	private final BooleanSetting radarMobs = new BooleanSetting(EncryptedString.of("Mobs"), true)
			.setDescription(EncryptedString.of("Show hostile and passive mobs on radar"));
	public HUD() {
		super(EncryptedString.of("HUD"),
				EncryptedString.of("Overlay info as you play"),
				-1,
				Category.RENDER);
		addSettings(info, coords, modules, radar, radarRange, radarRadius, radarMobs);
	}
	@Override
	public void onEnable() {
		eventManager.add(HudListener.class, this);
		super.onEnable();
	}
	@Override
	public void onDisable() {
		eventManager.remove(HudListener.class, this);
		super.onDisable();
	}
	@Override
	public void onRenderHud(HudEvent event) {
		if (mc.currentScreen instanceof ClickGui) return;
		if (mc.currentScreen == dlindustries.vigillant.system.system.INSTANCE.clickGui) return;

		DrawContext context = event.context;
		float scaleFactor = (float) mc.getWindow().getScaleFactor();
		float invScale = 1.0f / scaleFactor;
		context.getMatrices().pushMatrix();
		context.getMatrices().scale(invScale, invScale);
		if (info.getValue() && mc.player != null) {
			String playerName = mc.player.getName().getString();
			NameProtect nameProtect =
					dlindustries.vigillant.system.system.INSTANCE.getModuleManager().getModule(
							NameProtect.class);
			if (nameProtect != null) {
				playerName = nameProtect.replaceName(playerName);
			}
			String serverName = (mc.getCurrentServerEntry() == null ? "None" : mc.getCurrentServerEntry().address);
			String buildString = "System";
			String hudText = String.format("%s | %s | %s | %d FPS", buildString, playerName, serverName, mc.getCurrentFps());
			int textX = 15;
			int textY = 15;
			int textWidth = TextRenderer.getWidth(hudText);
			int textHeight = mc.textRenderer.fontHeight;
			int bgPadding = 8;
			RenderUtils.renderRoundedQuad(
					context.getMatrices(),
					new Color(35, 35, 35, 180),
					textX - bgPadding,
					textY - bgPadding,
					textX + textWidth + bgPadding,
					textY + textHeight + bgPadding,
					5,
					15
			);
			TextRenderer.drawString(
					hudText,
					context,
					textX,
					textY,
					Utils.getMainColor(255, 4).getRGB()
			);
		}
		if (coords.getValue() && mc.player != null) {
			int x = (int) mc.player.getX();
			int y = (int) mc.player.getY();
			int z = (int) mc.player.getZ();
			String coordText = String.format("XYZ: %d / %d / %d", x, y, z);
			int textX = 15;
			int textY = 45;
			int textWidth = TextRenderer.getWidth(coordText);
			int textHeight = mc.textRenderer.fontHeight;
			int bgPadding = 8;
			RenderUtils.renderRoundedQuad(
					context.getMatrices(),
					new Color(35, 35, 35, 180),
					textX - bgPadding,
					textY - bgPadding,
					textX + textWidth + bgPadding,
					textY + textHeight + bgPadding,
					5,
					15
			);
			TextRenderer.drawString(
					coordText,
					context,
					textX,
					textY,
					Utils.getMainColor(255, 4).getRGB()
			);
		}
		if (modules.getValue()) {
			int offset = 120;
			List<Module> enabledModules = dlindustries.vigillant.system.system.INSTANCE
					.getModuleManager()
					.getEnabledModules()
					.stream()
					.sorted((m1, m2) ->
							Integer.compare(
									TextRenderer.getWidth(m2.getName()),
									TextRenderer.getWidth(m1.getName())
							)
					)
					.toList();
			for (Module module : enabledModules) {
				int charOffset = 6 + TextRenderer.getWidth(module.getName());
				RenderUtils.renderRoundedQuad(
						context.getMatrices(),
						new Color(0, 0, 0, 175),
						0,
						offset - 4,
						charOffset + 5,
						offset + (mc.textRenderer.fontHeight * 2) - 1,
						0,
						0
				);
				context.fillGradient(
						0,
						offset - 4,
						2,
						offset + (mc.textRenderer.fontHeight * 2),
						Utils.getMainColor(255, enabledModules.indexOf(module)).getRGB(),
						Utils.getMainColor(255, enabledModules.indexOf(module) + 1).getRGB()
				);
				TextRenderer.drawString(
						module.getName(),
						context,
						8,
						offset,
						Utils.getMainColor(255, enabledModules.indexOf(module)).getRGB()
				);
				offset += (mc.textRenderer.fontHeight * 2) + 3;
			}
		}
		if (radar.getValue() && mc.player != null && mc.world != null) {
			int scaledWidth  = mc.getWindow().getFramebufferWidth();
			int scaledHeight = mc.getWindow().getFramebufferHeight();
			int rad        = (int) radarRadius.getValue();
			int bgPad      = 10;
			int centerX    = scaledWidth  - rad - bgPad * 2 - 15;
			int centerY    = rad + bgPad * 2 + 15;
			Color themeColor = Utils.getMainColor(255, 0);
			int innerRgb = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 25).getRGB();
			for (int dy = -rad; dy <= rad; dy++) {
				int dx = (int) Math.sqrt((double) rad * rad - (double) dy * dy);
				context.fill(centerX - dx, centerY + dy, centerX + dx, centerY + dy + 1, innerRgb);
			}
			int crossColor = new Color(80, 80, 80, 140).getRGB();
			context.fill(centerX - rad, centerY, centerX + rad, centerY + 1, crossColor);
			context.fill(centerX, centerY - rad, centerX + 1, centerY + rad, crossColor);
			float rangeVal = (float) radarRange.getValue();
			if (rangeVal >= 100 && rangeVal <= 200) {
				drawCircleOutline(context, centerX, centerY, rad / 2, new Color(70, 70, 70, 100), 1);
			} else if (rangeVal > 200) {
				drawCircleOutline(context, centerX, centerY, rad / 3, new Color(70, 70, 70, 100), 1);
				drawCircleOutline(context, centerX, centerY, (rad * 2) / 3, new Color(70, 70, 70, 100), 1);
			}
			drawCircleOutline(context, centerX, centerY, rad - 1, Utils.getMainColor(120, 0), 3);
			fillDot(context, centerX, centerY, 3, new Color(0, 0, 0, 255));
			int northTickColor = Utils.getMainColor(200, 2).getRGB();
			context.fill(centerX - 1, centerY - rad - bgPad / 2, centerX + 1, centerY - rad + 3, northTickColor);
			float range = (float) radarRange.getValue();
			float yawRad = (float) Math.toRadians(mc.player.getYaw());
			for (AbstractClientPlayerEntity other : mc.world.getPlayers()) {
				if (other == mc.player) continue;
				double dx =  other.getX() - mc.player.getX();
				double dz =  other.getZ() - mc.player.getZ();
				double dist = Math.sqrt(dx * dx + dz * dz);
				double rotX =  dx * Math.cos(yawRad) - dz * Math.sin(yawRad);
				double rotZ =  dx * Math.sin(yawRad) + dz * Math.cos(yawRad);
				double scale = rad / (double) range;
				int dotX = (int) Math.round(centerX + rotX * scale);
				int dotY = (int) Math.round(centerY + rotZ * scale);
				double vecX = dotX - centerX;
				double vecY = dotY - centerY;
				double vecDist = Math.sqrt(vecX * vecX + vecY * vecY);
				if (vecDist > rad - 3) {
					double factor = (rad - 3) / vecDist;
					dotX = (int) Math.round(centerX + vecX * factor);
					dotY = (int) Math.round(centerY + vecY * factor);
				}
				float distFraction = (float) Math.min(dist / range, 1.0f);
				Color base = Utils.getMainColor(255, 0);
				int a = (int) (230 - 80 * distFraction);
				Color dotColor = new Color(base.getRed(), base.getGreen(), base.getBlue(), a);

				fillDot(context, dotX, dotY, 3, dotColor);
			}
			if (radarMobs.getValue()) {
				mc.world.getEntities().forEach(entity -> {
					Color mobColor = null;
					if (entity instanceof HostileEntity) {
						Color t = Utils.getMainColor(255, 4);
						mobColor = new Color(
								Math.max(0, t.getRed() - 40),
								Math.max(0, t.getGreen() - 40),
								Math.max(0, t.getBlue() - 40),
								210
						);
					} else if (entity instanceof PassiveEntity) {
						Color t = Utils.getMainColor(255, 8);
						mobColor = new Color(
								Math.min(255, t.getRed() + 60),
								Math.min(255, t.getGreen() + 60),
								Math.min(255, t.getBlue() + 60),
								180
						);
					}
					if (mobColor == null) return;
					double dx = entity.getX() - mc.player.getX();
					double dz = entity.getZ() - mc.player.getZ();
					double dist = Math.sqrt(dx * dx + dz * dz);
					if (dist > range) return;
					double rotX = dx * Math.cos(yawRad) - dz * Math.sin(yawRad);
					double rotZ = dx * Math.sin(yawRad) + dz * Math.cos(yawRad);
					double scale = rad / (double) range;
					int dotX = (int) Math.round(centerX + rotX * scale);
					int dotY = (int) Math.round(centerY + rotZ * scale);
					double vecX = dotX - centerX;
					double vecY = dotY - centerY;
					double vecDist = Math.sqrt(vecX * vecX + vecY * vecY);
					if (vecDist > rad - 3) {
						double factor = (rad - 3) / vecDist;
						dotX = (int) Math.round(centerX + vecX * factor);
						dotY = (int) Math.round(centerY + vecY * factor);
					}
					fillDot(context, dotX, dotY, 2, mobColor);
				});
			}

		}

		context.getMatrices().popMatrix();
	}
	private void fillDot(DrawContext context, int cx, int cy, int r, Color color) {
		context.fill(cx - r, cy - r, cx + r, cy + r, color.getRGB());
	}
	private void drawCircleOutline(DrawContext context, int cx, int cy, int r, Color color, int thickness) {
		int rgb = color.getRGB();
		int x = 0, y = r;
		int d = 3 - 2 * r;
		while (y >= x) {
			plotCirclePoints(context, cx, cy, x, y, rgb, thickness);
			x++;
			if (d > 0) {
				y--;
				d += 4 * (x - y) + 10;
			} else {
				d += 4 * x + 6;
			}
		}
	}
	private void plotCirclePoints(DrawContext context, int cx, int cy, int x, int y, int rgb, int t) {
		int h = t / 2;
		context.fill(cx + x - h, cy + y - h, cx + x - h + t, cy + y - h + t, rgb);
		context.fill(cx - x - h, cy + y - h, cx - x - h + t, cy + y - h + t, rgb);
		context.fill(cx + x - h, cy - y - h, cx + x - h + t, cy - y - h + t, rgb);
		context.fill(cx - x - h, cy - y - h, cx - x - h + t, cy - y - h + t, rgb);
		context.fill(cx + y - h, cy + x - h, cx + y - h + t, cy + x - h + t, rgb);
		context.fill(cx - y - h, cy + x - h, cx - y - h + t, cy + x - h + t, rgb);
		context.fill(cx + y - h, cy - x - h, cx + y - h + t, cy - x - h + t, rgb);
		context.fill(cx - y - h, cy - x - h, cx - y - h + t, cy - x - h + t, rgb);
	}
}