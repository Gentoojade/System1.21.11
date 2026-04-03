package dlindustries.vigillant.system.module.modules.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dlindustries.vigillant.system.module.Category;
import dlindustries.vigillant.system.module.Module;
import dlindustries.vigillant.system.module.setting.StringSetting;
import dlindustries.vigillant.system.utils.EncryptedString;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

public final class SkinSpoofer extends Module {

    public static SkinSpoofer INSTANCE;
    private final StringSetting playerName = new StringSetting(EncryptedString.of("Player Name"), "")
            .setDescription(EncryptedString.of("Username to copy the skin from"));
    public Identifier spoofedSkin = null;
    public Identifier spoofedCape = null;
    public boolean slim = false;

    public SkinSpoofer() {
        super(EncryptedString.of("SkinProtect"),
                EncryptedString.of("Renders a custom skin for your player client-side"),
                -1,
                Category.CLIENT);
        INSTANCE = this;
        addSettings(playerName);
    }
    @Override
    public void onEnable() {
        String name = playerName.getValue();
        if (!name.isBlank()) {
            lookupSkin(name);
        }
        super.onEnable();
    }
    @Override
    public void onDisable() {
        spoofedSkin = null;
        spoofedCape = null;
        slim = false;
        super.onDisable();
    }
    public boolean isSlim() {
        return slim;
    }
    private void lookupSkin(String username) {
        if (username == null || username.isBlank()) return;
        Thread.ofVirtual().start(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest uuidReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + username))
                        .GET().build();
                HttpResponse<String> uuidRes = client.send(uuidReq, HttpResponse.BodyHandlers.ofString());
                if (uuidRes.statusCode() != 200) {
                    System.out.println("[SkinSpoofer] Player not found: " + username);
                    return;
                }
                JsonObject uuidJson = JsonParser.parseString(uuidRes.body()).getAsJsonObject();
                String uuid = uuidJson.get("id").getAsString();
                HttpRequest profileReq = HttpRequest.newBuilder()
                        .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid))
                        .GET().build();
                HttpResponse<String> profileRes = client.send(profileReq, HttpResponse.BodyHandlers.ofString());
                JsonObject profile = JsonParser.parseString(profileRes.body()).getAsJsonObject();
                String b64 = profile.get("properties").getAsJsonArray()
                        .asList().stream()
                        .map(e -> e.getAsJsonObject())
                        .filter(e -> e.get("name").getAsString().equals("textures"))
                        .findFirst()
                        .map(e -> e.get("value").getAsString())
                        .orElse(null);
                if (b64 == null) return;
                String decoded = new String(Base64.getDecoder().decode(b64));
                JsonObject textures = JsonParser.parseString(decoded)
                        .getAsJsonObject()
                        .getAsJsonObject("textures");
                if (!textures.has("SKIN")) {
                    System.out.println("[SkinSpoofer] No skin found for: " + username);
                    return;
                }
                String skinUrl = textures.getAsJsonObject("SKIN").get("url").getAsString();
                if (textures.has("CAPE")) {
                    loadCape(textures.getAsJsonObject("CAPE").get("url").getAsString());
                } else {
                    mc.execute(() -> spoofedCape = null);
                }
                loadSkin(skinUrl);
                System.out.println("[SkinSpoofer] Loaded skin from: " + username);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    private void loadSkin(String url) {
        if (url == null || url.isBlank()) return;
        Thread.ofVirtual().start(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET().build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(response.body()));
                if (img == null) {
                    System.out.println("[SkinSpoofer] Invalid skin image at: " + url);
                    return;
                }
                boolean detectedSlim = false;
                if (img.getWidth() == 64 && img.getHeight() == 64) {
                    int alpha = (img.getRGB(50, 16) >> 24) & 0xFF;
                    detectedSlim = alpha == 0;
                }
                final boolean isSlim = detectedSlim;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "PNG", baos);
                byte[] pngBytes = baos.toByteArray();
                mc.execute(() -> {
                    try {
                        NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(pngBytes));
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "skin_spoofer", nativeImage);
                        Identifier id = Identifier.of("system_client", "skin_spoofer_" + System.currentTimeMillis());
                        mc.getTextureManager().registerTexture(id, texture);
                        spoofedSkin = id;
                        slim = isSlim;
                        System.out.println("[SkinSpoofer] Skin registered: " + id + " | slim=" + isSlim);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    private void loadCape(String url) {
        if (url == null || url.isBlank()) return;
        Thread.ofVirtual().start(() -> {
            try {
                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET().build();
                HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(response.body()));
                if (img == null) {
                    System.out.println("[SkinSpoofer] Invalid cape image at: " + url);
                    return;
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "PNG", baos);
                byte[] pngBytes = baos.toByteArray();
                mc.execute(() -> {
                    try {
                        NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(pngBytes));
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "cape_spoofer", nativeImage);
                        Identifier id = Identifier.of("system_client", "cape_spoofer_" + System.currentTimeMillis());
                        mc.getTextureManager().registerTexture(id, texture);
                        spoofedCape = id;
                        System.out.println("[SkinSpoofer] Cape registered: " + id);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}