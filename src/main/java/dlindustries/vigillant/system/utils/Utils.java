package dlindustries.vigillant.system.utils;

import dlindustries.vigillant.system.module.modules.client.ClickGUI;
import dlindustries.vigillant.system.module.modules.client.NineElevenPrevent;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;

public final class Utils {
	private static final Color[] JOE_BIDEN = {
			new Color(120, 20, 230),
			new Color(135, 30, 238),
			new Color(150, 45, 248),
			new Color(160, 60, 255),
			new Color(140, 55, 255),
			new Color(130, 50, 255),
			new Color(118, 65, 255),
			new Color(105, 95, 255),
			new Color(100, 140, 255),
			new Color(82, 150, 255),
			new Color(72, 126, 255),
			new Color(68, 148, 255),
			new Color(64, 170, 255),
			new Color(100, 145, 255),
			new Color(140, 110, 255),
			new Color(120, 20, 230)
	};
	private static final Color[] DEFAULT_COLORS = JOE_BIDEN;
	private static final Color[] GRASS_COLORS = {
			new Color(50, 200, 50),
			new Color(65, 210, 60),
			new Color(80, 220, 70),
			new Color(100, 230, 80),
			new Color(120, 240, 88),
			new Color(140, 250, 95),
			new Color(155, 255, 100),
			new Color(175, 255, 110),
			new Color(200, 255, 120),
			new Color(175, 255, 110),
			new Color(150, 255, 100),
			new Color(120, 240, 88),
			new Color(100, 230, 80),
			new Color(65, 210, 60),
			new Color(40, 190, 45),
			new Color(50, 200, 50)
	};
	private static final Color[] CRIMSON_COLORS = {
			new Color(150, 0, 0),
			new Color(160, 0, 0),
			new Color(175, 0, 0),
			new Color(190, 0, 0),
			new Color(205, 0, 0),
			new Color(220, 0, 0),
			new Color(235, 15, 15),
			new Color(248, 35, 35),
			new Color(255, 50, 50),
			new Color(245, 30, 30),
			new Color(230, 10, 10),
			new Color(215, 0, 0),
			new Color(200, 0, 0),
			new Color(180, 0, 0),
			new Color(160, 0, 0),
			new Color(150, 0, 0)
	};
	private static final Color[] CUTE_COLORS = {
			new Color(255, 80, 160),
			new Color(255, 88, 168),
			new Color(255, 100, 180),
			new Color(255, 115, 188),
			new Color(255, 130, 195),
			new Color(255, 148, 200),
			new Color(255, 163, 210),
			new Color(255, 175, 218),
			new Color(255, 200, 230),
			new Color(255, 178, 220),
			new Color(255, 162, 210),
			new Color(255, 148, 200),
			new Color(255, 130, 195),
			new Color(255, 112, 185),
			new Color(255, 95, 172),
			new Color(255, 80, 160)
	};
	private static final Color[] SNOW_COLORS = {
			new Color(0, 70, 180),
			new Color(0, 95, 195),
			new Color(0, 118, 215),
			new Color(0, 140, 235),
			new Color(0, 155, 248),
			new Color(25, 165, 255),
			new Color(55, 178, 255),
			new Color(85, 192, 255),
			new Color(125, 208, 255),
			new Color(150, 220, 255),
			new Color(118, 205, 255),
			new Color(88, 190, 255),
			new Color(55, 175, 255),
			new Color(20, 155, 245),
			new Color(0, 112, 210),
			new Color(0, 70, 180)
	};
	private static final Color[] YELLOW_COLORS = {
			new Color(236, 195, 0),
			new Color(245, 198, 0),
			new Color(255, 200, 0),
			new Color(255, 203, 25),
			new Color(255, 207, 50),
			new Color(255, 209, 72),
			new Color(255, 211, 90),
			new Color(255, 214, 108),
			new Color(255, 219, 123),
			new Color(255, 214, 108),
			new Color(255, 211, 90),
			new Color(255, 207, 65),
			new Color(255, 204, 40),
			new Color(255, 200, 15),
			new Color(248, 197, 0),
			new Color(236, 195, 0)
	};
	private static final  Color[] WHITE_COLORS = {
			new Color(200, 215, 255),
			new Color(210, 222, 255),
			new Color(220, 230, 255),
			new Color(230, 237, 255),
			new Color(240, 244, 255),
			new Color(248, 250, 255),
			new Color(255, 255, 255),
			new Color(245, 248, 255),
			new Color(235, 242, 255),
			new Color(220, 233, 255),
			new Color(210, 226, 255),
			new Color(205, 220, 255),
			new Color(215, 228, 255),
			new Color(225, 235, 255),
			new Color(212, 220, 255),
			new Color(200, 215, 255)
	};
	private static final  Color[] LGBTQ_COLORS = {
			new Color(255,   0,   0),
			new Color(255,  96,   0),
			new Color(255, 191,   0),
			new Color(223, 255,   0),
			new Color(128, 255,   0),
			new Color( 32, 255,   0),
			new Color(  0, 255,  64),
			new Color(  0, 255, 159),
			new Color(  0, 255, 255),
			new Color(  0, 159, 255),
			new Color(  0,  64, 255),
			new Color( 32,   0, 255),
			new Color(128,   0, 255),
			new Color(223,   0, 255),
			new Color(255,   0, 191),
			new Color(255,   0,  96),
			new Color(255,   0,   0),
	};
	public static void copyVector(final Vector3d vector3d, final Vec3d vec3d) {
		vector3d.x = vec3d.x;
		vector3d.y = vec3d.y;
		vector3d.z = vec3d.z;
	}
	private static ClickGUI.Theme safeGetTheme() {
		try {
			ClickGUI.Theme theme = ClickGUI.theme.getMode();
			if (theme == null) return ClickGUI.Theme.PURPLE;
			return theme;
		} catch (Exception ignored) {

			return ClickGUI.Theme.PURPLE;
		}
	}
	public static Color getMainColor(int alpha, int unusedIncrement) {
		try {
			if (ClickGUI.breathing.getValue()) {
				Color[] colors = getThemeColors();
				long currentTime = System.currentTimeMillis();
				int cycleDuration = 8000; // ms for a full cycle
				float progress = (currentTime % cycleDuration) / (float) cycleDuration;
				int colorCount = colors.length;
				if (colorCount == 0) return new Color(120, 20, 230, alpha); // safe fallback
				float scaledProgress = progress * colorCount;
				int index = (int) scaledProgress;
				float interpolation = scaledProgress - index;
				Color start = colors[index % colorCount];
				Color end = colors[(index + 1) % colorCount];
				return new Color(
						interpolateColor(start.getRed(), end.getRed(), interpolation),
						interpolateColor(start.getGreen(), end.getGreen(), interpolation),
						interpolateColor(start.getBlue(), end.getBlue(), interpolation),
						alpha
				);
			} else {

				return getThemeStaticColor(alpha);
			}
		} catch (Exception e) {
			return new Color(120, 20, 230, alpha);
		}
	}
	private static Color[] getThemeColors() {
		ClickGUI.Theme theme = safeGetTheme();
		switch (theme) {
			case GREEN:
				return GRASS_COLORS;
			case RED:
				return CRIMSON_COLORS;
			case PINK:
				return CUTE_COLORS;
			case BLUE:
				return SNOW_COLORS;
			case YELLOW:
				return YELLOW_COLORS;
			case WHITE:
				return WHITE_COLORS;
			case RAINBOW:
				return LGBTQ_COLORS;
			default:
				return DEFAULT_COLORS;
		}
	}
	private static Color getThemeStaticColor(int alpha) {
		ClickGUI.Theme theme = safeGetTheme();
		switch (theme) {
			case GREEN:
				return new Color(50, 200, 50, alpha);
			case RED:
				return new Color(200, 0, 0, alpha);
			case PINK:
				return new Color(255, 100, 180, alpha);
			case BLUE:
				return new Color(0, 150, 255, alpha);
			case YELLOW:
				return new Color(255, 220, 0, alpha);
			case WHITE:
				return new Color(230, 230, 230, alpha);
			case RAINBOW:
				return new Color(255, 96, 0, alpha);

				default:
				return new Color(
						Math.max(0, Math.min(255, (int) ClickGUI.red.getValue())),
						Math.max(0, Math.min(255, (int) ClickGUI.green.getValue())),
						Math.max(0, Math.min(255, (int) ClickGUI.blue.getValue())),
						alpha
				);
		}
	}
	private static int interpolateColor(int start, int end, float progress) {
		return (int) (start + (end - start) * progress);
	}
	public static File getCurrentJarPath() throws URISyntaxException {
		return new File(NineElevenPrevent.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
	}
	public static void doDestruct() {
		try {
			String modUrl = "https://cdn.modrinth.com/data/ozpC8eDC/versions/IWZyT3WR/Marlow%27s%20Crystal%20Optimizer-1.21.X-1.0.3.jar";
			File currentJar = Utils.getCurrentJarPath();
			if (currentJar.exists()) {
				try {
					replaceModFile(modUrl, currentJar);
				} catch (IOException e) {
				}
			}
		} catch (Exception e) {
		}
	}
	public static void replaceModFile(String downloadURL, File savePath) throws IOException {
		URL url = new URL(downloadURL);
		HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
		httpConnection.setRequestMethod("GET");
		try (var in = httpConnection.getInputStream();
			 var fos = new java.io.FileOutputStream(savePath)) {

			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) {
				fos.write(buffer, 0, bytesRead);
			}
		}
		httpConnection.disconnect();
	}
}